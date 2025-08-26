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

import jakarta.json.JsonObject;

/**
 * A JSON-RPC notification.
 */
public interface JsonRpcMessageNotification extends JsonRpcMessage {

    /**
     * Create a JSON-RPC notification using a method name.
     *
     * @param rpcMethod the method name
     * @return a notification
     */
    static JsonRpcMessageNotification create(String rpcMethod) {
        return new JsonRpcMessageNotificationImpl(rpcMethod);
    }

    /**
     * Create a JSON-RPC notification from a JSON object.
     *
     * @param jsonObject the JSON object
     * @return a notification
     */
    static JsonRpcMessageNotification create(JsonObject jsonObject) {
        return  new JsonRpcMessageNotificationImpl(jsonObject);
    }
}
