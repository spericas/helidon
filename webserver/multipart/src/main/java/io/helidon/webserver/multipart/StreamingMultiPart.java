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

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MultiPartDataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public final class StreamingMultiPart implements Flow.Publisher<StreamingBodyPart> {

    private final ServerRequest request;
    private final ServerResponse response;
    private final BodyPartProcessor processor;
    private final Flow.Publisher<DataChunk> originPublisher;
    private StreamingBodyPart currentBodyPart;
    private Flow.Subscriber<? super StreamingBodyPart> subscriber;

    /**
     * Create a new multipart entity.
     *
     * @param request the server request
     * @param response the server response
     * @param originPublisher the original publisher (request content)
     */
    StreamingMultiPart(final ServerRequest request,
                           final ServerResponse response,
                           final Flow.Publisher<DataChunk> originPublisher) {
        this.request = request;
        this.response = response;
        this.originPublisher = originPublisher;
        this.processor = new BodyPartProcessor(this);
        originPublisher.subscribe(processor);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super StreamingBodyPart> subscriber) {
        this.subscriber = subscriber;
    }

    public ServerRequest request() {
        return request;
    }

    public ServerResponse response() {
        return response;
    }

    /**
     * @return the body part processor.
     */
    BodyPartProcessor processor() {
        return processor;
    }

    /**
     * Submit the current body for consumption.
     *
     * @return {@code true} if there is a handler registered to consume
     * the body, {@code false} otherwise
     */
    boolean onNewBodyPart() {
        if (subscriber != null && currentBodyPart != null) {
            subscriber.onNext(currentBodyPart);
            return true;
        }
        return false;
    }

    /**
     * Setup the next body part entity and subscribe it to the processor.
     * If the current body part entity is non {@code null}, its subscription
     * will be canceled first and the processor (re)subscribed to the
     * original publisher.
     */
    void setupNextBodyPart() {
        if (currentBodyPart != null) {
            currentBodyPart.cancelSubscription();
            originPublisher.subscribe(processor);
        }
        currentBodyPart = new StreamingBodyPart(this);
        processor.subscribe(currentBodyPart);
    }

    void onError(final Throwable error) {
        subscriber.onError(error);
    }

    void onBodyPartComplete() {
        if (processor.isComplete()) {
            subscriber.onComplete();
        }
    }

    /**
     * A delegated processor that converts a reactive stream of {@link DataChunk}
     * into a stream of {@link MultiPartDataChunk}.
     *
     * The processor exposes a method {@link #isComplete()} that is used to
     * check if there are more parts available.
     */
    static class BodyPartProcessor implements Flow.Subscriber<DataChunk>, Flow.Publisher<MultiPartDataChunk> {

        private final StreamingMultiPart multiPart;
        private boolean complete = false;
        private Flow.Subscriber<? super MultiPartDataChunk> subscriber = null;
        private Flow.Subscription subscription = null;
        private MultiPartDataChunk firstChunk = null;
        private boolean started = false;

        /**
         * Create a new processor.
         *
         * @param multiPart the multiPart entity
         */
        BodyPartProcessor(final StreamingMultiPart multiPart) {
            this.multiPart = multiPart;
        }

        /**
         * Indicate if there are no more chunks available in the stream.
         *
         * @return {@code true} if there are no more chunks available,
         * {@code false} otherwise.
         */
        boolean isComplete() {
            return complete;
        }

        @Override
        public void onSubscribe(final Flow.Subscription s) {
            if (subscription != null) {
                throw new IllegalStateException("Subscription should not null");
            }
            subscription = s;
            // request the first chunk only to start things off
            if (!started) {
                subscription.request(1);
                started = true;
            }
        }

        @Override
        public void onNext(final DataChunk item) {
            if (item instanceof MultiPartDataChunk) {
                MultiPartDataChunk chunk = (MultiPartDataChunk) item;
                if (firstChunk == null) {
                    firstChunk = chunk;
                    multiPart.setupNextBodyPart();
                    multiPart.onNewBodyPart();
                } else {
                    submitChunk(chunk);
                }
            } else {
                onError(new IllegalArgumentException("Not a multipart chunk!"));
            }
        }

        /**
         * Submit the first (cached) chunk.
         *
         * @throws IllegalArgumentException if the first chunk is {@code null}
         */
        void submitFirstChunk() {
            if (firstChunk == null) {
                throw new IllegalStateException("First chunk is null");
            }
            submitChunk(firstChunk);
        }

        /**
         * Submit the given chunk.
         *
         * @param chunk the chunk to submit
         */
        private void submitChunk(MultiPartDataChunk chunk) {
            checkComplete();
            if (subscriber == null) {
                throw new IllegalStateException("Subscriber is null");
            }
            subscriber.onNext(chunk);
            if (chunk.isLast()) {
                Flow.Subscriber s = subscriber;
                // reset for next part
                subscriber = null;
                subscription = null;
                firstChunk = null;
                s.onComplete();
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            if (subscriber == null) {
                throw new IllegalStateException(
                        "Cannot delegate error, subscriber is null", throwable);
            }
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            checkComplete();
            if (!complete) {
                complete = true;
                multiPart.onBodyPartComplete();
            }
        }

        @Override
        public void subscribe(final Flow.Subscriber<? super MultiPartDataChunk> s) {
            if (subscriber != null) {
                throw new IllegalStateException("Current part already subscribed");
            }
            subscriber = s;
            subscriber.onSubscribe(subscription);
        }

        /**
         * Check if this processor is completed.
         *
         * @throws {@link IllegalStateException} if this publisher is already
         * completed
         */
        private void checkComplete() {
            if (complete) {
                throw new IllegalStateException("Already completed");
            }
        }
    }
}

