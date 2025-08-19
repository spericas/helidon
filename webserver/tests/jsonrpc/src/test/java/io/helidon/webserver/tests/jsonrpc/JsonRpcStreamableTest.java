/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.webserver.tests.jsonrpc;

import java.time.Duration;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Status;
import io.helidon.http.sse.SseEvent;
import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.jsonrpc.core.JsonRpcMessageType;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webclient.sse.SseSource;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.jsonrpc.JsonRpcHandlers;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRouting;
import io.helidon.webserver.jsonrpc.JsonRpcRules;
import io.helidon.webserver.jsonrpc.JsonRpcService;
import io.helidon.webserver.jsonrpc.JsonRpcSink;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ServerTest
class JsonRpcStreamableTest {

    static final int TIMEOUT = 5000;

    static final String MACHINE_START = """
            {"jsonrpc": "2.0",
                "method": "start",
                "params": { "when" : "NOW", "duration" : "PT0S" },
                "id": 1}
            """;

    static final String MACHINE_STARTED = """
                {"jsonrpc":"2.0","id":1,"result":{"status":"STARTED"}}
            """;

    static final String MACHINE_NOTIFICATION = """
            {"jsonrpc":"2.0","method":"start","params":{"status":"Received","message":"In progress"}}
            """;

    static final String MACHINE_REQUEST = """
            {"jsonrpc":"2.0","method":"userinfo","id":100,"params":{"scope":"All"}}
            """;

    private final Http1Client client;
    private final JsonRpcClient jsonRpcClient;

    JsonRpcStreamableTest(Http1Client client, JsonRpcClient jsonRpcClient) {
        this.client = client;
        this.jsonRpcClient = jsonRpcClient;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        JsonRpcRouting jsonRpcRouting = JsonRpcRouting.builder()
                .service(new JsonRpcStreamableService())
                .build();
        builder.register("/rpc", jsonRpcRouting);
    }

    @Test
    void testStart() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(1);

        try (var res = client.post("/rpc/machine")
                .contentType(MediaTypes.APPLICATION_JSON)
                .accept(MediaTypes.TEXT_EVENT_STREAM)
                .submit(MACHINE_START)) {

            res.source(SseSource.TYPE, new SseSource() {
                private int calls = 0;

                @Override
                public void onEvent(SseEvent event) {
                    switch (calls) {
                    case 0:
                        assertThat(event.data(String.class), is(MACHINE_NOTIFICATION.trim()));
                        break;
                    case 1:
                        assertThat(event.data(String.class), is(MACHINE_REQUEST.trim()));
                        break;
                    case 2:
                        assertThat(event.data(String.class), is(MACHINE_STARTED.trim()));
                        break;
                    default:
                        fail();
                    }
                    calls++;
                }

                @Override
                public void onClose() {
                    try {
                        barrier.await(TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        barrier.await(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    static class JsonRpcStreamableService implements JsonRpcService {

        @Override
        public void routing(JsonRpcRules rules) {
            rules.register("/machine",
                           JsonRpcHandlers.builder()
                                   .method("start", this::start)
                                   .build());
        }

        void start(JsonRpcRequest req, JsonRpcResponse res) throws Exception {
            StartStopParams params = req.params().as(StartStopParams.class);

            if (params.when().equals("NOW")) {
                // switch to SSE channel
                try (JsonRpcSink sink = res.sink(JsonRpcSink.TYPE)) {
                    // send a notification
                    sink.emit(builder -> {
                        builder.type(JsonRpcMessageType.NOTIFICATION);
                        builder.rpcMethod(req.rpcMethod());
                        builder.param("status", "Received");
                        builder.param("message", "In progress");
                    });

                    // send a request
                    sink.emit(builder -> {
                        builder.type(JsonRpcMessageType.REQUEST);
                        builder.rpcId(100);
                        builder.rpcMethod("userinfo");
                        builder.param("scope", "All");
                    });

                    // finally, send the response to the request
                    sink.emit(builder -> {
                        builder.type(JsonRpcMessageType.RESPONSE);
                        builder.rpcMethod(req.rpcMethod());
                        builder.result(new StartStopResult("STARTED"));
                    });
                }
            } else {
                // return single response with error
                res.error(JsonRpcError.INVALID_PARAMS, "Bad param")
                        .status(Status.OK_200)
                        .send();
            }
        }

        public record StartStopParams(String when, Duration duration) {
        }

        public record StartStopResult(String status) {
        }
    }
}
