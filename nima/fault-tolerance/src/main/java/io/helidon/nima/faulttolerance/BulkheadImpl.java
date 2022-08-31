/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.nima.faulttolerance;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

class BulkheadImpl implements Bulkhead {

    private final Queue<DelayedTask<?>> queue;
    private final Semaphore inProgress;
    private final String name;

    private final AtomicLong concurrentExecutions = new AtomicLong(0L);
    private final AtomicLong callsAccepted = new AtomicLong(0L);
    private final AtomicLong callsRejected = new AtomicLong(0L);

    BulkheadImpl(Bulkhead.Builder builder) {
        this.inProgress = new Semaphore(builder.limit(), true);
        this.name = builder.name();

        if (builder.queueLength() == 0) {
            queue = new NoQueue();
        } else {
            this.queue = new LinkedBlockingQueue<>(builder.queueLength());
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T invoke(Supplier<? extends T> supplier) {
        CompletableFuture<T> future = (CompletableFuture<T>) invokeAsync(() -> Async.invokeAsync(supplier));
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }

    @Override
    public <T> CompletableFuture<T> invokeAsync(Supplier<? extends CompletionStage<T>> supplier) {
        return invokeTask(DelayedTask.createFuture(supplier));
    }

    // this method must be called while NOT holding a permit
    @SuppressWarnings("unchecked")
    private <R> R invokeTask(DelayedTask<R> task) {
        if (inProgress.tryAcquire()) {
            // free permit, we can invoke
            execute(task);
            return task.result();
        } else {
            // no free permit, let's try to enqueue
            if (queue.offer(task)) {
                return task.result();
            } else {
                callsRejected.incrementAndGet();
                return task.error(new BulkheadException("Bulkhead queue \"" + name + "\" is full"));
            }
        }
    }

    // this method must be called while holding a permit
    private void execute(DelayedTask<?> task) {
        callsAccepted.incrementAndGet();
        concurrentExecutions.incrementAndGet();

        task.execute()
                .handle((it, throwable) -> {
                    concurrentExecutions.decrementAndGet();
                    DelayedTask<?> polled = queue.poll();
                    if (polled != null) {
                        Async.invokeAsync(() -> {
                            execute(polled);
                            return null;
                        });
                    } else {
                        inProgress.release();
                    }
                    return null;
                });
    }

    private static class NoQueue extends ArrayDeque<DelayedTask<?>> {
        @Override
        public boolean offer(DelayedTask delayedTask) {
            return false;
        }

        @SuppressWarnings("ReturnOfNull")
        @Override
        public DelayedTask<?> poll() {
            // this queue is empty, poll must return null
            return null;
        }
    }
}
