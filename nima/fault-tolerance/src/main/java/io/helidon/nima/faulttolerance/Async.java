/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
import java.util.function.Supplier;

public interface Async {

    /**
     * Invoke a synchronous operation asynchronously.
     * This method never throws an exception. Any exception is returned via the
     * {@link java.util.concurrent.CompletableFuture} result.
     *
     * @param supplier supplier of value (may be a method reference)
     * @param <T> type of returned value
     * @return a Single that is a "promise" of the future result
     */
    <T> CompletableFuture<T> invoke(Supplier<T> supplier);

    /**
     * Async with default executor service.
     *
     * @return a default async instance
     */
    static Async create() {
        return AsyncImpl.DefaultAsyncInstance.instance();
    }

    /**
     * A new builder to build a customized {@link io.helidon.nima.faulttolerance.Async} instance.
     *
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience method avoid having to call {@link #create()}.
     *
     * @param supplier supplier of value (may be a method reference)
     * @param <T> type of returned value
     * @return a Single that is a "promise" of the future result
     */
    static <T> CompletableFuture<T> invokeAsync(Supplier<T> supplier) {
        return create().invoke(supplier);
    }

    /**
     * Fluent API Builder for {@link io.helidon.nima.faulttolerance.Async}.
     */
    class Builder implements io.helidon.common.Builder<Builder, Async> {

        private Builder() {
        }

        @Override
        public Async build() {
            return new AsyncImpl(this);
        }
    }
}
