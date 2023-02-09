/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.nima.sse.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BiConsumer;

import io.helidon.common.GenericType;
import io.helidon.common.http.Http;
import io.helidon.common.http.HttpMediaType;
import io.helidon.common.media.type.MediaType;
import io.helidon.nima.webserver.http.ServerResponse;
import io.helidon.nima.webserver.http.spi.Sink;

import static io.helidon.common.http.Http.HeaderValues.CONTENT_TYPE_EVENT_STREAM;

/**
 * Implementation of an SSE sink. Emits {@link SseEvent}s.
 */
public class SseSink implements Sink<SseEvent> {
    private static final byte[] SSE_DATA = "data:".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SSE_SEPARATOR = "\n\n".getBytes(StandardCharsets.UTF_8);

    /**
     * Type of SSE event sinks.
     */
    public static final GenericType<SseSink> TYPE = GenericType.create(SseSink.class);

    private final ServerResponse serverResponse;
    private final BiConsumer<Object, MediaType> eventConsumer;
    private final Runnable closeRunnable;
    private OutputStream outputStream;

    SseSink(ServerResponse serverResponse, BiConsumer<Object, MediaType> eventConsumer, Runnable closeRunnable) {
        // Verify response has no status or content type
        HttpMediaType ct = serverResponse.headers().contentType().orElse(null);
        if (serverResponse.status().code() != Http.Status.OK_200.code()
                || ct != null && !CONTENT_TYPE_EVENT_STREAM.values().equals(ct.mediaType().text())) {
            throw new IllegalStateException("ServerResponse instance cannot be used to create SseResponse");
        }

        // Ensure content type set for SSE
        if (ct == null) {
            serverResponse.headers().add(CONTENT_TYPE_EVENT_STREAM);
        }

        this.serverResponse = serverResponse;
        this.eventConsumer = eventConsumer;
        this.closeRunnable = closeRunnable;
    }

    @Override
    public SseSink emit(SseEvent sseEvent) {
        if (outputStream == null) {
            outputStream = serverResponse.outputStream();
            Objects.requireNonNull(outputStream);
        }
        try {
            outputStream.write(SSE_DATA);
            eventConsumer.accept(sseEvent.data(), sseEvent.mediaType());
            outputStream.write(SSE_SEPARATOR);
            outputStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    @Override
    public void close() {
        closeRunnable.run();
    }
}