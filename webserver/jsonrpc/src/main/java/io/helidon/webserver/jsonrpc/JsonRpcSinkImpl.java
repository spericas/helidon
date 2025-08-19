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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.sse.SseEvent;
import io.helidon.jsonrpc.core.JsonRpcMessage;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonValue;

class JsonRpcSinkImpl implements JsonRpcSink {

    private final JsonValue rpcId;
    private final SseSink sseSink;

    JsonRpcSinkImpl(ServerResponse res, JsonValue rpcId) {
        this.rpcId = rpcId;
        this.sseSink = res.sink(SseSink.TYPE);
    }

    @Override
    public JsonRpcSink emit(JsonRpcMessage message) {
        emitMessage(message);
        return this;
    }

    @Override
    public JsonRpcSink emit(Consumer<JsonRpcMessage.Builder> consumer) {
        JsonRpcMessage.Builder messageBuilder = JsonRpcMessage.builder();
        consumer.accept(messageBuilder);
        emitMessage(messageBuilder.build());
        return this;
    }

    private void emitMessage(JsonRpcMessage message) {
        SseEvent.Builder sseBuilder = SseEvent.builder().mediaType(MediaTypes.APPLICATION_JSON);

        switch (message.type()) {
        case RESPONSE:
            var res = new JsonRpcResponseImpl(rpcId, message);
            sseBuilder.data(res.asJsonObject());
            break;
        case NOTIFICATION:
            var not = new JsonRpcNotificationImpl(message);
            sseBuilder.data(not.asJsonObject());
            break;
        case REQUEST:
            var req = new JsonRpcRequestImpl(message);
            sseBuilder.data(req.asJsonObject());
            break;
        default:
            throw new IllegalStateException("Unsupported type " + message.type());
        }

        sseSink.emit(sseBuilder.build());
    }

    @Override
    public void close() {
        sseSink.close();
    }
}
