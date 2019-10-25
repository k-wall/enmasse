/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.executor.Exec;
import io.enmasse.systemtest.logs.CustomLogger;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.slf4j.Logger;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;

public class Minikube extends Kubernetes {
    private static Logger log = CustomLogger.getLogger();

    protected Minikube(String globalNamespace) {
        super(globalNamespace, () -> {
            final Environment instance = Environment.getInstance();
            Config config = new ConfigBuilder()
                    .withHttp2Disable(true) // Workaround https://github.com/square/okhttp/issues/3146
                    .withConnectionTimeout((int)instance.getKubernetesApiConnectTimeout().toMillis())
                    .withRequestTimeout((int)instance.getKubernetesApiWriteTimeout().toMillis())
                    .build();

            return new DefaultKubernetesClient(config);
        });
    }

    private static String runCommand(String... cmd) {
        try {
            Exec executor = new Exec(false);
            int returnCode = executor.exec(Arrays.asList(cmd), 10000);
            if (returnCode == 0) {
                return executor.getStdOut();
            } else {
                throw new RuntimeException(executor.getStdErr());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getIp(String namespace, String serviceName) {
        String [] output = runCommand("minikube", "service", "-n", namespace, "--format", "{{.IP}}", serviceName).split(" ");
        return output[output.length - 1];
    }

    private String getPort(String namespace, String serviceName) {
        String [] output = runCommand("minikube", "service", "-n", namespace, "--format", "{{.Port}}", serviceName).split(" ");
        return output[output.length - 1];
    }

    @Override
    public Endpoint getMasterEndpoint() {
        return new Endpoint(client.getMasterUrl());
    }

    @Override
    public Endpoint getRestEndpoint() {
        return new Endpoint(client.getMasterUrl());
    }

    @Override
    public Endpoint getKeycloakEndpoint() {
        return getExternalEndpoint("standard-authservice");
    }

    @Override
    public Endpoint getExternalEndpoint(String name) {
        return getExternalEndpoint(name, infraNamespace);
    }

    @Override
    public Endpoint getExternalEndpoint(String name, String namespace) {
        String externalName = name;
        if (!name.endsWith("-external")) {
            externalName += "-external";
        }
        Endpoint endpoint = new Endpoint(getIp(namespace, externalName), Integer.parseInt(getPort(namespace, externalName)));
        log.info("Minikube external endpoint - " + endpoint.toString());
        return endpoint;
    }

    @Override
    public void createExternalEndpoint(String name, String namespace, Service service, ServicePort targetPort) {
        if (!name.endsWith("-external")) {
            name += "-external";
        }
        ServiceBuilder builder = new ServiceBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .editOrNewSpec()
                .withType("LoadBalancer")
                .addToPorts(targetPort)
                .withSelector(service.getSpec().getSelector())
                .endSpec();

        client.services().inNamespace(namespace).withName(name).createOrReplace(builder.build());
    }

    @Override
    public void deleteExternalEndpoint(String namespace, String name) {
        if (!name.endsWith("-external")) {
            name += "-external";
        }
        client.services().inNamespace(name).withName(name).delete();
    }
}
