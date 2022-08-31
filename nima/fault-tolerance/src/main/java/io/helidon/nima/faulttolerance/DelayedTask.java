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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;;
import java.util.function.Supplier;

import io.helidon.common.LazyValue;

import static io.helidon.nima.faulttolerance.FaultTolerance.createDependency;


interface DelayedTask<T> {
    // the result completes when the call fully completes (regardless of errors)
    CompletionStage<Void> execute();

    // get the (new) result
    T result();

    // create an error result
    T error(Throwable throwable);

    // cannot retry or fallback when data was already sent (only useful for multi)
    default boolean hadData() {
        return false;
    }

    static <T> DelayedTask<CompletableFuture<T>> createFuture(Supplier<? extends CompletionStage<T>> supplier) {
        return createFuture(supplier, true);
    }

    static <T> DelayedTask<CompletableFuture<T>> createFuture(Supplier<? extends CompletionStage<T>> supplier,
                                                              boolean cancelSource) {
        return new DelayedTask<>() {
            // future we returned as a result of invoke command
            private final LazyValue<CompletableFuture<T>> resultFuture = LazyValue.create(CompletableFuture::new);

            @Override
            public CompletionStage<Void> execute() {
                CompletionStage<T> result;
                try {
                    result = supplier.get();
                } catch (Exception e) {
                    result = CompletableFuture.failedStage(e);
                }

                CompletableFuture<T> future = resultFuture.get();
                createDependency(result, future);
                return result.thenRun(() -> {});
            }

            @Override
            public CompletableFuture<T> result() {
                return resultFuture.get();
            }

            @Override
            public CompletableFuture<T> error(Throwable throwable) {
                return CompletableFuture.failedFuture(throwable);
            }

            @Override
            public String toString() {
                return "DelayedTask " + System.identityHashCode(this);
            }
        };
    }
}
