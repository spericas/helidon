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
import java.util.concurrent.CompletableFuture;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * MultiPartSupport support {@link Service} and {@link Handler}.
 */
public class MultiPartSupport implements Service, Handler {

    @Override
    public void update(final Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(final ServerRequest req, final ServerResponse res) {
        req.headers().contentType().ifPresent(contentType -> {
            if (contentType.type().equals(MediaType.MULTIPART_FORM_DATA.type())
                    && contentType.subtype().equals(MediaType.MULTIPART_FORM_DATA.subtype())) {
                req.content().registerReader(MultiPart.class, (publisher, clazz) -> {
                    CompletableFuture<MultiPart> future = new CompletableFuture<>();
                    future.complete(new MultiPart(req, res, publisher));
                    return future;
                });
            }
        });
        req.next();
    }

    public static Flow.Publisher<DataChunk> multipart(Flow.Publisher<BodyPart> publisher) {
        Flow.Processor<BodyPart, DataChunk> processor = new Flow.Processor<BodyPart, DataChunk>() {

            private Flow.Subscription subscription;
            private DataChunk dataChunk;
            private boolean completed = false;

            // -- Publisher ---------------------------------------------------

            @Override
            public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        if (completed) {
                            subscriber.onComplete();
                        }
                        while (n-- > 0) {
                            subscription.request(1);
                            subscriber.onNext(dataChunk);
                        }
                    }

                    @Override
                    public void cancel() {
                        subscription.cancel();
                    }
                });
            }

            // -- Subscriber ---------------------------------------------------

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
            }

            @Override
            public void onNext(BodyPart bodyPart) {
                // convert bodyPart to dataChunk

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {
                completed = true;
            }
        };

        publisher.subscribe(processor);

        return processor;
    }
}
