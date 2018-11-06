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

package io.helidon.webserver;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.Content;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MultiPartDataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;

/**
 *
 * @author rgrecour
 */
public class MultiPartSupport implements Service, Handler {

    @Override
    public void update(final Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(final ServerRequest req, final ServerResponse res) {
        req.headers().contentType().ifPresent((contentType) -> {
            if (contentType.getType().equals(MediaType.MULTIPART_FORM_DATA.getType())
                    && contentType.getSubtype().equals(MediaType.MULTIPART_FORM_DATA.getSubtype())) {
                req.content().registerReader(MultiPart.class, (publisher, clazz) -> {
                    CompletableFuture<MultiPart> future = new CompletableFuture();
                    future.complete(new MultiPartImpl(req, res, publisher));
                    return future;
                });
            }
        });
        req.next();
    }

    /**
     * Implementation of {@link MultiPart}.
     */
    static class MultiPartImpl implements MultiPart {

        private final ServerRequest request;
        private final ServerResponse response;
        private final CompletableFuture completeFuture;
        private final BodyPartProcessor processor;
        private Consumer<BodyPart> bodyPartConsumer;
        private BodyPartImpl currentBodyPart;

        MultiPartImpl(final ServerRequest request,
                final ServerResponse response,
                final Publisher<DataChunk> originPublisher) {

            this.request = request;
            this.response = response;
            this.completeFuture = new CompletableFuture();
            this.processor = new BodyPartProcessor(this);
            originPublisher.subscribe(processor);
            this.currentBodyPart = new BodyPartImpl(this);
            processor.subscribe(currentBodyPart);
            this.bodyPartConsumer = null;
        }

        @Override
        public ServerRequest request() {
            return request;
        }

        @Override
        public ServerResponse response() {
            return response;
        }

        @Override
        public BodyPartHeaders headers() {
            throw new IllegalStateException(
                    "cannot invoke headers() on top level entity");
        }

        @Override
        public Content content() {
            throw new IllegalStateException(
                    "cannot invoke content() on top level entity");
        }

        @Override
        public BodyPart parent() {
            return null;
        }

        @Override
        public MultiPart onBodyPart(final Consumer<BodyPart> handler) {
            Objects.requireNonNull(handler, "handler is null");
            this.bodyPartConsumer = handler;
            return this;
        }

        @Override
        public CompletionStage onComplete() {
            return completeFuture;
        }

        /**
         * Submit the current body for consumption.
         * @return {@code true} if there is a handler registered to consume
         *  the body, {@code false} otherwise
         */
        boolean fireBodyPartEvent(){
            if(bodyPartConsumer != null && currentBodyPart != null){
                bodyPartConsumer.accept(currentBodyPart);
                return true;
            }
            return false;
        }

        /**
         * Propagate the given error in the complete future.
         * @param error the error to propagate
         */
        void onError(final Throwable error){
            completeFuture.completeExceptionally(error);
        }

        /**
         * Setup the next body part if there are more body parts to process
         *  or complete the future.
         */
        void onBodyPartComplete(){
            if(processor.isComplete()){
                completeFuture.complete(null);
            } else {
                currentBodyPart = new BodyPartImpl(this);
                processor.subscribe(currentBodyPart);
            }
        }
    }

    /**
     * Implementation of the body part entity.
     * This class is a delegated processor that converts a reactive stream of
     * {@link MultiPartDataChunk} into a stream of {@link DataChunk}.
     */
    static class BodyPartImpl implements BodyPart,
            Subscriber<MultiPartDataChunk>, Publisher<DataChunk> {

        private final MultiPartImpl parent;
        private final Content content;
        private Subscription subscription = null;
        private Subscriber<? super DataChunk> subscriber = null;
        private BodyPartHeaders headers = null;

        BodyPartImpl(final MultiPartImpl parent) {
            Objects.requireNonNull(parent, "parent is null");
            this.parent = parent;
            this.content = new Request.Content(
                    (Request)parent.request(), this);
        }

        @Override
        public ServerRequest request() {
            return parent.request();
        }

        @Override
        public ServerResponse response() {
            return parent.response();
        }

        @Override
        public BodyPartHeaders headers() {
            if(headers == null){
                throw new IllegalStateException(
                        "needs at least one processed chunk");
            }
            return headers;
        }

        @Override
        public Content content() {
            return content;
        }

        @Override
        public BodyPart parent() {
            return parent;
        }

        @Override
        public void onSubscribe(final Subscription s) {
            if(subscription == null){
                subscription = new BodyPartSubscription(s, parent.processor);
            }
        }

        @Override
        public void onNext(final MultiPartDataChunk item) {
            if(headers == null){
                headers = item.headers();
            }
            if(subscriber != null){
                subscriber.onNext(item);
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            if(subscriber != null){
                subscriber.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            parent.onBodyPartComplete();
            if(subscriber != null){
                subscriber.onComplete();
            }
        }

        @Override
        public void subscribe(final Subscriber<? super DataChunk> s) {
            if(subscriber == null){
                subscriber = s;
                subscriber.onSubscribe(subscription);
            }
        }
    }

    /**
     * A delegated subscription used to send the cached first chunk when
     * the content subscriber has requested data.
     */
    static class BodyPartSubscription implements Subscription {

        private final Subscription delegate;
        private final BodyPartProcessor processor;

        BodyPartSubscription(final Subscription delegate,
                final BodyPartProcessor processor) {

            this.delegate = delegate;
            this.processor = processor;
        }

        @Override
        public void request(long n) {
            if(n > 0){
                processor.submitFirstChunk();
                delegate.request(n);
            }
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }
    }

    /**
     * A delegated processor that converts a reactive stream of {@link DataChunk}
     *  into a stream of {@link MultiPartDataChunk}.
     *
     * The processor exposes a method {@link #isComplete()} that is used by
     *  {@link BodyPartIterator} to check if there are more parts available.
     */
    static class BodyPartProcessor implements Subscriber<DataChunk>,
            Publisher<MultiPartDataChunk> {

        private boolean complete = false;
        private Subscriber<? super MultiPartDataChunk> subscriber = null;
        private Subscription subscription = null;
        private final MultiPartImpl parent;
        private MultiPartDataChunk firstChunk = null;

        /**
         * Create a new processor.
         * @param parent the parent multipart entity
         */
        BodyPartProcessor(final MultiPartImpl parent) {
            Objects.requireNonNull(parent, "parent is null");
            this.parent = parent;
        }

        /**
         * Indicate if there are no more chunks available in the stream.
         * @return {@code true} if there are no more chunks available,
         *  {@code false} otherwise.
         */
        boolean isComplete() {
            return complete;
        }

        @Override
        public void onSubscribe(final Flow.Subscription s) {
            if(subscription == null){
                subscription = s;
                // get the first item
                subscription.request(1);
            }
        }

        @Override
        public void onNext(final DataChunk item) {
            if (item instanceof MultiPartDataChunk) {
                MultiPartDataChunk chunk = (MultiPartDataChunk) item;
                if(firstChunk == null){
                    firstChunk = chunk;
                    parent.fireBodyPartEvent();
                } else {
                    submitChunk(chunk);
                }
            } else {
                onError(new IllegalArgumentException("Not a multipart chunk!"));
            }
        }

        void submitFirstChunk(){
            if(firstChunk == null){
                throw new IllegalStateException("First chunk is null");
            }
            if(subscriber == null){
                throw new IllegalStateException("Subscriber is null");
            }
            submitChunk(firstChunk);
        }

        private void submitChunk(MultiPartDataChunk chunk) {
            if (subscriber != null) {
                subscriber.onNext(chunk);
                if (chunk.isLast()) {
                    Subscriber s = subscriber;
                    subscriber = null;
                    firstChunk = null;
                    s.onComplete();
                }
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            if (subscriber != null) {
                subscriber.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!complete) {
                complete = true;
                parent.onBodyPartComplete();
            }
        }

        @Override
        public void subscribe(final Subscriber<? super MultiPartDataChunk> s) {
            if (firstChunk != null) {
                throw new IllegalStateException("current part already subscribed");
            }
            subscriber = s;
            subscriber.onSubscribe(subscription);
        }
    }
}
