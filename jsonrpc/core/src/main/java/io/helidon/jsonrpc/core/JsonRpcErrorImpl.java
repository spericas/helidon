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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * An implementation of a JSON-RPC error.
 */
class JsonRpcErrorImpl implements JsonRpcError {

    private final int code;
    private final String message;
    private final JsonValue data;

    JsonRpcErrorImpl(int code, String message, JsonValue data) {
        this.code = code;
        this.message = Objects.requireNonNull(message, "message is null");
        this.data = data;
    }

    JsonRpcErrorImpl(JsonObject jsonObject) {
        Objects.requireNonNull(jsonObject);
        this.code = jsonObject.getInt("code");
        this.message = Objects.requireNonNull(jsonObject.getString("message"));
        this.data = jsonObject.get("data");
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Optional<JsonValue> data() {
        return Optional.ofNullable(data);
    }

    @Override
    public <T> Optional<T> dataAs(Class<T> type) {
        return data == null ? Optional.empty()
                : Optional.of(JsonUtil.jsonpToJsonb(data.asJsonObject(), type));
    }

    @Override
    public JsonObject asJsonObject() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("code", code);
        builder.add("message", message);
        if (data != null) {
            builder.add("data", data);
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JsonRpcErrorImpl that)) {
            return false;
        }
        return code == that.code && Objects.equals(message, that.message) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message, data);
    }
}
