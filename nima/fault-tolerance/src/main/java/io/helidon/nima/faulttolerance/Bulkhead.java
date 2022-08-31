/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Bulkhead protects a resource that cannot serve unlimited parallel
 * requests.
 * <p>
 * When the limit of parallel execution is reached, requests are enqueued
 * until the queue length is reached. Once both the limit and queue are full,
 * additional attempts to invoke will end with a failed response with
 * {@link io.helidon.nima.faulttolerance.BulkheadException}.
 */
public interface Bulkhead extends FtHandler {
    /**
     * A new builder for {@link io.helidon.nima.faulttolerance.Bulkhead}.
     *
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    <T> CompletableFuture<T> invokeAsync(Supplier<? extends CompletionStage<T>> supplier);

    /**
     * Fluent API builder for {@link io.helidon.nima.faulttolerance.Bulkhead}.
     */
    class Builder implements io.helidon.common.Builder<Builder, Bulkhead> {
        private static final int DEFAULT_LIMIT = 10;
        private static final int DEFAULT_QUEUE_LENGTH = 10;

        private int limit = DEFAULT_LIMIT;
        private int queueLength = DEFAULT_QUEUE_LENGTH;
        private String name = "Bulkhead-" + System.identityHashCode(this);

        private Builder() {
        }

        @Override
        public Bulkhead build() {
            return new BulkheadImpl(this);
        }

        /**
         * Maximal number of parallel requests going through this bulkhead.
         * When the limit is reached, additional requests are enqueued.
         *
         * @param limit maximal number of parallel calls, defaults is {@value DEFAULT_LIMIT}
         * @return updated builder instance
         */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Maximal number of enqueued requests waiting for processing.
         * When the limit is reached, additional attempts to invoke
         * a request will receive a {@link BulkheadException}.
         *
         * @param queueLength length of queue
         * @return updated builder instance
         */
        public Builder queueLength(int queueLength) {
            this.queueLength = queueLength;
            return this;
        }

        /**
         * A name assigned for debugging, error reporting or configuration purposes.
         *
         * @param name the name
         * @return updated builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        int limit() {
            return limit;
        }

        int queueLength() {
            return queueLength;
        }

        String name() {
            return name;
        }
    }
}
