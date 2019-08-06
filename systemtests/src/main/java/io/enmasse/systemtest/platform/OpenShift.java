/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.platform;

import io.enmasse.address.model.TlsTermination;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import org.slf4j.Logger;

/**
 * Handles interaction with openshift cluster
 */
public class OpenShift extends Kubernetes {
    private static Logger log = CustomLogger.getLogger();

    public OpenShift(Environment environment, String globalNamespace) {
        super(globalNamespace, () -> {
            final Environment instance = Environment.getInstance();
            Config config = new ConfigBuilder().withMasterUrl(environment.getApiUrl())
                    .withOauthToken(environment.getApiToken())
                    .withHttp2Disable(true) //  // Workaround https://github.com/square/okhttp/issues/3146
                    .withConnectionTimeout((int)instance.getKubernetesApiConnectTimeout().toMillis())
                    .withRequestTimeout((int)instance.getKubernetesApiWriteTimeout().toMillis())
                    .build();

            return new DefaultOpenShiftClient(new OpenShiftConfig(config));
        });
    }

    @Override
    public Endpoint getMasterEndpoint() {
        return new Endpoint(client.getMasterUrl());
    }

    @Override
    public Endpoint getRestEndpoint() {
        Endpoint endpoint = null;

        endpoint = new Endpoint(client.getMasterUrl());

        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            log.info("Route endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint("api-server", infraNamespace, "https");
        }
    }

    @Override
    public Endpoint getKeycloakEndpoint() {
        OpenShiftClient openShift = client.adapt(OpenShiftClient.class);
        Route route = openShift.routes().inNamespace(infraNamespace).withName("keycloak").get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 443);
        log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            log.info("Endpoint didn't resolve, falling back to service endpoint");
            return getEndpoint("standard-authservice", infraNamespace, "https");
        }
    }

    @Override
    public Endpoint getExternalEndpoint(String endpointName) {
        return getExternalEndpoint(endpointName, infraNamespace);
    }

    @Override
    public Endpoint getExternalEndpoint(String endpointName, String namespace) {
        OpenShiftClient openShift = client.adapt(OpenShiftClient.class);
        Route route = openShift.routes().inNamespace(namespace).withName(endpointName).get();
        Endpoint endpoint = new Endpoint(route.getSpec().getHost(), 443);
        log.info("Testing endpoint : " + endpoint);
        if (TestUtils.resolvable(endpoint)) {
            return endpoint;
        } else {
            log.info("Endpoint didn't resolve, falling back to service endpoint");
            String port;
            switch (endpointName) {
                case "messaging":
                    port = "amqps";
                    break;
                case "console":
                    port = "https";
                    break;
                case "mqtt":
                    port = "secure-mqtt";
                    break;
                default:
                    throw new IllegalStateException(String.format("Endpoint '%s' doesn't exist.",
                            endpointName));
            }
            return getEndpoint(endpointName, namespace, port);
        }
    }

    @Override
    public void createExternalEndpoint(String name, String namespace, Service service, ServicePort targetPort) {
        Route route = new RouteBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .editOrNewSpec()
                .editOrNewPort()
                .withNewTargetPort(targetPort.getPort())
                .endPort()
                .withNewTls()
                .withTermination("passthrough")
                .endTls()
                .withNewTo()
                .withName(service.getMetadata().getName())
                .withKind("Service")
                .endTo()
                .endSpec()
                .build();

        OpenShiftClient openShift = client.adapt(OpenShiftClient.class);
        openShift.routes().inNamespace(namespace).withName(name).createOrReplace(route);
    }

    @Override
    public void deleteExternalEndpoint(String namespace, String name) {
        OpenShiftClient openShift = client.adapt(OpenShiftClient.class);
        openShift.routes().inNamespace(namespace).withName(name).delete();
    }

}
