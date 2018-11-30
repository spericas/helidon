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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

/**
 * An entity that represents a top level multipart payload.
 */
public final class MultiPart {

    final List<BodyPart> bodyParts;
    final StreamingMultiPart streamingMultiPart;
    final CompletableFuture<MultiPart> future;

    MultiPart(final ServerRequest request,
                  final ServerResponse response,
                  final Flow.Publisher<DataChunk> originPublisher,
                  final CompletableFuture<MultiPart> future) {
        this.future = future;
        bodyParts = new ArrayList<>();
        streamingMultiPart = new StreamingMultiPart(request, response, originPublisher);
        streamingMultiPart.subscribe(new Flow.Subscriber<StreamingBodyPart>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            /**
             * Collect all chunks for a part and create a buffered part that
             * holds a single {@link DataChunk} with all its content.
             *
             * @param bodyPart The body part received.
             */
            @Override
            public void onNext(StreamingBodyPart bodyPart) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bodyPart.content().subscribe(new Flow.Subscriber<DataChunk>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(DataChunk item) {
                        final byte[] data = item.bytes();
                        try {
                            baos.write(data);
                        } catch (IOException e) {
                            future.completeExceptionally(e);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }

                    @Override
                    public void onComplete() {
                        DataChunk partData = DataChunk.create(baos.toByteArray());
                        bodyParts.add(new BodyPart(request, bodyPart.headers(), partData, null));
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(MultiPart.this);
            }
        });
    }

    public Collection<BodyPart> bodyParts() {
        return bodyParts;
    }
}
