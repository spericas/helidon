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

class JsonRpcMessageRequestTest {

    @Test
    void testCreateRequestFromJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("method", "test");
        JsonObject jsonObject = builder.build();

        JsonRpcMessageRequest request = JsonRpcMessageRequest.create(jsonObject);
        assertThat(request.version(), is("2.0"));
        assertThat(request.rpcId().isPresent(), is(true));
        assertThat(request.rpcId().get(), is(Json.createValue("1")));
        assertThat(request.rpcMethod(), is("test"));
        assertThat(request.params().asJsonObject().size(), is(0));
    }

    @Test
    void testCreateRequestObjParamsFromJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("method", "test");
        builder.add("params", Json.createObjectBuilder()
                .add("foo", "bar")
                .add("bar", "baz")
                .build());
        JsonObject jsonObject = builder.build();

        JsonRpcMessageRequest request = JsonRpcMessageRequest.create(jsonObject);
        assertThat(request.version(), is("2.0"));
        assertThat(request.rpcId().isPresent(), is(true));
        assertThat(request.rpcId().get(), is(Json.createValue("1")));
        assertThat(request.rpcMethod(), is("test"));
        assertThat(request.params().asJsonObject().size(), is(2));
        assertThat(request.params().get("foo"), is(Json.createValue("bar")));
        assertThat(request.params().get("bar"), is(Json.createValue("baz")));
    }

    @Test
    void testCreateRequestArrayParamsFromJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("id", "1");
        builder.add("method", "test");
        builder.add("params", Json.createArrayBuilder()
                .add(0, "foo")
                .add(1, "bar")
                .build());
        JsonObject jsonObject = builder.build();

        JsonRpcMessageRequest request = JsonRpcMessageRequest.create(jsonObject);
        assertThat(request.version(), is("2.0"));
        assertThat(request.rpcId().isPresent(), is(true));
        assertThat(request.rpcId().get(), is(Json.createValue("1")));
        assertThat(request.rpcMethod(), is("test"));
        assertThat(request.params().asJsonArray().size(), is(2));
        assertThat(request.params().get(0), is(Json.createValue("foo")));
        assertThat(request.params().get(1), is(Json.createValue("bar")));
    }

    @Test
    void testCreateRequest() {
        JsonRpcMessageRequest request = JsonRpcMessageRequest.create("start");
        request.rpcId(1);
        assertThat(request.version(), is("2.0"));
        assertThat(request.rpcId().isPresent(), is(true));
        assertThat(request.rpcId().get(), is(Json.createValue(1)));
        assertThat(request.rpcMethod(), is("start"));
    }

    @Test
    void testCreateRequestWithObjParams() {
        JsonRpcMessageRequest request = JsonRpcMessageRequest.create("start");
        request.rpcId(1)
                .params()
                .put("foo", "bar")
                .put("bar", "baz");

        assertThat(request.version(), is("2.0"));
        assertThat(request.rpcId().isPresent(), is(true));
        assertThat(request.rpcId().get(), is(Json.createValue(1)));
        assertThat(request.rpcMethod(), is("start"));
        assertThat(request.params().asJsonObject().size(), is(2));
        assertThat(request.params().get("foo"), is(Json.createValue("bar")));
        assertThat(request.params().get("bar"), is(Json.createValue("baz")));
    }

    @Test
    void testCreateRequestWithArrayParams() {
        JsonRpcMessageRequest request = JsonRpcMessageRequest.create("start");
        request.rpcId(1)
                .params()
                .put(0, "foo")
                .put(1, "bar");

        assertThat(request.version(), is("2.0"));
        assertThat(request.rpcId().isPresent(), is(true));
        assertThat(request.rpcId().get(), is(Json.createValue(1)));
        assertThat(request.rpcMethod(), is("start"));
        assertThat(request.params().asJsonArray().size(), is(2));
        assertThat(request.params().get(0), is(Json.createValue("foo")));
        assertThat(request.params().get(1), is(Json.createValue("bar")));
    }
}
