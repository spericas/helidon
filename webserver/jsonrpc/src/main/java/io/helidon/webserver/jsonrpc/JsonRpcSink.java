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
package io.helidon.webserver.jsonrpc;

import java.util.function.Consumer;

import io.helidon.common.GenericType;
import io.helidon.jsonrpc.core.JsonRpcMessage;
import io.helidon.webserver.http.spi.Sink;

/**
 * An SSE sink for {@link io.helidon.jsonrpc.core.JsonRpcMessage}s.
 */
public interface JsonRpcSink extends Sink<JsonRpcMessage> {

    /**
     * Type of SSE event sinks.
     */
    GenericType<JsonRpcSink> TYPE = GenericType.create(JsonRpcSink.class);

    /**
     * Emit an event after it has been initialized by a builder consumer.
     *
     * @param consumer a response consumer
     * @throws Exception if an error occurs
     */
    JsonRpcSink emit(Consumer<JsonRpcMessage.Builder> consumer) throws Exception;
}
