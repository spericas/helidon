/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.webserver.tests.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;
import io.helidon.webserver.udp.UdpEndpoint;
import io.helidon.webserver.udp.UdpMessage;

import org.junit.jupiter.api.Test;

import static java.lang.System.Logger.Level.INFO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Simple UDP echo test.
 */
@ServerTest(udp = true)
class UdpSimpleTest extends UdpBaseTest {
    private static final System.Logger LOGGER = System.getLogger(UdpSimpleTest.class.getName());

    private final InetSocketAddress address;

    public UdpSimpleTest(WebServer webServer) {
        this.address = new InetSocketAddress("localhost", webServer.port());
    }

    @SetUpServer
    static void setupServer(WebServerConfig.Builder builder) {
        builder.udpEndpoint(new EchoService());
    }

    @Test
    void testEndpoint() throws Exception {
        echoMessage("hello", address);
        echoMessage("how are you?", address);
        echoMessage("good bye", address);
    }

    @Test
    void testEndpointConnected() throws Exception {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.connect(address);
            assertThat(channel.isConnected(), is(true));
            echoMessageOnChannel("hello", channel, address);
            echoMessageOnChannel("how are you?", channel, address);
            echoMessageOnChannel("good bye", channel, address);
            channel.disconnect();
            assertThat(channel.isConnected(), is(false));
        }
    }

    static class EchoService implements UdpEndpoint {

        @Override
        public void onMessage(UdpMessage message) {
            try {
                String str = message.as(String.class);
                LOGGER.log(INFO, "Server RCV: " + str);
                message.udpClient().sendMessage(str);
                LOGGER.log(INFO, "Server SND: " + str);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
