/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.webserver.multipart;

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.Content;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.Request;
import io.helidon.webserver.ServerRequest;

/**
 * An entity representing a nested body part.
 */
public final class BodyPart {

    private final ServerRequest request;
    private final BodyPartHeaders headers;
    private DataChunk data;
    private final Object entity;

    BodyPart(ServerRequest request, BodyPartHeaders headers, DataChunk data, Object entity) {
        this.request = request;
        this.headers = headers;
        this.data = data;
        this.entity = entity;
    }

    /**
     * Get the body part headers.
     *
     * @return The body part headers.
     */
    public BodyPartHeaders headers() {
        return headers;
    }

    /**
     * Get the entity of this part.
     *
     * @return The entity or {@code null} if not set.
     */
    public Object entity() {
        return entity;
    }

    /**
     * Returns content that includes a {@link Flow.Publisher} producing a single
     * {@link DataChunk} for this cached part.
     *
     * @return The content for this part or {@code null} if not set.
     */
    public Content content() {
        return data == null ? null
                : new Request.Content((Request) request,
                (Flow.Subscriber<? super DataChunk> subscriber) -> {
                    subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            if (n > 0 && data != null) {
                                subscriber.onNext(data);
                                subscriber.onComplete();
                                data.release();
                                data = null;
                            }
                        }

                        @Override
                        public void cancel() {
                            throw new UnsupportedOperationException("Subscription cannot be cancelled");
                        }
                    });
                });
    }

    public static <T> BodyPart create(T entity) {
        return builder().entity(entity).build();
    }

    public static <T> BodyPart create(BodyPartHeaders headers, T entity) {
        return builder().headers(headers).entity(entity).build();
    }

    public static Builder builder() {
        return new BodyPart.Builder();
    }

    static final class Builder<T> implements io.helidon.common.Builder<BodyPart> {

        private BodyPartHeaders headers;
        private DataChunk dataChunk;
        private T entity;

        public Builder<T> entity(T entity) {
            this.entity = entity;
            return this;
        }

        public Builder<T> dataChunk(DataChunk dataChunk) {
            this.dataChunk = dataChunk;
            return this;
        }

        public Builder<T> headers(BodyPartHeaders headers) {
            this.headers = headers;
            return this;
        }

        public BodyPart build() {
            return new BodyPart(null, headers, dataChunk, entity);
        }
    }
}
