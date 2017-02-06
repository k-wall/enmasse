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

package enmasse.mqtt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Vert.x based MQTT Last Will and Testament service for EnMasse
 */
@Component
public class MqttLwt extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttLwt.class);

    // connection info to the messaging service
    private String messagingServiceHost;
    private int messagingServiceInternalPort;

    /**
     * Set the address for connecting to the AMQP internal network
     *
     * @param messagingServiceHost   address for AMQP connection
     * @return  current MQTT LWT instance
     */
    @Value(value = "${messaging.service.host:0.0.0.0}")
    public MqttLwt setMessagingServiceHost(String messagingServiceHost) {
        this.messagingServiceHost = messagingServiceHost;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP internal network
     *
     * @param messagingServiceInternalPort   port for AMQP connection
     * @return  current MQTT LWT instance
     */
    @Value(value = "${messaging.service..internal.port:55673}")
    public MqttLwt setMessagingServiceInternalPort(int messagingServiceInternalPort) {
        this.messagingServiceInternalPort = messagingServiceInternalPort;
        return this;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        LOG.info("Starting MQTT LWT service verticle...");
        // TODO
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        LOG.info("Stopping MQTT LWT service verticle...");
        // TODO
    }
}
