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
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * A representation of a JSON-RPC response.
 */
public interface JsonRpcMessageResponse extends JsonRpcMessage {

    /**
     * Create an instance of this class from a string.
     *
     * @param rpcId the ID
     * @return the newly created instance
     */
    static JsonRpcMessageResponse create(String rpcId) {
        return new JsonRpcMessageResponseImpl(Json.createValue(rpcId));
    }

    /**
     * Create an instance of this class from an int.
     *
     * @param rpcId the ID
     * @return the newly created instance
     */
    static JsonRpcMessageResponse create(int rpcId) {
        return new JsonRpcMessageResponseImpl(Json.createValue(rpcId));
    }

    /**
     * Create an instance of this class from an ID.
     *
     * @param rpcId the ID
     * @return the newly created instance
     */
    static JsonRpcMessageResponse create(JsonValue rpcId) {
        return new JsonRpcMessageResponseImpl(rpcId);
    }

    /**
     * Create an instance of this type from a JSON object.
     *
     * @param jsonObject the JSON object
     * @return the newly created instance
     */
    static JsonRpcMessageResponse create(JsonObject jsonObject) {
        return new JsonRpcMessageResponseImpl(jsonObject);
    }

    @Override
    default JsonRpcMessage rpcMethod(String rpcMethod) {
        throw new UnsupportedOperationException("Cannot set method name");
    }

    @Override
    JsonRpcMessageResponse rpcId(JsonValue rpcId);

    @Override
    default JsonRpcMessageResponse rpcId(int rpcId) {
        return rpcId(Json.createValue(rpcId));
    }
    @Override
    default JsonRpcMessageResponse rpcId(String rpcId) {
        return rpcId(Json.createValue(rpcId));
    }

    /**
     * Set a result for this response as a JSON value.
     *
     * @param result the result
     * @return this response
     * @see #error()
     */
    JsonRpcMessageResponse result(JsonValue result);

    /**
     * Set a result for this response.
     *
     * @param result the result
     * @return this response
     * @see #error()
     */
    JsonRpcMessageResponse result(JsonRpcResult result);

    /**
     * Set a result as an arbitrary object that can be mapped to JSON. This
     * method will serialize the parameter using JSONB.
     *
     * @param object the object
     * @return this response
     * @throws jakarta.json.JsonException if an error occurs during serialization
     * @see #error()
     */
    JsonRpcMessageResponse result(Object object);

    /**
     * Set a JSON-RPC error on this response with a code and a message.
     *
     * @param code the error code
     * @param message the error message
     * @return this response
     * @see #result()
     */
    JsonRpcMessageResponse error(int code, String message);

    /**
     * Set a JSON-RPC error on this response with a code, a message and
     * some associated data.
     *
     * @param code the error code
     * @param message the error message
     * @param data the data
     * @return this response
     * @see #result()
     */
    JsonRpcMessageResponse error(int code, String message, JsonValue data);

    /**
     * Set a JSON-RPC error on this response.
     *
     * @param error the error
     * @return this response
     * @see #result()
     */
    JsonRpcMessageResponse error(JsonRpcError error);

    /**
     * Get the result set on this response.
     *
     * @return the result
     */
    Optional<JsonRpcResult> result();

    /**
     * Get an error set on this response.
     *
     * @return the error
     */
    Optional<JsonRpcError> error();
}
