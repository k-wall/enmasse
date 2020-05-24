/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.plans.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanBuilder;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.BrokeredInfraConfigBuilder;
import io.enmasse.admin.model.v1.BrokeredInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.bases.plans.PlansTestBase;
import io.enmasse.systemtest.broker.ArtemisUtils;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlansTestBrokered extends PlansTestBase implements ITestIsolatedBrokered {
    private static Logger log = CustomLogger.getLogger();

    @Test
    void testReplaceAddressSpacePlanBrokered() throws Exception {
        //define and create address plans
        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("small-queue", AddressType.QUEUE,
                Collections.singletonList(new ResourceRequest("broker", 0.4)));

        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("bigger-queue", AddressType.QUEUE,
                Collections.singletonList(new ResourceRequest("broker", 0.7)));

        isolatedResourcesManager.createAddressPlan(beforeQueuePlan);
        isolatedResourcesManager.createAddressPlan(afterQueuePlan);

        //define and create address space plans

        AddressSpacePlan beforeAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("before-update-brokered-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 5.0)),
                Collections.singletonList(beforeQueuePlan));

        AddressSpacePlan afterAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("after-update-brokered-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 5.0)),
                Collections.singletonList(afterQueuePlan));

        isolatedResourcesManager.createAddressSpacePlan(beforeAddressSpacePlan);
        isolatedResourcesManager.createAddressSpacePlan(afterAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-brokered-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(beforeAddressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-1")
                .withPlan(beforeQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.setAddresses(queue);

        clientUtils.sendDurableMessages(resourcesManager, addressSpace, queue, user, 16);

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(afterAddressSpacePlan.getMetadata().getName()).endSpec().done();
        isolatedResourcesManager.replaceAddressSpace(addressSpace);

        clientUtils.receiveDurableMessages(resourcesManager, addressSpace, queue, user, 16);

        Address afterQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-2")
                .withPlan(afterQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.appendAddresses(afterQueue);

        getClientUtils().assertCanConnect(addressSpace, user, Arrays.asList(afterQueue, queue), resourcesManager);
    }

    @Test
    void testUnknownAddressPlan() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-brokered-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        String unknownPlan = "unknown-plan";
        String unknownPlanMessages = String.format("Unknown address plan '%s'", unknownPlan);
        List<StageHolder> stageHolders = new ArrayList<>();
        stageHolders.add(new StageHolder(addressSpace, "initial-unknown-plan")
                .addStage(unknownPlan, assertAddressStatusNotReady(unknownPlanMessages)));
        stageHolders.add(new StageHolder(addressSpace, "becomes-unknown-plan")
                .addStage(DestinationPlan.BROKERED_QUEUE, assertAddressStatusReady(DestinationPlan.BROKERED_QUEUE))
                .addStage(unknownPlan, assertAddressStatusNotReady(unknownPlanMessages)));
        stageHolders.add(new StageHolder(addressSpace, "initial-unknown-plan-for-type")
                .addStage(DestinationPlan.BROKERED_TOPIC, assertAddressStatusNotReady(String.format("Unknown address plan '%s'", DestinationPlan.BROKERED_TOPIC)))
                .addStage(DestinationPlan.BROKERED_QUEUE, assertAddressStatusReady(DestinationPlan.BROKERED_QUEUE)));
        stageHolders.add(new StageHolder(addressSpace,"good").addStage(DestinationPlan.BROKERED_QUEUE,
                assertAddressStatusReady(DestinationPlan.BROKERED_QUEUE)));

        doTestUnknownAddressPlan(addressSpace, stageHolders);
    }

    @Test
    void testUpdatePlanBrokerCredit_changesPerAddressMaxSize() throws Exception {
        BrokeredInfraConfig infra = new BrokeredInfraConfigBuilder()
                .withNewMetadata()
                .withName("vbusch")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new BrokeredInfraConfigSpecBrokerBuilder()
                        .withGlobalMaxSize("1mb")
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("512Mi")
                        .withStorage("512Mi")

                        .endResources()
                        .build())
                .endSpec()
                .build();

        resourcesManager.createInfraConfig(infra);

        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.5));
        AddressPlan queuePlan = PlanUtils.createAddressPlanObject("brokered-queue-plan", AddressType.QUEUE, addressResourcesQueue);
        List<ResourceRequest> afterAddressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.7));
        List<ResourceRequest> afterAddressLargeResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.9));
        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("brokered-large-queue-plan", AddressType.QUEUE, afterAddressResourcesQueue);

        isolatedResourcesManager.createAddressPlan(queuePlan);
        isolatedResourcesManager.createAddressPlan(afterQueuePlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 9),
                new ResourceAllowance("aggregate", 10.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, afterQueuePlan);

        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject("queue-plan", infra.getMetadata().getName(), AddressSpaceType.BROKERED, resources, addressPlans);
        resourcesManager.createAddressSpacePlan(addressSpacePlan);

        //create address space plan with new plan
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("planspace")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(addressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);

        //deploy destinations
        Address queueDest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "myqueue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("myqueue")
                .withPlan(queuePlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.setAddresses(queueDest);

        //get destinations
        Address queue = kubernetes.getAddressClient(addressSpace.getMetadata().getNamespace()).withName(queueDest.getMetadata().getName()).get();

        String assertMessage = "Queue plan wasn't set properly";
        assertEquals(queue.getSpec().getPlan(),
                queuePlan.getMetadata().getName(), assertMessage);

        //Send messages to ensure queue fills up
        UserCredentials user = new UserCredentials("test-newplan-name", "test_newplan_password");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        AmqpClient client = getAmqpClientFactory().createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(user);
        byte[] bytes = new byte[1024 * 100];
        Random random = new Random();
        Message message = Message.Factory.create();
        random.nextBytes(bytes);
        message.setBody(new AmqpValue(new Data(new Binary(bytes))));
        message.setAddress(queue.getSpec().getAddress());
        message.setDurable(true);

//        client.sendMessages(queue.getSpec().getAddress(), (List<String>) Arrays.asList(message.getBody().toString()));
        Stream<Message> messageStream = Stream.generate(() -> message);
        int messagesSent = client.sendMessagesCheckDelivery(queue.getSpec().getAddress(), messageStream::iterator,
                protonDelivery -> protonDelivery.remotelySettled() && protonDelivery.getRemoteState().getType().equals(DeliveryState.DeliveryStateType.Rejected))
                .get(5, TimeUnit.MINUTES);

        assertTrue(messagesSent > 0, "Verify a few messages were sent before queue fills up");

        //Verify maxSizeBytes are set
        Map<String, Object> addressSettings = ArtemisUtils.getAddressSettings(kubernetes, addressSpace);
        assertEquals(Integer.valueOf("524288"), (Integer) addressSettings.get("maxSizeBytes"), "maxSizeBytes should be set");

        //Switch to a large plan
        Address largeQueue = new AddressBuilder(queue).editSpec().withPlan(afterQueuePlan.getMetadata().getName()).endSpec().build();
        isolatedResourcesManager.replaceAddress(largeQueue);
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(5, TimeUnit.MINUTES), largeQueue);
        addressSettings = ArtemisUtils.getAddressSettings(kubernetes, addressSpace);
        assertEquals(Integer.valueOf("734003"), (Integer) addressSettings.get("maxSizeBytes"), "maxSizeBytes should be set");

        //Update plan to have more credit
        AddressPlan updatedAfterQueuePlan = new AddressPlanBuilder(afterQueuePlan).editSpec().withResources(afterAddressLargeResourcesQueue.stream().collect(Collectors.toMap(ResourceRequest::getName, ResourceRequest::getCredit))).endSpec().build();
        isolatedResourcesManager.replaceAddressPlan(updatedAfterQueuePlan);
        AddressUtils.waitForDestinationPlanApplied(new TimeoutBudget(5, TimeUnit.MINUTES), largeQueue);
        addressSettings = ArtemisUtils.getAddressSettings(kubernetes, addressSpace);
        assertEquals(Integer.valueOf("943718"), (Integer)addressSettings.get("maxSizeBytes"), "maxSizeBytes should be set");
    }
}
