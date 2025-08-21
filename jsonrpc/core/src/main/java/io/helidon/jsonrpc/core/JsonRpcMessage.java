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
import jakarta.json.JsonValue;

/**
 * A JSON-RPC message that can be a request or a response.
 */
public interface JsonRpcMessage {

    /**
     * Create either a JSON-RPC request or response from a JSON object.
     *
     * @param jsonObject the JSON object
     * @return a request or a response
     * @throws java.lang.IllegalArgumentException if the type of message is unknown
     */
    static JsonRpcMessage create(JsonObject jsonObject) {
        Objects.requireNonNull(jsonObject);
        if (jsonObject.containsKey("method")) {
            return new JsonRpcMessageRequestImpl(jsonObject);
        }
        if (jsonObject.containsKey("result") || jsonObject.containsKey("error")) {
            return new JsonRpcMessageResponseImpl(jsonObject);
        }
        throw new IllegalArgumentException("Unable to construct JsonRpcMessage");
    }

    /**
     * The request version. Must always be "2.0".
     *
     * @return the request version
     */
    default String version() {
        return "2.0";
    }

    /**
     * The JSON-RPC method name.
     *
     * @return the request method or {@code ""} for a response
     */
    default String rpcMethod() {
        return "";
    }

    /**
     * Set the JSON-RPC method name.
     *
     * @param rpcMethod new name
     * @return this message
     */
    JsonRpcMessage rpcMethod(String rpcMethod);

    /**
     * The JSON-RPC ID.
     *
     * @return an optional request ID
     */
    Optional<JsonValue> rpcId();

    /**
     * Set a JSON-RPC ID for this response.
     *
     * @param rpcId the ID
     * @return this response
     */
    JsonRpcMessage rpcId(JsonValue rpcId);

    /**
     * Set a JSON-RPC ID for this response as an int.
     *
     * @param rpcId the ID
     * @return this response
     */
    JsonRpcMessage rpcId(int rpcId);

    /**
     * Set a JSON-RPC ID for this response as a string.
     *
     * @param rpcId the ID
     * @return this response
     */
    JsonRpcMessage rpcId(String rpcId);

    /**
     * Get a complete message as a JSON object.
     *
     * @return a JSON object that represents the request
     */
    JsonObject asJsonObject();
}
