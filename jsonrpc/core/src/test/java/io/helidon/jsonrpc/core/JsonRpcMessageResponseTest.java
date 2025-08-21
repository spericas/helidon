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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class JsonRpcMessageResponseTest {

    @Test
    void testCreateResultResponseFromJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("result", Json.createValue("success"));
        JsonObject jsonObject = builder.build();

        JsonRpcMessageResponse response = JsonRpcMessageResponse.create(jsonObject);
        assertThat(response.version(), is("2.0"));
        assertThat(response.rpcId().isPresent(), is(true));
        assertThat(response.rpcId().get(), is(Json.createValue("1")));
        assertThat(response.result().isPresent(), is(true));
        assertThat(response.result().get(), is(JsonRpcResult.create("success")));
    }

    @Test
    void testCreateErrorResponseFromJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("error", Json.createObjectBuilder()
                .add("code", -32000)
                .add("message", "Bad Request")
                .build());
        JsonObject jsonObject = builder.build();

        JsonRpcMessageResponse response = JsonRpcMessageResponse.create(jsonObject);
        assertThat(response.version(), is("2.0"));
        assertThat(response.rpcId().isPresent(), is(true));
        assertThat(response.rpcId().get(), is(Json.createValue("1")));
        assertThat(response.result().isPresent(), is(false));
        assertThat(response.error().isPresent(), is(true));
        assertThat(response.error().get(), is(JsonRpcError.create(-32000, "Bad Request")));
    }

    @Test
    void testCreateResultResponse() {
        JsonRpcMessageResponse response = JsonRpcMessageResponse.create("1");
        response.result(Json.createValue("success"));

        assertThat(response.version(), is("2.0"));
        assertThat(response.rpcId().isPresent(), is(true));
        assertThat(response.rpcId().get(), is(Json.createValue("1")));
        assertThat(response.error().isPresent(), is(false));
        assertThat(response.result().isPresent(), is(true));
        assertThat(response.result().get(), is(JsonRpcResult.create("success")));
    }

    @Test
    void testCreateErrorResponse() {
        JsonRpcMessageResponse response = JsonRpcMessageResponse.create("1");
        response.error(-32000, "Bad Request");

        assertThat(response.version(), is("2.0"));
        assertThat(response.rpcId().isPresent(), is(true));
        assertThat(response.rpcId().get(), is(Json.createValue("1")));
        assertThat(response.result().isPresent(), is(false));
        assertThat(response.error().isPresent(), is(true));
        assertThat(response.error().get(), is(JsonRpcError.create(-32000, "Bad Request")));
    }
}

