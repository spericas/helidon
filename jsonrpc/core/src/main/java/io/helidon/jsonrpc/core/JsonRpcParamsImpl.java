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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

/**
 * An implementation of {@link io.helidon.jsonrpc.core.JsonRpcParams}.
 */
class JsonRpcParamsImpl implements JsonRpcParams {

    private final ArrayList<JsonValue> arrayParams = new ArrayList<>();
    private final Map<String, JsonValue> mapParams = new HashMap<>();

    JsonRpcParamsImpl() {
    }

    JsonRpcParamsImpl(JsonStructure params) {
        if (params instanceof JsonObject object) {
            mapParams.putAll(object);
        } else if (params instanceof JsonArray array) {
            arrayParams.addAll(array);
        } else {
            throw new IllegalArgumentException("Invalid params in JsonRpcParamsImpl");
        }
    }

    @Override
    public JsonValue get(String name) {
        JsonValue value = mapParams.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Unable to find param " + name);
        }
        return value;
    }

    @Override
    public String getString(String name) {
        return ((JsonString) get(name)).getString();
    }

    @Override
    public Optional<JsonValue> find(String name) {
        return Optional.ofNullable(mapParams.get(name));
    }

    @Override
    public JsonRpcParams put(String name, JsonValue value) {
        mapParams.put(name, value);
        return this;
    }

    @Override
    public JsonValue get(int index) {
        return arrayParams.get(index);
    }

    @Override
    public String getString(int index) {
        return ((JsonString) get(index)).getString();
    }

    @Override
    public Optional<JsonValue> find(int index) {
        if (index >= 0 && index < arrayParams.size()) {
            return Optional.of(arrayParams.get(index));
        }
        return Optional.empty();
    }

    @Override
    public JsonRpcParams put(int index, JsonValue value) {
        arrayParams.add(index, value);
        return this;
    }

    @Override
    public <T> T as(Class<T> type) {
        return JsonUtil.jsonpToJsonb(asJsonObject(), type);
    }

    @Override
    public JsonStructure asJsonStructure() {
        return !arrayParams.isEmpty() ? asJsonArray() : asJsonObject();
    }

    @Override
    public JsonObject asJsonObject() {
        JsonObjectBuilder builder = JsonUtil.JSON_BUILDER_FACTORY.createObjectBuilder();
        for (Map.Entry<String, JsonValue> entry : mapParams.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    @Override
    public JsonArray asJsonArray() {
        JsonArrayBuilder builder = JsonUtil.JSON_BUILDER_FACTORY.createArrayBuilder();
        for (JsonValue value : arrayParams) {
            builder.add(value);
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JsonRpcParamsImpl that)) {
            return false;
        }
        return Objects.equals(arrayParams, that.arrayParams) && Objects.equals(mapParams, that.mapParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arrayParams, mapParams);
    }
}
