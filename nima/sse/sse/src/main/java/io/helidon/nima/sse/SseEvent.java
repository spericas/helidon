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

package io.helidon.nima.sse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import io.helidon.common.GenericType;
import io.helidon.common.http.Http;
import io.helidon.common.http.WritableHeaders;
import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.nima.http.media.EntityReader;
import io.helidon.nima.http.media.MediaContext;

/**
 * An SSE event.
 */
public class SseEvent {

    private static final WritableHeaders<?> EMPTY_HEADERS = WritableHeaders.create();

    private final String id;
    private final String name;
    private final Object data;
    private final String comment;
    private final MediaType mediaType;
    private final Duration reconnectMillis;
    private final MediaContext mediaContext;

    private SseEvent(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.data = builder.data;
        this.comment = builder.comment;
        this.mediaType = builder.mediaType;
        this.reconnectMillis = builder.reconnectMillis;
        this.mediaContext = builder.mediaContext;
    }

    /**
     * Creates a new SSE event with data.
     *
     * @param data data for the event
     * @return newly created SSE event
     */
    public static SseEvent create(Object data) {
        return builder().data(data).build();
    }

    /**
     * Creates a new SSE event with data and media type.
     *
     * @param data data for the event
     * @param mediaType media type for this event if not text/plain
     * @return newly created SSE event
     */
    public static SseEvent create(Object data, MediaType mediaType) {
        return builder().data(data).mediaType(mediaType).build();
    }

    /**
     * Creates builder for this class.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get data for this event.
     *
     * @return the data
     */
    public Object data() {
        return data;
    }

    /**
     * Get data for this event as type T. Uses Nima's media support to convert
     * event data to type T.
     *
     * @param clazz the class
     * @param mediaType media type of event data
     * @param <T> the converted type
     * @return the converted data
     */
    @SuppressWarnings("unchecked")
    public <T> T data(Class<T> clazz, MediaType mediaType) {
        if (!(data instanceof String sdata)) {
            throw new IllegalStateException("Cannot convert non-string event data");
        }

        Objects.requireNonNull(clazz);
        Objects.requireNonNull(mediaType);

        if (clazz.equals(String.class)) {
            return (T) sdata;
        }
        if (clazz.equals(byte[].class)) {
            return (T) sdata.getBytes(StandardCharsets.UTF_8);
        }
        try {
            if (mediaContext == null) {
                throw new IllegalStateException("Media context has not been set on this event");
            }
            GenericType<T> type = GenericType.create(clazz);
            WritableHeaders<?> headers;
            if (!mediaType.equals(MediaTypes.WILDCARD)) {
                headers = WritableHeaders.create();
                headers.set(Http.Header.CONTENT_TYPE, mediaType.text());
            } else {
                headers = EMPTY_HEADERS;
            }
            EntityReader<T> reader = mediaContext.reader(GenericType.create(clazz), headers);
            try (InputStream is = new ByteArrayInputStream(sdata.getBytes(StandardCharsets.UTF_8))) {
                return reader.read(type, is, headers);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Get data for this event as type T. Uses Nima's media support to convert
     * event data to type T.
     *
     * @param clazz the class
     * @param <T> the converted type
     * @return the converted data
     */
    @SuppressWarnings("unchecked")
    public <T> T data(Class<T> clazz) {
        return data(clazz, MediaTypes.WILDCARD);
    }

    /**
     * Get media type for this event.
     *
     * @return the media type
     */
    public MediaType mediaType() {
        return mediaType;
    }

    /**
     * Get optional ID for this event.
     *
     * @return optional ID
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Get optional name for this event.
     *
     * @return optional name
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Get optional comment for this event.
     *
     * @return optional comment
     */
    public Optional<String> comment() {
        return Optional.ofNullable(comment);
    }

    /**
     * Get optional reconnect for this event.
     *
     * @return optional reconnect
     */
    public Optional<Duration> reconnectMillis() {
        return Optional.of(reconnectMillis);
    }

    /**
     * Fluent API builder for {@link SseEvent}.
     */
    public static class Builder implements io.helidon.common.Builder<Builder, SseEvent> {

        private String id;
        private String name;
        private Object data;
        private String comment;
        private MediaType mediaType = MediaTypes.TEXT_PLAIN;
        private Duration reconnectMillis;
        private MediaContext mediaContext;

        private Builder() {
        }

        @Override
        public SseEvent build() {
            if (data == null) {
                throw new IllegalArgumentException("Event must include some data");
            }
            return new SseEvent(this);
        }

        /**
         * Set the event id. Optional.
         *
         * @param id the id
         * @return updated builder instance
         */
        public Builder id(String id) {
            Objects.requireNonNull(id);
            this.id = id;
            return this;
        }

        /**
         * Set event name. Optional.
         *
         * @param name event name
         * @return updated builder instance
         */
        public Builder name(String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        /**
         * Set comment string associated with the event. Optional. The comment will be
         * serialized with the event, before event data is serialized.
         *
         * @param comment comment string
         * @return updated builder instance
         */
        public Builder comment(String comment) {
            Objects.requireNonNull(comment);
            this.comment = comment;
            return this;
        }

        /**
         * Set the media type for the event data. Default value is {@link MediaTypes#TEXT_PLAIN}.
         *
         * @param mediaType media type of event data
         * @return updated builder instance
         */
        public Builder mediaType(MediaType mediaType) {
            Objects.requireNonNull(mediaType);
            this.mediaType = mediaType;
            return this;
        }

        /**
         * Set event data.
         *
         * @param data event data
         * @return updated builder instance
         */
        public Builder data(Object data) {
            Objects.requireNonNull(data);
            this.data = data;
            return this;
        }

        /**
         * Set reconnection delay that indicates how long the event receiver should
         * wait before attempting to reconnect in case a connection is lost. Optional.
         *
         * @param reconnectMillis reconnection delay
         * @return updated builder instance
         */
        public Builder reconnectDelay(Duration reconnectMillis) {
            Objects.requireNonNull(reconnectMillis);
            this.reconnectMillis = reconnectMillis;
            return this;
        }

        /**
         * Set the media context for this event. This is only required when using
         * media providers.
         *
         * @param mediaContext the media context
         * @return updated builder instance
         * @see #data(Class)
         */
        public Builder mediaContext(MediaContext mediaContext) {
            Objects.requireNonNull(mediaContext);
            this.mediaContext = mediaContext;
            return this;
        }
    }
}
