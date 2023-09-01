/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.webserver;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webserver.utils.SocketHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class ProxyProtocolTest extends BaseServerTest {

    @BeforeAll
    static void startServer() throws Exception {
        Config config = Config.builder()
                .addSource(ConfigSources.classpath("proxy/application.yaml").build().get())
                .build();
        Routing routing = Routing.builder()
                .get("/foo", (req, res) -> res.send("OK")).build();
        startServer(0, routing, config.get("server"));
    }

    @Test
    void testProxyProtocol() throws Exception {
        String s = SocketHttpClient.sendAndReceive(
                "PROXY TCP4 192.168.0.1 192.168.0.2 56324 8080",
                "/foo",
                Http.Method.GET,
                null,
                webServer());
        assertThat(s, containsString("OK"));
    }
}
