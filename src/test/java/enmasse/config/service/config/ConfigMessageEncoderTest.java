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

package enmasse.config.service.config;

import enmasse.config.service.TestResource;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigMessageEncoderTest {
    @Test
    public void testEncoder() throws IOException {
        ConfigMessageEncoder encoder = new ConfigMessageEncoder();

        Set<ConfigResource> configSet = new LinkedHashSet<>(Arrays.asList(
                new ConfigResource(new TestResource.TestValue("r1", AddressConfigCodec.encodeLabels(true, false), AddressConfigCodec.encodeAnnotations("[\"myqueue\", \"myqueue2\"]"), "v1")),
                new ConfigResource(new TestResource.TestValue("r2", AddressConfigCodec.encodeLabels(true, true), AddressConfigCodec.encodeAnnotations("[\"mytopic\"]"), "v2"))));

        Message message = encoder.encode(configSet);
        String json = (String) ((AmqpValue) message.getBody()).getValue();
        assertThat(json, is("{\"myqueue\":{\"store_and_forward\":true,\"multicast\":false},\"myqueue2\":{\"store_and_forward\":true,\"multicast\":false},\"mytopic\":{\"store_and_forward\":true,\"multicast\":true}}"));
    }
}
