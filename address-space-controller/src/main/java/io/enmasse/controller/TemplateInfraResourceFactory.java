/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.TemplateParameter;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.SchemaProvider;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static io.enmasse.address.model.KubeUtil.applyPodTemplate;
import static io.enmasse.address.model.KubeUtil.lookupResource;

public class TemplateInfraResourceFactory implements InfraResourceFactory {
    private static final Logger log = LoggerFactory.getLogger(TemplateInfraResourceFactory.class.getName());
    private final String WELL_KNOWN_CONSOLE_SERVICE_NAME = "console";

    private final Kubernetes kubernetes;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final boolean openShift;
    private final SchemaProvider schemaProvider;

    public TemplateInfraResourceFactory(Kubernetes kubernetes, AuthenticationServiceRegistry authenticationServiceRegistry, boolean openShift, SchemaProvider schemaProvider) {
        this.kubernetes = kubernetes;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
        this.openShift = openShift;
        this.schemaProvider = schemaProvider;
    }

    private void prepareParameters(InfraConfig infraConfig,
                                   AddressSpace addressSpace,
                                   Map<String, String> parameters) {

        AuthenticationService authService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService())
                .orElseThrow(() -> new IllegalArgumentException("Unable to find authentication service " + addressSpace.getSpec().getAuthenticationService()));

        if (authService.getStatus() == null) {
            throw new IllegalArgumentException("Authentication service '" + authService.getMetadata().getName() + "' is not yet deployed");
        }

