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

import io.helidon.common.LogConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BulkheadTest {

    private static final long WAIT_TIMEOUT_MILLIS = 50000000;

    @BeforeAll
    static void setupTest() {
        LogConfig.configureRuntime();
    }

    @Test
    void testBulkhead() throws InterruptedException, ExecutionException, TimeoutException {
        // Create bulkhead of 1 with queue length 1
        String name = "unit:testBulkhead";
        Bulkhead bulkhead = Bulkhead.builder()
                .limit(1)
                .queueLength(1)
                .name(name)
                .build();

        // Create 3 tasks, one will be queued, one will be rejected
        Task inProgress = new Task(0);
        CompletableFuture<Integer> inProgressResult = bulkhead.invokeAsync(inProgress::run);
        Task enqueued = new Task(1);
        CompletableFuture<Integer> enqueuedResult = bulkhead.invokeAsync(enqueued::run);
        Task rejected = new Task(2);
        CompletableFuture<Integer> rejectedResult = bulkhead.invokeAsync(rejected::run);

        // Wait for inProgress task to start and check state
        if (!inProgress.waitUntilStarted(WAIT_TIMEOUT_MILLIS)) {
            fail("Invoke method of inProgress was not called");
        }
        assertThat(inProgress.isStarted(), is(true));
        assertThat(inProgress.isBlocked(), is(true));
        assertThat(enqueued.isStarted(), is(false));
        assertThat(enqueued.isBlocked(), is(true));
        assertThat(rejected.isStarted(), is(false));
        assertThat(rejected.isBlocked(), is(true));

        // Unblock inProgress task and get result to free bulkhead
        inProgress.unblock();
        inProgressResult.get(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        // Wait for enqueued task to start and check state
        if (!enqueued.waitUntilStarted(WAIT_TIMEOUT_MILLIS)) {
            fail("Invoke method of inProgress was not called");
        }
        assertThat(enqueued.isStarted(), is(true));
        assertThat(enqueued.isBlocked(), is(true));
        assertThat(rejected.isStarted(), is(false));
        assertThat(rejected.isBlocked(), is(true));

        // Unblock enqueued task and get result
        enqueued.unblock();
        enqueuedResult.get(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        // Verify rejected task was indeed rejected
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> rejectedResult.get(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        Throwable cause = executionException.getCause();
        assertThat(cause, notNullValue());
        assertThat(cause, instanceOf(BulkheadException.class));
        assertThat(cause.getMessage(), is("Bulkhead queue \"" + name + "\" is full"));
    }

    @Test
    void testBulkheadQueue() throws InterruptedException, ExecutionException, TimeoutException {
        // Create bulkhead of 1 with a queue of 1000
        Bulkhead bulkhead = Bulkhead.builder()
                .limit(1)
                .queueLength(1000)
                .build();

        // Submit request to bulkhead of limit 1
        Task first = new Task(0);
        CompletableFuture<?> firstFuture = bulkhead.invokeAsync(first::run);

        // Submit additional request to fill up queue
        Task[] tasks = new Task[999];
        for (int i = 0; i < tasks.length; i++) {
            Task task = new Task(i + 1);
            tasks[i] = task;
            CompletableFuture<?> f = bulkhead.invokeAsync(task::run);
            tasks[i].future(f);
        }

        // Verify all tasks are queued and unblock them
        for (Task task : tasks) {
            assertFalse(task.isStarted());
            task.unblock();
        }

        // Let first complete operation and free bulkhead
        assertTrue(first.isBlocked());
        first.unblock();
        firstFuture.get(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        // Get all results
        for (Task task : tasks) {
            task.future().get(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    void testBulkheadWithError() throws InterruptedException, ExecutionException, TimeoutException {
        // Create bulkhead of 1 with a queue of 1
        Bulkhead bulkhead = Bulkhead.builder()
                .limit(1)
                .queueLength(1)
                .build();

        // First check exception throw using synchronous call
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> bulkhead.invoke(() -> {
            throw new IllegalStateException();
        }));
        Throwable cause = runtimeException.getCause();
        assertThat(cause, notNullValue());
        assertThat(cause, instanceOf(IllegalStateException.class));

        // Send 2 tasks to bulkhead, one that fails
        Task inProgress = new Task(0);
        CompletableFuture<?> inProgressFuture = bulkhead.invokeAsync(inProgress::run);
        CompletableFuture<?> failedFuture = bulkhead.invokeAsync(() -> {
            throw new IllegalStateException();
        });

        // Verify completion of inProgress task
        if (!inProgress.waitUntilStarted(WAIT_TIMEOUT_MILLIS)) {
            fail("Invoke method of inProgress was not called");
        }
        inProgress.unblock();
        inProgressFuture.get(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        // Verify failure of other task
        ExecutionException executionException = assertThrows(ExecutionException.class,
                () -> failedFuture.get(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
        cause = executionException.getCause();
        assertThat(cause, notNullValue());
        assertThat(cause, instanceOf(IllegalStateException.class));
    }

    private static class Task {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch blocked = new CountDownLatch(1);

        private final int index;
        private CompletableFuture<?> future;

        Task(int index) {
            this.index = index;
        }

        CompletableFuture<Integer> run() {
            System.out.println("Task running in " + Thread.currentThread());
            started.countDown();
            return Async.invokeAsync(() -> {
                try {
                    blocked.await();
                    return index;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        boolean isStarted() {
            return started.getCount() == 0;
        }

        boolean waitUntilStarted(long millis) throws InterruptedException {
            return started.await(millis, TimeUnit.MILLISECONDS);
        }

        boolean isBlocked() {
            return blocked.getCount() == 1;
        }

        void unblock() {
            blocked.countDown();
        }

        void future(CompletableFuture<?> future) {
            this.future = future;
        }

        CompletableFuture<?> future() {
            return future;
        }
    }
}
