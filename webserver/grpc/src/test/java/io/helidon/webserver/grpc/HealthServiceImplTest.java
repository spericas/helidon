/*
 * Copyright (c) 2019, 2024 Oracle and/or its affiliates.
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

package io.helidon.webserver.grpc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.stub.StreamObserver;
import org.eclipse.microprofile.health.HealthCheck;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HealthServiceImplTest {

    @Test
    public void shouldRequestCheckForUpService() {
        HealthServiceImpl healthService = HealthServiceImpl.create();
        String serviceName = "foo";
        HealthCheck check = ConstantHealthCheck.up(serviceName);
        HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(serviceName).build();
        TestStreamObserver<HealthCheckResponse> observer = new TestStreamObserver<>();
        healthService.add(serviceName, check);
        healthService.check(request, observer);
        observer.assertComplete().assertValueCount(1);
        List<HealthCheckResponse> responses = observer.values();
        assertThat(responses.size(), is(1));
        HealthCheckResponse response = responses.getFirst();
        assertThat(response.getStatus(), is(HealthCheckResponse.ServingStatus.SERVING));
    }

    @Test
    public void shouldRequestCheckForDownService() {
        HealthServiceImpl healthService = HealthServiceImpl.create();
        String serviceName = "foo";
        HealthCheck check = ConstantHealthCheck.down(serviceName);
        HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(serviceName).build();
        TestStreamObserver<HealthCheckResponse> observer = new TestStreamObserver<>();
        healthService.add(serviceName, check);
        healthService.check(request, observer);
        observer.assertComplete().assertValueCount(1);
        List<HealthCheckResponse> responses = observer.values();
        assertThat(responses.size(), is(1));
        HealthCheckResponse response = responses.getFirst();
        assertThat(response.getStatus(), is(HealthCheckResponse.ServingStatus.NOT_SERVING));
    }

    @Test
    public void shouldRequestCheckForGlobalService() {
        HealthServiceImpl healthService = HealthServiceImpl.create();
        String serviceName = "";
        HealthCheck check = ConstantHealthCheck.up(serviceName);
        HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(serviceName).build();
        TestStreamObserver<HealthCheckResponse> observer = new TestStreamObserver<>();
        healthService.add(serviceName, check);
        healthService.check(request, observer);
        observer.assertComplete().assertValueCount(1);
        List<HealthCheckResponse> responses = observer.values();
        assertThat(responses.size(), is(1));
        HealthCheckResponse response = responses.getFirst();
        assertThat(response.getStatus(), is(HealthCheckResponse.ServingStatus.SERVING));
    }

    @Test
    public void shouldRequestCheckWithoutServiceName() {
        HealthServiceImpl healthService = HealthServiceImpl.create();
        String serviceName = "";
        HealthCheck check = ConstantHealthCheck.up(serviceName);
        HealthCheckRequest request = HealthCheckRequest.newBuilder().build();
        TestStreamObserver<HealthCheckResponse> observer = new TestStreamObserver<>();
        healthService.add(serviceName, check);
        healthService.check(request, observer);
        observer.assertComplete().assertValueCount(1);
        List<HealthCheckResponse> responses = observer.values();
        assertThat(responses.size(), is(1));
        HealthCheckResponse response = responses.getFirst();
        assertThat(response.getStatus(), is(HealthCheckResponse.ServingStatus.SERVING));
    }

    @Test
    public void shouldRequestCheckForUnknownService() {
        HealthServiceImpl healthService = HealthServiceImpl.create();
        String serviceName = "unknown";
        HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(serviceName).build();
        TestStreamObserver<HealthCheckResponse> observer = new TestStreamObserver<>();
        healthService.check(request, observer);
        observer.assertError(this::isNotFoundError);
    }

    private boolean isNotFoundError(Throwable thrown) {
        if (thrown instanceof StatusException) {
            return ((StatusException) thrown).getStatus().getCode().equals(Status.NOT_FOUND.getCode());
        } else if (thrown instanceof StatusRuntimeException) {
            return ((StatusRuntimeException) thrown).getStatus().getCode().equals(Status.NOT_FOUND.getCode());
        } else {
            return false;
        }
    }

    static class TestStreamObserver<T> implements StreamObserver<T> {

        private final List<T> values = new ArrayList<>();
        private final CompletableFuture<Boolean> future = new CompletableFuture<>();

        @Override
        public void onNext(T t) {
            values.add(t);
        }

        @Override
        public void onError(Throwable throwable) {
            values.clear();
            future.completeExceptionally(throwable);
        }

        @Override
        public void onCompleted() {
            future.complete(true);
        }

        List<T> values() {
            return values;
        }

        void assertError(Predicate<Throwable> consumer) {
            if (future.isCompletedExceptionally()) {
                try {
                    future.get();
                } catch (Exception e) {
                    assertThat(consumer.test(e.getCause()), is(true));
                }
            }
        }

        TestStreamObserver<T> assertComplete() {
            assertThat(awaitTerminalEvent(), is(true));
            return this;
        }

        TestStreamObserver<T> assertValueCount(int count) {
            assertThat(values.size(), is(count));
            return this;
        }

        private boolean awaitTerminalEvent() {
            try {
                future.get(10, TimeUnit.SECONDS);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
