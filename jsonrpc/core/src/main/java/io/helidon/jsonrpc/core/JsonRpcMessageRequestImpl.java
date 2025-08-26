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

import java.util.Objects;
import java.util.Optional;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

/**
 * An implementation of a JSON-RPC message request.
 */
class JsonRpcMessageRequestImpl implements JsonRpcMessageRequest {

    private String rpcMethod;
    private JsonValue rpcId;
    private JsonRpcParams params = new JsonRpcParamsImpl();

    JsonRpcMessageRequestImpl(String rpcMethod) {
        this(rpcMethod, null);
    }

    JsonRpcMessageRequestImpl(String rpcMethod, JsonValue rpcId) {
        this.rpcMethod = Objects.requireNonNull(rpcMethod);
        this.rpcId = rpcId;
    }

    JsonRpcMessageRequestImpl(JsonObject jsonObject) {
        if (!"2.0".equals(jsonObject.getString("jsonrpc"))) {
            throw new IllegalArgumentException("Only JSON-RPC versions 2.0 is supported");
        }
        this.rpcMethod = Objects.requireNonNull(jsonObject.getString("method", ""));
        if (jsonObject.containsKey("id")) {
            this.rpcId = jsonObject.get("id");
        }
        JsonValue params = jsonObject.get("params");
        if (params instanceof JsonStructure structure) {
            this.params = new JsonRpcParamsImpl(structure);
        } else if (params != null) {
            throw new IllegalArgumentException("Params must be of type JsonStructure");
        }
    }

    @Override
    public String rpcMethod() {
        return rpcMethod;
    }

    @Override
    public JsonRpcMessageRequest rpcMethod(String rpcMethod) {
        this.rpcMethod = rpcMethod;
        return this;
    }

    @Override
    public Optional<JsonValue> rpcId() {
        return Optional.ofNullable(rpcId);
    }

    @Override
    public JsonRpcMessageRequest rpcId(JsonValue rpcId) {
        this.rpcId = rpcId;
        return this;
    }

    @Override
    public JsonRpcParams params() {
        return params;
    }

    @Override
    public JsonObject asJsonObject() {
        JsonObjectBuilder builder = JsonUtil.JSON_BUILDER_FACTORY.createObjectBuilder();
        builder.add("version", "2.0");
        if (!rpcMethod.isEmpty()) {
            builder.add("method", rpcMethod);
        }
        builder.add("id", rpcId);

        return builder.build();
    }
}
