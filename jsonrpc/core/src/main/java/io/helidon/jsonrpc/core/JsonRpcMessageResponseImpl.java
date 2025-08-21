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
import jakarta.json.JsonValue;

import static io.helidon.jsonrpc.core.JsonUtil.JSON_BUILDER_FACTORY;

/**
 * An implementation of JSON-RPC response.
 */
class JsonRpcMessageResponseImpl implements JsonRpcMessageResponse {

    private JsonValue rpcId;
    private JsonRpcResult result;
    private JsonRpcError error;

    JsonRpcMessageResponseImpl(JsonValue rpcId) {
        this.rpcId = rpcId;
    }

    JsonRpcMessageResponseImpl(JsonObject jsonObject) {
        if (!"2.0".equals(jsonObject.getString("jsonrpc"))) {
            throw new IllegalArgumentException("Only JSON-RPC versions 2.0 is supported");
        }
        this.rpcId = jsonObject.get("id");
        if (jsonObject.containsKey("result")) {
            this.result = new JsonRpcResultImpl(jsonObject.get("result"));
        }
        if (jsonObject.containsKey("error")) {
            this.error = new JsonRpcErrorImpl(jsonObject.getJsonObject("error"));
        }
    }

    @Override
    public Optional<JsonValue> rpcId() {
        return Optional.ofNullable(rpcId);
    }

    @Override
    public JsonRpcMessageResponse rpcId(JsonValue rpcId) {
        this.rpcId = rpcId;
        return this;
    }

    @Override
    public Optional<JsonRpcResult> result() {
        return Optional.ofNullable(result);
    }

    @Override
    public JsonRpcMessageResponse result(JsonValue result) {
        this.result = result == null ? null : new JsonRpcResultImpl(result);
        return this;
    }

    @Override
    public Optional<JsonRpcError> error() {
        return Optional.ofNullable(error);
    }

    @Override
    public JsonRpcMessageResponse error(int code, String message) {
        Objects.requireNonNull(message, "message is null");
        error = JsonRpcError.create(code, message);
        return this;
    }

    @Override
    public JsonRpcMessageResponse error(int code, String message, JsonValue data) {
        Objects.requireNonNull(message, "message is null");
        Objects.requireNonNull(data, "data is null");
        error = JsonRpcError.create(code, message, data);
        return this;
    }

    @Override
    public JsonRpcMessageResponse error(JsonRpcError error) {
        this.error = error;
        return this;
    }

    @Override
    public JsonRpcMessageResponse result(Object object) {
        result = object == null ? null : new JsonRpcResultImpl(JsonUtil.jsonbToJsonp(object));
        return this;
    }

    @Override
    public JsonRpcMessageResponse result(JsonRpcResult result) {
        this.result = result;
        return this;
    }

    @Override
    public JsonObject asJsonObject() {
        JsonObjectBuilder builder = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0");
        if (rpcId != null) {
            builder.add("id", rpcId);
        }
        if (result != null) {
            builder.add("result", result.asJsonValue());
        } else if (error != null) {
            builder.add("error", error.asJsonObject());
        }
        return builder.build();
    }
}
