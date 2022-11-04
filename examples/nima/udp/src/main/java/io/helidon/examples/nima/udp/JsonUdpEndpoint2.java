/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.examples.nima.udp;

import jakarta.inject.Inject;

import jakarta.json.JsonObject;

/**
 * Example of UDP declarative endpoint.
 */
@UdpEndpointInfo(port=8433)
class JsonUdpEndpoint2 {

    @Inject
    UdpClient sender;

    @Inject
    JsonProcessor processor;

    @Inject
    @InetAddressInfo(host="udp.oracle.com", port=8888)
    UdpClient forward;

    @OnMessage
    public void onMessage(JsonObject recv) {
        JsonObject sent = processor.process(recv);
        sender.sendMessage(sent);
        forward.sendMessage(sent);
    }
}
