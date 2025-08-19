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

import java.util.Map;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import jakarta.json.JsonStructure;

/**
 * This type represents a JSON-RPC message that be either a request,
 * a response or a notification.
 */
@Prototype.Blueprint
interface JsonRpcMessageBlueprint {

    /**
     * The type of this JSON-RPC message.
     *
     * @return the type
     */
    @Option.Default("RESPONSE")
    JsonRpcMessageType type();

    /**
     * The JSON-RPC message method name.
     *
     * @return the request method
     */
    String rpcMethod();

    /**
     * The JSON-RPC message ID.
     *
     * @return an optional request ID
     */
    Optional<Object> rpcId();

    /**
     * The JSON-RPC message named params.
     *
     * @return the params
     */
    @Option.Singular(value = "param", withPrefix = false)
    Map<String, Object> params();

    /**
     * An alternative to {@link #params} where params are provided as a JSON
     * structure. If available, it takes precedence over {@link #params()}.
     *
     * @return an option JSON structure that represents the params
     */
    Optional<JsonStructure> param();

    /**
     * The JSON-RPC message result.
     *
     * @return the optional result
     */
    Optional<Object> result();

    /**
     * The JSON-RPC message error.
     *
     * @return the optional error
     */
    Optional<JsonRpcError> error();
}
