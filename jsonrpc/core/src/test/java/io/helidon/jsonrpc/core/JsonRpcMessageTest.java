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
package io.helidon.jsonrpc.core;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonRpcMessageTest {

    @Test
    void testCreateRequest() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("method", "test");
        JsonObject jsonObject = builder.build();
        assertThat(JsonRpcMessage.create(jsonObject), instanceOf(JsonRpcMessageRequest.class));
    }

    @Test
    void testCreateResultResponse() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("result", "test");
        JsonObject jsonObject = builder.build();
        assertThat(JsonRpcMessage.create(jsonObject), instanceOf(JsonRpcMessageResponse.class));
    }

    @Test
    void testCreateErrorResponse() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("error", Json.createObjectBuilder()
                .add("code", -32000)
                .add("message", "There was an error")
                .build());
        JsonObject jsonObject = builder.build();
        assertThat(JsonRpcMessage.create(jsonObject), instanceOf(JsonRpcMessageResponse.class));
    }

    @Test
    void testBadMessage() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        JsonObject jsonObject = builder.build();
        assertThrows(IllegalArgumentException.class, () -> JsonRpcMessage.create(jsonObject));
    }
}
