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
import jakarta.json.JsonValue;

/**
 * A representation of a JSON-RPC request.
 */
public interface JsonRpcMessageRequest extends JsonRpcMessage {

    /**
     * Create an instance of this type.
     *
     * @param rpcMethod the method name
     * @return a newly created instance
     */
    static JsonRpcMessageRequest create(String rpcMethod) {
        return new JsonRpcMessageRequestImpl(rpcMethod, null);
    }

    /**
     * Create an instance of this type.
     *
     * @param rpcMethod the method name
     * @param rpcId the JSON-RPC ID
     * @return a newly created instance
     */
    static JsonRpcMessageRequest create(String rpcMethod, JsonValue rpcId) {
        return new JsonRpcMessageRequestImpl(rpcMethod, rpcId);
    }

    /**
     * Create an instance of this type from a JSON object.
     *
     * @param jsonObject the JSON object
     * @return the newly created instance
     */
    static JsonRpcMessageRequest create(JsonObject jsonObject) {
        return new JsonRpcMessageRequestImpl(jsonObject);
    }

    @Override
    JsonRpcMessageRequest rpcId(JsonValue rpcId);

    @Override
    default JsonRpcMessageRequest rpcId(int rpcId) {
        return rpcId(Json.createValue(rpcId));
    }

    default JsonRpcMessageRequest rpcId(String rpcId) {
        return rpcId(Json.createValue(rpcId));
    }

    /**
     * Set the JSON-RPC method name.
     *
     * @param rpcMethod new name
     * @return this message
     */
    @Override
    JsonRpcMessageRequest rpcMethod(String rpcMethod);

    /**
     * The params associated with the request. If omitted in the request, then
     * internally initialized using {@link jakarta.json.JsonValue#EMPTY_JSON_OBJECT}.
     *
     * @return the params
     */
    JsonRpcParams params();
}