        Optional<ConsoleService> console = schemaProvider.getSchema().findConsoleService(WELL_KNOWN_CONSOLE_SERVICE_NAME);
        if (console.isEmpty()) {
            log.warn("No ConsoleService found named '{}', address space console service will be unavailable", WELL_KNOWN_CONSOLE_SERVICE_NAME);
        }

        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        parameters.put(TemplateParameter.INFRA_NAMESPACE, kubernetes.getNamespace());
        parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getMetadata().getName());
        parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
        parameters.put(TemplateParameter.ADDRESS_SPACE_NAMESPACE, addressSpace.getMetadata().getNamespace());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authService.getStatus().getHost());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authService.getStatus().getPort()));
        parameters.put(TemplateParameter.ADDRESS_SPACE_PLAN, addressSpace.getSpec().getPlan());

        String encodedCaCert = Optional.ofNullable(authService.getStatus().getCaCertSecret())
                .map(secretName ->
                    kubernetes.getSecret(secretName.getName()).map(secret ->
                            secret.getData().get("tls.crt"))
                            .orElseThrow(() -> new IllegalArgumentException("Unable to decode secret " + secretName)))
                .orElseGet(() -> {
                    try {
                        return Base64.getEncoder().encodeToString(Files.readAllBytes(new File("/etc/ssl/certs/ca-bundle.crt").toPath()));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_CERT, encodedCaCert);
        if (authService.getStatus().getClientCertSecret()  != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, authService.getStatus().getClientCertSecret().getName());
        }

        if (authService.getSpec().getRealm() != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, authService.getSpec().getRealm());
        } else {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, addressSpace.getAnnotation(AnnotationKeys.REALM_NAME));
        }

        if (authService.getMetadata().getAnnotations() != null &&
                authService.getMetadata().getAnnotations().get(AnnotationKeys.OAUTH_URL) != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_OAUTH_URL, authService.getMetadata().getAnnotations().get(AnnotationKeys.OAUTH_URL));
        }


        Map<String, CertSpec> serviceCertMapping = new HashMap<>();
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getCert() != null) {
                serviceCertMapping.put(endpoint.getService(), endpoint.getCert());
            }
        }
        parameters.put(TemplateParameter.MESSAGING_SECRET, serviceCertMapping.get("messaging").getSecretName());
        parameters.put(TemplateParameter.CONSOLE_SECRET, serviceCertMapping.get("console").getSecretName());

        if (console.isPresent()) {
            ConsoleService consoleService = console.get();
            ConsoleServiceSpec service = consoleService.getSpec();

           kubernetes.getSecret(service.getOauthClientSecret().getName()).ifPresentOrElse(secret -> {
               parameters.put(TemplateParameter.CONSOLE_OAUTH_DISCOVERY_URL, service.getDiscoveryMetadataURL());
               parameters.put(TemplateParameter.CONSOLE_OAUTH_SCOPE, service.getScope());
               Base64.Decoder decoder = Base64.getDecoder();
               parameters.put(TemplateParameter.CONSOLE_OAUTH_CLIENT_ID, new String(decoder.decode(secret.getData().get("client-id")), StandardCharsets.UTF_8));
               parameters.put(TemplateParameter.CONSOLE_OAUTH_CLIENT_SECRET, new String(decoder.decode(secret.getData().get("client-secret")), StandardCharsets.UTF_8));
           }, () -> log.warn("No console OAuth parameters available from ConsoleService {}, " +
                   "address space console will be unavailable.", consoleService.getMetadata().getName()));
        }
    }

    private void prepareMqttParameters(AddressSpace addressSpace, Map<String, String> parameters) {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getMetadata().getName());
        parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
        Map<String, CertSpec> serviceCertMapping = new HashMap<>();
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getCert() != null) {
                serviceCertMapping.put(endpoint.getService(), endpoint.getCert());
            }
        }
        parameters.put(TemplateParameter.MQTT_SECRET, serviceCertMapping.get("mqtt").getSecretName());
    }

    private List<HasMetadata> createStandardInfraMqtt(AddressSpace addressSpace, String templateName) {
        Map<String, String> parameters = new HashMap<>();
        prepareMqttParameters(addressSpace, parameters);
        return new ArrayList<>(kubernetes.processTemplate(templateName, parameters).getItems());
    }


    private List<HasMetadata> createStandardInfra(AddressSpace addressSpace, StandardInfraConfig standardInfraConfig) {

        Map<String, String> parameters = new HashMap<>();

        prepareParameters(standardInfraConfig, addressSpace, parameters);

        if (standardInfraConfig.getSpec().getBroker() != null) {
            if (standardInfraConfig.getSpec().getBroker().getResources() != null) {
                if (standardInfraConfig.getSpec().getBroker().getResources().getMemory() != null) {
                    parameters.put(TemplateParameter.BROKER_MEMORY_LIMIT, standardInfraConfig.getSpec().getBroker().getResources().getMemory());
                }
                if (standardInfraConfig.getSpec().getBroker().getResources().getStorage() != null) {
                    parameters.put(TemplateParameter.BROKER_STORAGE_CAPACITY, standardInfraConfig.getSpec().getBroker().getResources().getStorage());
                }
            }

            if (standardInfraConfig.getSpec().getBroker().getAddressFullPolicy() != null) {
                parameters.put(TemplateParameter.BROKER_ADDRESS_FULL_POLICY, standardInfraConfig.getSpec().getBroker().getAddressFullPolicy());
            }

            if (standardInfraConfig.getSpec().getBroker().getGlobalMaxSize() != null) {
                parameters.put(TemplateParameter.BROKER_GLOBAL_MAX_SIZE, standardInfraConfig.getSpec().getBroker().getGlobalMaxSize());
            }
        }

        if (standardInfraConfig.getSpec().getRouter() != null) {
            if (standardInfraConfig.getSpec().getRouter().getResources() != null && standardInfraConfig.getSpec().getRouter().getResources().getMemory() != null) {
                parameters.put(TemplateParameter.ROUTER_MEMORY_LIMIT, standardInfraConfig.getSpec().getRouter().getResources().getMemory());
            }

            if (standardInfraConfig.getSpec().getRouter().getLinkCapacity() != null) {
                parameters.put(TemplateParameter.ROUTER_LINK_CAPACITY, String.valueOf(standardInfraConfig.getSpec().getRouter().getLinkCapacity()));
            }

            if (standardInfraConfig.getSpec().getRouter().getHandshakeTimeout() != null) {
                parameters.put(TemplateParameter.ROUTER_HANDSHAKE_TIMEOUT, String.valueOf(standardInfraConfig.getSpec().getRouter().getHandshakeTimeout()));
            }

            if (standardInfraConfig.getSpec().getRouter().getIdleTimeout() != null) {
                parameters.put(TemplateParameter.ROUTER_IDLE_TIMEOUT, String.valueOf(standardInfraConfig.getSpec().getRouter().getIdleTimeout()));
            }

            if (standardInfraConfig.getSpec().getRouter().getWorkerThreads() != null) {
                parameters.put(TemplateParameter.ROUTER_WORKER_THREADS, String.valueOf(standardInfraConfig.getSpec().getRouter().getWorkerThreads()));
            }

        }

        if (standardInfraConfig.getSpec().getAdmin() != null && standardInfraConfig.getSpec().getAdmin().getResources() != null && standardInfraConfig.getSpec().getAdmin().getResources().getMemory() != null) {
            parameters.put(TemplateParameter.ADMIN_MEMORY_LIMIT, standardInfraConfig.getSpec().getAdmin().getResources().getMemory());
        }

        parameters.put(TemplateParameter.STANDARD_INFRA_CONFIG_NAME, standardInfraConfig.getMetadata().getName());

        Map<String, String> infraAnnotations = standardInfraConfig.getMetadata().getAnnotations();
        String templateName = getAnnotation(infraAnnotations, AnnotationKeys.TEMPLATE_NAME, "standard-space-infra");
        List<HasMetadata> items = new ArrayList<>(kubernetes.processTemplate(templateName, parameters).getItems());

        if (standardInfraConfig.getSpec().getRouter() != null && standardInfraConfig.getSpec().getRouter().getMinReplicas() != null) {
            // Workaround since parameterized integer fields cannot be loaded locally by fabric8 kubernetes-client
            for (HasMetadata item : items) {
                if (item instanceof StatefulSet && "qdrouterd".equals(item.getMetadata().getLabels().get(LabelKeys.NAME))) {
                    StatefulSet router = (StatefulSet) item;
                    router.getSpec().setReplicas(standardInfraConfig.getSpec().getRouter().getMinReplicas());
                }
            }
        }

        if (standardInfraConfig.getSpec().getAdmin() != null && standardInfraConfig.getSpec().getAdmin().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = standardInfraConfig.getSpec().getAdmin().getPodTemplate();
            Deployment adminDeployment = lookupResource("Deployment", KubeUtil.getAdminDeploymentName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (standardInfraConfig.getSpec().getRouter() != null && standardInfraConfig.getSpec().getRouter().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = standardInfraConfig.getSpec().getRouter().getPodTemplate();
            StatefulSet routerSet = lookupResource("StatefulSet", KubeUtil.getRouterSetName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = routerSet.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (Boolean.parseBoolean(getAnnotation(infraAnnotations, AnnotationKeys.WITH_MQTT, "false"))) {
            String mqttTemplateName = getAnnotation(infraAnnotations, AnnotationKeys.MQTT_TEMPLATE_NAME, "standard-space-infra-mqtt");
            items.addAll(createStandardInfraMqtt(addressSpace, mqttTemplateName));
        }

        if (standardInfraConfig.getSpec().getBroker() != null) {
            return applyStorageClassName(standardInfraConfig.getSpec().getBroker().getStorageClassName(), items);
        } else {
            return items;
        }
    }

    private String getAnnotation(Map<String, String> annotations, String key, String defaultValue) {
        return Optional.ofNullable(annotations)
                .flatMap(m -> Optional.ofNullable(m.get(key)))
                .orElse(defaultValue);
    }

    private List<HasMetadata> createBrokeredInfra(AddressSpace addressSpace, BrokeredInfraConfig brokeredInfraConfig) {
        Map<String, String> parameters = new HashMap<>();

        prepareParameters(brokeredInfraConfig, addressSpace, parameters);

        if (brokeredInfraConfig.getSpec().getBroker() != null) {
            if (brokeredInfraConfig.getSpec().getBroker().getResources() != null) {
                if (brokeredInfraConfig.getSpec().getBroker().getResources().getMemory() != null) {
                    parameters.put(TemplateParameter.BROKER_MEMORY_LIMIT, brokeredInfraConfig.getSpec().getBroker().getResources().getMemory());
                }
                if (brokeredInfraConfig.getSpec().getBroker().getResources().getStorage() != null) {
                    parameters.put(TemplateParameter.BROKER_STORAGE_CAPACITY, brokeredInfraConfig.getSpec().getBroker().getResources().getStorage());
                }
            }

            if (brokeredInfraConfig.getSpec().getBroker().getAddressFullPolicy() != null) {
                parameters.put(TemplateParameter.BROKER_ADDRESS_FULL_POLICY, brokeredInfraConfig.getSpec().getBroker().getAddressFullPolicy());
            }

            if (brokeredInfraConfig.getSpec().getBroker().getGlobalMaxSize() != null) {
                parameters.put(TemplateParameter.BROKER_GLOBAL_MAX_SIZE, brokeredInfraConfig.getSpec().getBroker().getGlobalMaxSize());
            }
        }

        if (brokeredInfraConfig.getSpec().getAdmin() != null && brokeredInfraConfig.getSpec().getAdmin().getResources() != null && brokeredInfraConfig.getSpec().getAdmin().getResources().getMemory() != null) {
            parameters.put(TemplateParameter.ADMIN_MEMORY_LIMIT, brokeredInfraConfig.getSpec().getAdmin().getResources().getMemory());
        }

        List<HasMetadata> items;
        String templateName = getAnnotation(brokeredInfraConfig.getMetadata().getAnnotations(), AnnotationKeys.TEMPLATE_NAME, "brokered-space-infra");
        if (brokeredInfraConfig.getSpec().getBroker() != null) {
            items = applyStorageClassName(brokeredInfraConfig.getSpec().getBroker().getStorageClassName(), kubernetes.processTemplate(templateName, parameters).getItems());
        } else {
            items = kubernetes.processTemplate(templateName, parameters).getItems();
        }

        if (brokeredInfraConfig.getSpec().getAdmin() != null && brokeredInfraConfig.getSpec().getAdmin().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = brokeredInfraConfig.getSpec().getAdmin().getPodTemplate();
            Deployment adminDeployment = lookupResource("Deployment", KubeUtil.getAgentDeploymentName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (brokeredInfraConfig.getSpec().getBroker() != null && brokeredInfraConfig.getSpec().getBroker().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = brokeredInfraConfig.getSpec().getBroker().getPodTemplate();
            Deployment brokerDeployment = lookupResource("Deployment", KubeUtil.getBrokeredBrokerSetName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = brokerDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        return items;
    }

    private List<HasMetadata> applyStorageClassName(String storageClassName, List<HasMetadata> items) {
        if (storageClassName != null) {
            for (HasMetadata item : items) {
                if (item instanceof PersistentVolumeClaim) {
                    ((PersistentVolumeClaim) item).getSpec().setStorageClassName(storageClassName);
                }
            }
        }
        return items;
    }

    @Override
    public List<HasMetadata> createInfraResources(AddressSpace addressSpace, InfraConfig infraConfig) {


        if ("standard".equals(addressSpace.getSpec().getType())) {
            return createStandardInfra(addressSpace, (StandardInfraConfig) infraConfig);
        } else if ("brokered".equals(addressSpace.getSpec().getType())) {
            return createBrokeredInfra(addressSpace, (BrokeredInfraConfig) infraConfig);
        } else {
            throw new IllegalArgumentException("Unknown address space type " + addressSpace.getSpec().getType());
        }
    }
}
