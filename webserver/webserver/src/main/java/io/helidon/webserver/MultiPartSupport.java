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

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.Content;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.MultiPartDataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.common.reactive.Flow.Publisher;
import io.helidon.common.reactive.Flow.Subscriber;
import io.helidon.common.reactive.Flow.Subscription;
import java.util.concurrent.locks.Condition;

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

    static class MultiPartImpl implements MultiPart {

        private final ServerRequest request;
        private final ServerResponse response;
        private final BodyPartIterator iterator;

        MultiPartImpl(final ServerRequest request,
                final ServerResponse response,
                final Publisher<DataChunk> publisher) {

            this.request = request;
            this.response = response;
            this.iterator = new BodyPartIterator(publisher, this);
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
        public Iterator<BodyPart> iterator() {
            return iterator;
        }
    }

    /**
     * A lazy iterator of body part based on a reactive stream of {@link DataChunk}.
     * The methods {@link #hasNext() } and {@link #next() } will block when
     * the payload for each body part is being consumed.
     */
    static class BodyPartIterator implements Iterator<BodyPart> {

        private final BodyPartProcessor processor;
        private final MultiPart parent;
        private final Lock lock;
        private final Condition condition;
        private volatile BodyPartImpl currentPart;

        BodyPartIterator(final Publisher<DataChunk> originPublisher,
                final MultiPart parent) {

            this.parent = parent;
            this.processor = new BodyPartProcessor();
            // subscribe the processor to the main stream
            originPublisher.subscribe(processor);
            // subscribe the first potential body part to the processor
            currentPart = new BodyPartImpl(parent, this);
            processor.subscribe(currentPart);
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
        }

        /**
         * Setup a new body part as the current part and subscribe it
         * to the processor.
         */
        void setNextPart(){
            currentPart = new BodyPartImpl(parent, this);
            processor.subscribe(currentPart);
            condition.signal();
        }

        /**
         *  Blocks if a part is being consumed.
         */
        void waitNextPart(){
            try {
                if (currentPart == null) {
                    condition.await();
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public boolean hasNext() {
            waitNextPart();
            return !processor.isComplete();
        }

        @Override
        public BodyPart next() {
            waitNextPart();
            if (processor.isComplete()) {
                return null;
            }
            BodyPart part = currentPart;
            currentPart = null;
            return part;
        }
    }

    /**
     * Implementation of the body part entity.
     * This class is a delegated processor that converts a reactive stream of
     * {@link MultiPartDataChunk} into a stream of {@link DataChunk}.
     */
    static class BodyPartImpl implements BodyPart,
            Subscriber<MultiPartDataChunk>, Publisher<DataChunk> {

        private final MultiPart parent;
        private final Content content;
        private final BodyPartIterator iterator;
        private volatile Subscription subscription = null;
        private volatile Subscriber<? super DataChunk> subscriber = null;
        private volatile BodyPartHeaders headers = null;

        BodyPartImpl(final MultiPart parent, final BodyPartIterator iterator) {
            this.parent = parent;
            this.iterator = iterator;
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
            Objects.requireNonNull(s, "subscription is null");
            if(subscription != null){
                throw new IllegalStateException(
                        "The subscription is already set!");
            }
            subscription = s;
        }

        @Override
        public void onNext(final MultiPartDataChunk item) {
            requireSubscriber();
            if(headers == null){
                headers = item.headers();
            }
            subscriber.onNext(item);
        }

        @Override
        public void onError(final Throwable throwable) {
            requireSubscription();
            subscription.cancel();
        }

        @Override
        public void onComplete() {
            // release the lock to allow next part iteration
            iterator.setNextPart();
        }

        @Override
        public void subscribe(final Subscriber<? super DataChunk> s) {
            requireSubscription();
            if(subscriber != null){
                if(!subscriber.equals(s)){
                    throw new IllegalStateException("subscriber is already set");
                }
            } else {
                subscriber = s;
                subscriber.onSubscribe(subscription);
            }
        }

        private void requireSubscription(){
            if(subscription == null){
                throw new IllegalStateException("The subscription is not set!");
            }
        }

        private void requireSubscriber() {
            if(subscriber == null){
                throw new IllegalStateException("The subscriber is not set");
            }
        }
    }

    /**
     * A delegated processor that converts a reactive stream of {@link DataChunk}
     *  into a stream of {@link MultiPartDataChunk}.
     *
     * The processor releases each data chunk after successfully consumed.
     * It exposes a method {@link #isComplete()} that is used by
     *  {@link BodyPartIterator} to check if there are more parts available.
     */
    static class BodyPartProcessor implements Subscriber<DataChunk>,
            Publisher<MultiPartDataChunk> {

        private volatile boolean complete = false;
        private volatile Subscriber<? super MultiPartDataChunk> subscriber = null;
        private volatile Subscription subscription = null;

        /**
         * Indicate if there are no more chunks available in the stream.
         * @return {@code true} if there are no more chunks available,
         *  {@code false} otherwise.
         */
        boolean isComplete(){
            return complete;
        }

        @Override
        public void onSubscribe(final Flow.Subscription s) {
            if(subscription != null){
                throw new IllegalStateException(
                        "The subscription is already set!");
            }
            subscription = s;
        }

        @Override
        public void onNext(final DataChunk item) {
            requireSubscriber();
            try {
                if (item instanceof MultiPartDataChunk) {
                    MultiPartDataChunk chunk = (MultiPartDataChunk) item;
                    subscriber.onNext(chunk);
                    if (chunk.isLast()) {
                        subscriber.onComplete();
                    }
                } else {
                    onError(new IllegalArgumentException(
                            "Not a multipart chunk!"));
                }
            } catch(Throwable throwable){
                onError(throwable);
            } finally {
                // blame netbeans for the if statement
                if(item != null){
                    item.release();
                }
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            requireSubscriber();
            if (subscriber != null) {
                subscriber.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!complete) {
                complete = true;
            }
        }

        @Override
        public void subscribe(final Subscriber<? super MultiPartDataChunk> s) {
            requireSubscription();
            if(subscriber != null){
                if(!subscriber.equals(s)){
                    throw new IllegalStateException("subscriber is already set");
                }
            } else {
                subscriber = s;
                subscriber.onSubscribe(subscription);
            }
        }

        private void requireSubscription(){
            if(subscription == null){
                throw new IllegalStateException("The subscription is not set!");
            }
        }

        private void requireSubscriber() {
            if(subscriber == null){
                throw new IllegalStateException("The subscriber is not set");
            }
        }
    }
}
