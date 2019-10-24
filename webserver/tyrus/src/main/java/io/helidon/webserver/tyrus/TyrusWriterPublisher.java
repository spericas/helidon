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

package io.helidon.webserver.tyrus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;

import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Writer;

/**
 * Class TyrusWriterProducer.
 */
public class TyrusWriterPublisher extends Writer implements Flow.Publisher<DataChunk> {

    private Flow.Subscriber<? super DataChunk> subscriber;

    private final Queue<QueuedBuffer> queue = new LinkedList<>();

    private final AtomicLong requested = new AtomicLong(0L);

    private static class QueuedBuffer {
        private final CompletionHandler<ByteBuffer> completionHandler;
        private final ByteBuffer dataFrame;

        QueuedBuffer(ByteBuffer dataFrame, CompletionHandler<ByteBuffer> completionHandler) {
            this.dataFrame = dataFrame;
            this.completionHandler = completionHandler;
        }

        CompletionHandler<ByteBuffer> completionHandler() {
            return completionHandler;
        }

        ByteBuffer dataFrame() {
            return dataFrame;
        }
    }

    // -- Writer --------------------------------------------------------------

    @Override
    public void write(ByteBuffer byteBuffer, CompletionHandler<ByteBuffer> handler) {
        if (subscriber == null) {
            return;
        }
        if (requested.get() > 0) {
            if (requested.get() != Long.MAX_VALUE) {
                requested.decrementAndGet();
            }
            DataChunk dataChunk = DataChunk.create(true, byteBuffer, true);
            if (handler != null) {
                dataChunk.writeFuture(fromCompletionHandler(handler));
            }
            subscriber.onNext(dataChunk);
        } else {
            queue.add(new QueuedBuffer(byteBuffer, handler));
        }
    }

    @Override
    public void close() throws IOException {
        if (subscriber != null) {
            subscriber.onComplete();
        }
    }

    // -- Publisher -----------------------------------------------------------

    @Override
    public void subscribe(Flow.Subscriber<? super DataChunk> newSubscriber) {
        if (subscriber != null) {
            throw new IllegalStateException("Only one subscriber is allowed");
        }
        subscriber = newSubscriber;
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if (n == Long.MAX_VALUE) {
                    requested.set(Long.MAX_VALUE);
                } else {
                    requested.getAndAdd(n);
                }
            }

            @Override
            public void cancel() {
                requested.set(0L);
            }
        });
    }

    // -- Utility methods -----------------------------------------------------

    /**
     * Wraps {@code CompletionHandler} into a {@code CompletableFuture} so that
     * when the latter succeeds or fails so does the former.
     *
     * @param handler Handler to wrap.
     * @return Wrapped handler.
     */
    private static CompletableFuture<DataChunk> fromCompletionHandler(CompletionHandler<ByteBuffer> handler) {
        CompletableFuture<DataChunk> future = new CompletableFuture<>();
        future.whenComplete((chunk, throwable) -> {
            if (throwable == null) {
                handler.completed(chunk.data());
            } else {
                handler.failed(throwable);
            }
        });
        return future;
    }
}
