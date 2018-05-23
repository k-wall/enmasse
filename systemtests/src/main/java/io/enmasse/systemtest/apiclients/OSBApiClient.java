/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import com.google.common.net.HttpHeaders;
import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.resources.ServiceInstance;
import io.enmasse.systemtest.selenium.resources.BindingSecretData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OSBApiClient extends ApiClient {
    protected static Logger log = CustomLogger.getLogger();
    private final String apiVersion = "2.13";
    private final String catalogPath = "/osbapi/v2/catalog";                                         //GET
    private final String serviceInstancesPath = "/osbapi/v2/service_instances/%s";                   //PUT, PATCH, DELETE
    private final String lastOperation = "/osbapi/v2/service_instances/%s/last_operation";           //GET
    private final String serviceBindingsPath = "/osbapi/v2/service_instances/%s/service_bindings/%s";//PUT, DELETE
    private final int retry = 10;

    public OSBApiClient(Kubernetes kubernetes) {
        super(kubernetes, kubernetes.getOSBEndpoint());
    }

    public void close() {
        client.close();
        vertx.close();
    }

    @Override
    protected String apiClientName() {
        return "Open Service Broker";
    }


    /**
     * Provision of new instance of enmasse
     *
     * @param addressSpace Address space that will be created (plan and type have to be set)
     * @return autogenerated instanceId of provided instance
     * @throws Exception
     */
    public ServiceInstance provisionInstance(AddressSpace addressSpace, String username, Optional<String> optInstanceId) throws Exception {
        JsonObject serviceCatalog = getCatalog(username);
        String serviceId = getAddressSpaceID(serviceCatalog.getJsonArray("services"), serviceName(addressSpace));
        String planId = getAddressSpacePlanID(serviceCatalog.getJsonArray("services"), addressSpace);

        //prepare payload
        JsonObject payload = new JsonObject();
        payload.put("service_id", serviceId);
        payload.put("plan_id", planId);
        payload.put("organization_guid", addressSpace.getName());
        payload.put("space_guid", addressSpace.getName());
        if (addressSpace.getName() != null) {
            JsonObject parameters = new JsonObject();
            parameters.put("name", addressSpace.getName());
            payload.put("parameters", parameters);
        }

        //prepare and send request
        String instanceId = optInstanceId.isPresent() ? optInstanceId.get() : UUID.randomUUID().toString();
        String putPath = String.format(serviceInstancesPath, instanceId);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        log.info("PUT-ServiceInstance: path {}; ", putPath);
        JsonObject response = doRequestNTimes(retry, () -> {
            client.put(endpoint.getPort(), endpoint.getHost(), putPath)
                    .putHeader("X-Broker-API-Version", apiVersion)
                    .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                    .putHeader("X-Broker-API-Originating-Identity", encodeOriginatingIndentValue(username))
                    .addQueryParam("accepts_incomplete", "true")
                    .as(BodyCodec.jsonObject())
                    .timeout(10_000)
                    .sendJsonObject(payload,
                            ar -> responseHandler(ar, responsePromise, "Error: put service instance"));
            return responsePromise.get(20, TimeUnit.SECONDS);
        }, Optional.empty());
        log.info("PUT-ServiceInstance: Async operation done with result: {}", response.toString());
        String dashboardUrl = response.getString("dashboard_url");
        ServiceInstance provInstance = new ServiceInstance(instanceId, dashboardUrl);
        return provInstance;
    }

    public ServiceInstance provisionInstance(AddressSpace addressSpace, String username) throws Exception {
        return provisionInstance(addressSpace, username, Optional.empty());
    }


    /**
     * Deprovision of already existing instance
     *
     * @param addressSpace address space that will be removed
     * @param instanceID   id of instance
     * @throws Exception
     */
    public void deprovisionInstance(AddressSpace addressSpace, String username, String instanceID) throws Exception {
        JsonObject serviceCatalog = getCatalog(username);
        String serviceId = getAddressSpaceID(serviceCatalog.getJsonArray("services"), serviceName(addressSpace));
        String planId = getAddressSpacePlanID(serviceCatalog.getJsonArray("services"), addressSpace);

        //prepare and send request
        String deletePath = String.format(serviceInstancesPath, instanceID);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        log.info("DELETE-ServiceInstance: path {}; ", deletePath);
        JsonObject response = doRequestNTimes(retry, () -> {
            client.delete(endpoint.getPort(), endpoint.getHost(), deletePath)
                    .putHeader("X-Broker-API-Version", apiVersion)
                    .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                    .putHeader("X-Broker-API-Originating-Identity", encodeOriginatingIndentValue(username))
                    .addQueryParam("accepts_incomplete", "true")
                    .addQueryParam("service_id", serviceId)
                    .addQueryParam("plan_id", planId)
                    .as(BodyCodec.jsonObject())
                    .timeout(10_000)
                    .send(ar -> responseHandler(ar, responsePromise, "Error: delete service instance"));
            return responsePromise.get(20, TimeUnit.SECONDS);
        }, Optional.empty());
        log.info("DELETE-ServiceInstance: Async operation done with result: {}", response.toString());
    }


    /**
     * available binding keys: sendAddresses(string), receiveAddresses(string), consoleAccess(boolean), consoleAdmin(boolean), externalAccess(boolean)
     *
     * @param addressSpace
     * @param instanceId
     * @throws Exception
     */
    public BindingSecretData generateBinding(AddressSpace addressSpace, String username, String instanceId, HashMap<String, String> bindings) throws Exception {
        JsonObject serviceCatalog = getCatalog(username);
        String serviceId = getAddressSpaceID(serviceCatalog.getJsonArray("services"), serviceName(addressSpace));
        String planId = getAddressSpacePlanID(serviceCatalog.getJsonArray("services"), addressSpace);

        //prepare payload
        JsonObject payload = new JsonObject();
        payload.put("service_id", serviceId);
        payload.put("plan_id", planId);

        if (bindings.size() > 0) {
            JsonObject bindResource = new JsonObject();
            for (Map.Entry<String, String> entry : bindings.entrySet()) {
                bindResource.put(entry.getKey(), entry.getValue());
            }
            payload.put("parameters", bindResource);
        }

        //prepare and send request
        String bindingId = UUID.randomUUID().toString();
        String putPath = String.format(serviceBindingsPath, instanceId, bindingId);
        log.info("PUT-ServiceBindings: path {}; ", putPath);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        JsonObject response = doRequestNTimes(retry, () -> {
            client.put(endpoint.getPort(), endpoint.getHost(), putPath)
                    .putHeader("X-Broker-API-Version", apiVersion)
                    .putHeader("X-Broker-API-Originating-Identity", encodeOriginatingIndentValue(username))
                    .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(10_000)
                    .sendJsonObject(payload,
                            ar -> responseHandler(ar, responsePromise, "Error: put service bindings"));
            return responsePromise.get(20, TimeUnit.SECONDS);
        }, Optional.empty());
        log.info("PUT-ServiceBindings: finished successfully with response {}; ", response.toString());
        return new BindingSecretData(response, bindingId);
    }

    //!TODO this functionality is not verified yet
    public void deprovisionBinding(AddressSpace addressSpace, String username, String instanceId, String bindingId) throws Exception {
        JsonObject serviceCatalog = getCatalog(username);
        String serviceId = getAddressSpaceID(serviceCatalog.getJsonArray("services"), serviceName(addressSpace));
        String planId = getAddressSpacePlanID(serviceCatalog.getJsonArray("services"), addressSpace);

        //prepare and send request
        String deletePath = String.format(serviceBindingsPath, instanceId, bindingId);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        log.info("DELETE-Binding: path {}; ", deletePath);
        JsonObject response = doRequestNTimes(retry, () -> {
            client.delete(endpoint.getPort(), endpoint.getHost(), deletePath)
                    .putHeader("X-Broker-API-Version", apiVersion)
                    .putHeader("X-Broker-API-Originating-Identity", encodeOriginatingIndentValue(username))
                    .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                    .addQueryParam("service_id", serviceId)
                    .addQueryParam("plan_id", planId)
                    .as(BodyCodec.jsonObject())
                    .timeout(10_000)
                    .send(ar -> responseHandler(ar, responsePromise, "Error: delete binding instance"));
            return responsePromise.get(20, TimeUnit.SECONDS);
        }, Optional.empty());
        log.info("DELETE-Binding: operation done with result: {}", response.toString());
    }


    /**
     * Get last operation on instance
     *
     * @param instanceId id of instance
     * @return answer from broker service
     * @throws Exception
     */
    public JsonObject getLastOperation(String username, String instanceId) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        String getPath = String.format(lastOperation, instanceId);
        log.info("GET-lastOperation: path {}", getPath);
        return doRequestNTimes(retry, () -> {
            client.get(endpoint.getPort(), endpoint.getHost(), getPath)
                    .putHeader("X-Broker-API-Version", apiVersion)
                    .putHeader("X-Broker-API-Originating-Identity", encodeOriginatingIndentValue(username))
                    .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(10_000)
                    .send(ar -> responseHandler(ar, responsePromise, "Error: get last operation on service instance"));
            return responsePromise.get(20, TimeUnit.SECONDS);
        }, Optional.empty());
    }


    /**
     * Get catalog of services
     *
     * @return JsonObject with "services"
     * @throws Exception
     */
    public JsonObject getCatalog(String username) throws Exception {
        log.info("GET-ServiceCatalog: path {}; ", catalogPath);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(retry, () -> {
            client.get(endpoint.getPort(), endpoint.getHost(), catalogPath)
                    .putHeader("X-Broker-API-Version", apiVersion)
                    .putHeader("X-Broker-API-Originating-Identity", encodeOriginatingIndentValue(username))
                    .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(10_000)
                    .send((ar) -> responseHandler(ar, responsePromise, "Error: get service catalog"));
            return responsePromise.get(20, TimeUnit.SECONDS);
        }, Optional.empty());
    }

    /**
     * Get address space ID by name of service
     *
     * @param services    JsonArray with services
     * @param serviceName name of service
     * @return
     */
    private String getAddressSpaceID(JsonArray services, String serviceName) {
        JsonObject service = services.stream().map(JsonObject.class::cast)
                .filter(serviceI -> serviceI.getString("name").equals(serviceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Service '{}' doesn't exist", serviceName)));
        return service.getString("id");
    }

    /**
     * Get id of plan for specific address space
     *
     * @param services     JsonArray with services
     * @param addressSpace AddressSpace with name of address space and name of address space plan
     * @return id of address space plan
     */
    private String getAddressSpacePlanID(JsonArray services, AddressSpace addressSpace) {
        String serviceName = serviceName(addressSpace);
        JsonObject service = services.stream().map(JsonObject.class::cast)
                .filter(serviceI -> serviceI.getString("name").equals(serviceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Plan '{}' doesn't exist within service '{}'",
                        addressSpace.getPlan(), serviceName)));
        return getPlanID(service.getJsonArray("plans"), addressSpace.getPlan());
    }

    /**
     * Get plan id from list of plans by name of the plan
     *
     * @param plans    JsonArray with available plans
     * @param planName name of address space plan
     * @return id of address space plan
     */
    private String getPlanID(JsonArray plans, String planName) {
        JsonObject plan = plans.stream().map(JsonObject.class::cast).filter(planI -> planI.getString("name").equals(planName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Plan '{}' doesn't exist", planName)));
        return plan.getString("id");
    }

    /**
     * Add 'enmasse' prefix before type of address space and return as a String
     *
     * @param addressSpace
     * @return
     */
    private String serviceName(AddressSpace addressSpace) {
        return String.format("enmasse-%s", addressSpace.getType());
    }

    /**
     * Return value for X-Broker-API-Originating-Identity header
     * syntax is: X-Broker-API-Originating-Identity: Platform value
     *
     * @param username name of the user
     * @return
     */
    private String encodeOriginatingIndentValue(String username) {
        JsonObject payload = new JsonObject();
        payload.put("username", username);
        return String.format("kubernetes %s", Base64.getEncoder().encodeToString(payload.toString().getBytes()));
    }

}
