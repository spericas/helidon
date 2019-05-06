/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.media.jsonp.server;

import java.nio.charset.StandardCharsets;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.BaseStreamWriter;
import io.helidon.webserver.ServerResponse;

/**
 * Class JsonArrayStreamWriter.
 */
public class JsonArrayStreamWriter<T> extends BaseStreamWriter<T> {

    // TODO charsets
    private static final DataChunk BEGIN_CHUNK = DataChunk.create("[".getBytes(StandardCharsets.UTF_8));
    private static final DataChunk SEPARATOR_CHUNK = DataChunk.create(",".getBytes(StandardCharsets.UTF_8));
    private static final DataChunk END_CHUNK = DataChunk.create("]".getBytes(StandardCharsets.UTF_8));

    public JsonArrayStreamWriter(ServerResponse response, Class<T> type) {
        super(response, type, BEGIN_CHUNK, SEPARATOR_CHUNK, END_CHUNK);
    }

    @Override
    public Flow.Publisher<DataChunk> apply(Flow.Publisher<T> publisher) {
        return new JsonArrayStreamPublisher(publisher);
    }

    class JsonArrayStreamPublisher implements Flow.Publisher<DataChunk>, Flow.Subscriber<T> {

        private boolean first = true;
        private Flow.Subscriber<? super DataChunk> subscriber;
        private final Flow.Publisher<T> publisher;

        JsonArrayStreamPublisher(Flow.Publisher<T> publisher) {
            this.publisher = publisher;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
            this.subscriber = subscriber;
            this.subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    publisher.subscribe(JsonArrayStreamPublisher.this);
                }

                @Override
                public void cancel() {
                }
            });

        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (subscriber != null) {
                subscriber.onNext(getBeginChunk());
            }
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T item) {
            if (!first) {
                subscriber.onNext(getSeparatorChunk());
            } else {
                first = false;
            }

            Flow.Publisher<DataChunk> itemPublisher = findPublisher(item);
            itemPublisher.subscribe(new Flow.Subscriber<DataChunk>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(DataChunk item) {
                    subscriber.onNext(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                }

                @Override
                public void onComplete() {
                }
            });
        }

        @Override
        public void onError(Throwable throwable) {
            if (subscriber != null) {
                subscriber.onNext(getEndChunk());
            }
        }

        @Override
        public void onComplete() {
            if (subscriber != null) {
                subscriber.onNext(getEndChunk());
                subscriber.onComplete();
            }
        }

        private Flow.Publisher<DataChunk> findPublisher(T item) {
            Flow.Publisher<DataChunk> itemPublisher = getResponse().createPublisherUsingWriter(item);
            if (itemPublisher == null) {
                throw new RuntimeException("Unable to find publisher for item " + item);
            }
            return itemPublisher;
        }
    }
}
