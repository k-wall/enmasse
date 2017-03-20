/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.controller.api.v1v2common;

import enmasse.controller.address.AddressManager;
import enmasse.controller.address.AddressManagerFactory;
import enmasse.controller.model.Destination;
import enmasse.controller.model.DestinationGroup;
import enmasse.controller.model.InstanceId;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class RestServiceBase {
    private static final Logger log = LoggerFactory.getLogger(RestServiceBase.class.getName());

    private final InstanceId instanceId;
    private final AddressManagerFactory addressManagerFactory;
    private final Vertx vertx;

    public RestServiceBase(@Context InstanceId instanceId, @Context AddressManagerFactory addressManagerFactory, @Context Vertx vertx) {
        this.instanceId = instanceId;
        this.addressManagerFactory = addressManagerFactory;
        this.vertx = vertx;
    }

    private AddressManager getAddressManager() {
        return addressManagerFactory.getOrCreateAddressManager(instanceId);
    }

    protected void getAddresses(@Suspended final AsyncResponse response) {
        vertx.executeBlocking(promise -> {
            try {
                promise.complete(getResponseEntity(getAddressManager().listDestinationGroups()));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                response.resume(Response.ok(result.result(), MediaType.APPLICATION_JSON_TYPE).build());
            } else {
                log.error("Error retrieving addresses", result.cause());
                response.resume(Response.serverError().build());
            }
        });
    }

    protected void putAddresses(Set<DestinationGroup> destinationGroups, @Suspended final AsyncResponse response) {
        vertx.executeBlocking(promise -> {
            try {
                getAddressManager().destinationsUpdated(destinationGroups);
                promise.complete(getResponseEntity(destinationGroups));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                response.resume(Response.ok(result.result(), MediaType.APPLICATION_JSON_TYPE).build());
            } else {
                log.error("Error replacing addresses", result.cause());
                response.resume(Response.serverError().build());
            }
        });
    }

    protected void deleteAddresses(List<String> data, @Suspended final AsyncResponse response) {
        vertx.executeBlocking(promise -> {
            try {
                AddressManager addressManager = getAddressManager();
                Set<DestinationGroup> destinationGroups = addressManager.listDestinationGroups();

                for (String address : data) {
                    destinationGroups = deleteAddressFromSet(address, destinationGroups);
                }
                addressManager.destinationsUpdated(destinationGroups);
                promise.complete(getResponseEntity(destinationGroups));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                response.resume(Response.ok(result.result()).build());
            } else {
                log.error("Error deleting addresses", result.cause());
                response.resume(Response.serverError().build());
            }
        });
    }

    private static Set<DestinationGroup> deleteAddressFromSet(String address, Set<DestinationGroup> destinationGroups) {
        Set<DestinationGroup> newDestinations = new HashSet<>(destinationGroups);
        for (DestinationGroup destinationGroup : destinationGroups) {
            DestinationGroup.Builder groupBuilder = new DestinationGroup.Builder(destinationGroup.getGroupId());
            for (Destination destination : destinationGroup.getDestinations()) {
                if (!destination.address().equals(address)) {
                    groupBuilder.destination(new Destination(address, destinationGroup.getGroupId(), destination.storeAndForward(), destination.multicast(), destination.flavor(), destination.uuid()));
                }
            }
            DestinationGroup newGroup = groupBuilder.build();
            if (newGroup.getDestinations().size() > 0) {
                newDestinations.add(newGroup);
            }
        }
        return newDestinations;
    }

    protected void appendAddresses(Set<DestinationGroup> newDestinationGroups, @Suspended final AsyncResponse response) {
        vertx.executeBlocking(promise -> {
            try {
                AddressManager addressManager = getAddressManager();
                Set<DestinationGroup> destinationGroups = new HashSet<>(addressManager.listDestinationGroups());
                destinationGroups.addAll(newDestinationGroups);
                addressManager.destinationsUpdated(destinationGroups);

                promise.complete(getResponseEntity(destinationGroups));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                response.resume(Response.ok(result.result()).build());
            } else {
                log.error("Error appending addresses", result.cause());
                response.resume(Response.serverError().build());
            }
        });
    }

    protected abstract Object getResponseEntity(Collection<DestinationGroup> destinationGroups);
}
