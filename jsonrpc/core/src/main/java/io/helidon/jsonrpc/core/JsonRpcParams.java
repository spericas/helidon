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

import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

/**
 * A representation of a JSON-RPC params.
 */
public interface JsonRpcParams {

    /**
     * Create an instance from a JSON structure.
     *
     * @param params the structure
     * @return a new instance of this class
     */
    static JsonRpcParams create(JsonStructure params) {
        return new JsonRpcParamsImpl(params);
    }

    /**
     * Access all request params as a JSON structure.
     *
     * @return a JSON structure
     */
    JsonStructure asJsonStructure();

    /**
     * Access all request params as a single JSON object.
     *
     * @return a JSON object
     * @throws ClassCastException if not a JSON object
     */
    JsonObject asJsonObject();

    /**
     * Access all request params as a single JSON array.
     *
     * @return a JSON array
     * @throws ClassCastException if not a JSON array
     */
    JsonArray asJsonArray();

    /**
     * Get a single param by name as a JSON value.
     *
     * @param name param name
     * @return the param value
     * @throws ClassCastException       if not a JSON object
     * @throws IllegalArgumentException if the param does not exist
     */
    JsonValue get(String name);

    /**
     * Get a single param by name as a string.
     *
     * @param name param name
     * @return the param value as a string
     * @throws ClassCastException       if not a JSON object or value not a string
     * @throws IllegalArgumentException if the param does not exist
     */
    String getString(String name);

    /**
     * Get a single param by name as a JSON value, if present.
     *
     * @param name param name
     * @return an optional param value
     * @throws ClassCastException if not a JSON object
     */
    Optional<JsonValue> find(String name);

    /**
     * Set a param by name using a string.
     *
     * @param name param name
     * @param value param value
     * @return this instance
     */
    default JsonRpcParams put(String name, String value) {
        return put(name, Json.createValue(value));
    }

    /**
     * Set a param by name using a JSON value.
     *
     * @param name param name
     * @param value param value
     * @return this instance
     */
    JsonRpcParams put(String name, JsonValue value);

    /**
     * Get a single array param by index as a JSON value.
     *
     * @param index the index
     * @return the param value
     * @throws ClassCastException        if not a JSON array
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    JsonValue get(int index);

    /**
     * Get a single array param by index as a string.
     *
     * @param index the index
     * @return the param value as a string
     * @throws ClassCastException        if not a JSON array or value not a string
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    String getString(int index);

    /**
     * Get a single array param by index as a JSON value, if present.
     *
     * @param index the index
     * @return an optional param value
     * @throws ClassCastException        if not a JSON array
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    Optional<JsonValue> find(int index);

    /**
     * Set an array param by index using a string.
     *
     * @param index the index
     * @param value param value
     * @return this instance
     */
    default JsonRpcParams put(int index, String value) {
        return put(index, Json.createValue(value));
    }

    /**
     * Set an array param by index using a JSON value.
     *
     * @param index the index
     * @param value param value
     * @return this instance
     */
    JsonRpcParams put(int index, JsonValue value);

    /**
     * Map all request params to a bean class type using JSONB.
     *
     * @param type the bean class
     * @param <T>  the bean type
     * @return an instance of the bean type
     * @throws jakarta.json.bind.JsonbException if an error occurs during mapping
     */
    <T> T as(Class<T> type);
}
