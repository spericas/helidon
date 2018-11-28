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
 * StreamingMultiPart support {@link Service} and {@link Handler}.
 */
public class MultiPartSupport implements Service, Handler {

    @Override
    public void update(final Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(final ServerRequest req, final ServerResponse res) {
        req.headers().contentType().ifPresent((contentType) -> {
            if (contentType.type().equals(MediaType.MULTIPART_FORM_DATA.type())
                    && contentType.subtype().equals(MediaType.MULTIPART_FORM_DATA.subtype())) {
                req.content().registerReader(StreamingMultiPart.class, (publisher, clazz) -> {
                    CompletableFuture<StreamingMultiPart> future = new CompletableFuture();
                    future.complete(new MultiPartImpl(req, res, publisher));
                    return future;
                });
            }
        });
        req.next();
    }

    /**
     * Implementation of {@link StreamingMultiPart}.
     */
    static final class MultiPartImpl implements StreamingMultiPart {

        private final ServerRequest request;
        private final ServerResponse response;
        private final CompletableFuture completeFuture;
        private final BodyPartProcessor processor;
        private final Publisher<DataChunk> originPublisher;
        private Consumer<BodyPart> bodyPartConsumer = null;
        private BodyPartImpl currentBodyPart = null;

        /**
         * Create a new multipart entity.
         * @param request the server request
         * @param response the server response
         * @param originPublisher the original publisher (request content)
         */
        MultiPartImpl(final ServerRequest request,
                final ServerResponse response,
                final Publisher<DataChunk> originPublisher) {

            this.request = request;
            this.response = response;
            this.completeFuture = new CompletableFuture();
            this.originPublisher = originPublisher;
            this.processor = new BodyPartProcessor(this);
            originPublisher.subscribe(processor);
        }

        public ServerRequest request() {
            return request;
        }

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
        public StreamingMultiPart onBodyPart(final Consumer<BodyPart> handler) {
            if(this.bodyPartConsumer != null){
                throw new IllegalStateException("Handler already registered");
            }
            if(handler == null){
                throw new IllegalArgumentException("Handler is null");
            }
            this.bodyPartConsumer = handler;
            return this;
        }

        @Override
        public CompletionStage onComplete() {
            return completeFuture;
        }

        /**
         * @return the original (request content) publisher
         */
        Publisher<DataChunk> originPublisher(){
            return originPublisher;
        }

        /**
         * @return the body part processor.
         */
        BodyPartProcessor processor() {
            return processor;
        }

        /**
         * Submit the current body for consumption.
         * @return {@code true} if there is a handler registered to consume
         *  the body, {@code false} otherwise
         */
        boolean onNewBodyPart(){
            if(bodyPartConsumer != null && currentBodyPart != null){
                bodyPartConsumer.accept(currentBodyPart);
                return true;
            }
            return false;
        }

        /**
         * Setup the next body part entity and subscribe it to the processor.
         * If the current body part entity is non {@code null}, its subscription
         * will be canceled first and the processor (re)subscribed to the
         *  original publisher.
         */
        void setupNextBodyPart(){
            if(currentBodyPart != null){
                currentBodyPart.cancelSubscription();
                originPublisher.subscribe(processor);
            }
            currentBodyPart = new BodyPartImpl(this);
            processor.subscribe(currentBodyPart);
        }

        /**
         * Propagate the given error in the complete future.
         * @param error the error to propagate
         */
        void onError(final Throwable error){
            completeFuture.completeExceptionally(error);
        }

        /**
         * Complete the future if the processor is complete.
         */
        void onBodyPartComplete(){
            if(processor.isComplete()){
                completeFuture.complete(null);
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
        private BodyPartSubscription subscription = null;
        private Subscriber<? super DataChunk> subscriber = null;
        private BodyPartHeaders headers = null;
        private boolean complete = false;

        BodyPartImpl(final MultiPartImpl parent) {
            if(parent == null){
                throw new IllegalArgumentException("Parent cannot be null");
            }
            this.parent = parent;
            this.content = new Request.Content(
                    (Request)parent.request(), this);
        }

        public ServerRequest request() {
            return parent.request();
        }

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
            if(subscription != null){
                throw new IllegalStateException("Subscription is not null");
            }
            subscription = new BodyPartSubscription(s, parent.processor());
        }

        @Override
        public void onNext(final MultiPartDataChunk item) {
            checkComplete();
            checkSubscriber();
            checkSubscription();
            if(headers == null){
                headers = item.headers();
            }
            subscriber.onNext(item);
            subscription.onDelivered();
        }

        @Override
        public void onError(final Throwable throwable) {
            checkComplete();
            if(subscriber == null){
                throw new IllegalStateException("Subscriber is null", throwable);
            }
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            checkComplete();
            checkSubscriber();
            checkSubscription();
            complete = true;
            subscriber.onComplete();
            subscriber = null;
            parent.onBodyPartComplete();
            if(!parent.processor().isComplete()
                    && !subscription.hasUndelivered()){
                // if the current subscription has not requested enough to
                // receive the chunk for the next potential body part
                // request 1 more to make sure we can receive it
                subscription.request(1);
            }
        }

        @Override
        public void subscribe(final Subscriber<? super DataChunk> s) {
            if(subscriber != null){
                throw new IllegalStateException("Already subscribed");
            }
            checkComplete();
            checkSubscription();
            subscriber = s;
            subscriber.onSubscribe(subscription);
        }

        /**
         * Cancel the subscription.
         * @throws IllegalStateException if the subscription is {@code null}
         */
        void cancelSubscription() {
            checkSubscription();
            subscription.cancel();
        }

        /**
         * Check if this publisher is completed.
         * @throws {@link IllegalStateException} if this publisher is already
         *  completed
         */
        private void checkComplete(){
            if(complete){
                throw new IllegalStateException("Already completed");
            }
        }

        /**
         * Check if this publisher has a subscriber.
         * @throws {@link IllegalStateException} if this publisher does not
         *  have a subscriber
         */
        private void checkSubscriber(){
            if(subscriber == null){
                throw new IllegalStateException("Subscriber is null");
            }
        }

        /**
         * Check if this publisher has a subscription.
         * @throws {@link IllegalStateException} if this publisher does not
         *  have a subscription
         */
        private void checkSubscription(){
            if(subscription == null){
                throw new  IllegalArgumentException("Subscription is null");
            }
        }
    }

    /**
     * A delegated subscription used to send the (cached) first chunk when
     * the content subscriber has requested data.
     * It keeps the count of requested and delivered items in order to indicate
     *  if there are more items to be delivered, see {@link #hasUndelivered()},
     *  {@link #onDelivered().
     */
    static class BodyPartSubscription implements Subscription {

        private final Subscription delegate;
        private final BodyPartProcessor processor;
        private long requested = 1;
        private long delivered = 0;
        private boolean canceled = false;

        /**
         * Create a new body part subscription.
         * @param delegate the subscription to delegate
         * @param processor the body part processor
         */
        BodyPartSubscription(final Subscription delegate,
                final BodyPartProcessor processor) {
            this.delegate = delegate;
            this.processor = processor;
        }

        /**
         * Check if this subscription expects more items.
         * @return {@code true} if this subscription expects more items to be
         *  delivered, {@code false} otherwise
         */
        boolean hasUndelivered(){
            return requested == Long.MAX_VALUE || requested > delivered;
        }

        /**
         * Increase the count of delivered items of this subscription.
         */
        void onDelivered(){
            delivered++;
        }

        @Override
        public void request(long n) {
            checkCanceled();
            long reqCount = n;
            if(n > 0){
                if(delivered == 0){
                    reqCount--;
                    requested += (reqCount);
                    processor.submitFirstChunk();
                } else {
                    requested += reqCount;
                }
                if(requested > delivered){
                    delegate.request(reqCount);
                }
            }
        }

        @Override
        public void cancel() {
            checkCanceled();
            delegate.cancel();
            canceled = true;
        }

        /**
         * Check if this subscription is canceled.
         * @throws {@link IllegalStateException} if this is subscription is
         *  canceled
         */
        private void checkCanceled(){
            if(canceled){
                throw new IllegalArgumentException("Subscription has been canceled");
            }
        }
    }

    /**
     * A delegated processor that converts a reactive stream of {@link DataChunk}
     *  into a stream of {@link MultiPartDataChunk}.
     *
     * The processor exposes a method {@link #isComplete()} that is used to
     * check if there are more parts available.
     */
    static class BodyPartProcessor implements Subscriber<DataChunk>,
            Publisher<MultiPartDataChunk> {

        private final MultiPartImpl multiPart;
        private boolean complete = false;
        private Subscriber<? super MultiPartDataChunk> subscriber = null;
        private Subscription subscription = null;
        private MultiPartDataChunk firstChunk = null;
        private boolean started = false;

        /**
         * Create a new processor.
         * @param multiPart the multiPart entity
         */
        BodyPartProcessor(final MultiPartImpl multiPart) {
            this.multiPart = multiPart;
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
            if(subscription != null){
                throw new IllegalStateException("Subscription should not null");
            }
            subscription = s;
            // request the first chunk only to start things off
            if(!started){
                subscription.request(1);
                started = true;
            }
        }

        @Override
        public void onNext(final DataChunk item) {
            if (item instanceof MultiPartDataChunk) {
                MultiPartDataChunk chunk = (MultiPartDataChunk) item;
                if(firstChunk == null){
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
         * @throws IllegalArgumentException if the first chunk is {@code null}
         */
        void submitFirstChunk(){
            if(firstChunk == null){
                throw new IllegalStateException("First chunk is null");
            }
            submitChunk(firstChunk);
        }

        /**
         * Submit the given chunk.
         * @param chunk the chunk to submit
         */
        private void submitChunk(MultiPartDataChunk chunk) {
            checkComplete();
            if(subscriber == null){
                throw new IllegalStateException("Subscriber is null");
            }
            subscriber.onNext(chunk);
            if (chunk.isLast()) {
                Subscriber s = subscriber;
                // reset for next part
                subscriber = null;
                subscription = null;
                firstChunk = null;
                s.onComplete();
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            if(subscriber == null){
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
        public void subscribe(final Subscriber<? super MultiPartDataChunk> s) {
            if (subscriber != null) {
                throw new IllegalStateException("Current part already subscribed");
            }
            subscriber = s;
            subscriber.onSubscribe(subscription);
        }

        /**
         * Check if this processor is completed.
         * @throws {@link IllegalStateException} if this publisher is already
         *  completed
         */
        private void checkComplete(){
            if(complete){
                throw new IllegalStateException("Already completed");
            }
        }
    }
}
