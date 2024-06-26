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
package io.helidon.common.reactive;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MultiTimeoutTest {

    private static ScheduledExecutorService executor;

    @BeforeAll
    public static void beforeClass() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterAll
    public static void afterClass() {
        executor.shutdown();
    }

    @Test
    public void fallback() {
        TestSubscriber<Integer> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.<Integer>never()
                .timeout(1, TimeUnit.MILLISECONDS, executor, Multi.just(1))
                .subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1);
    }

    @Test
    public void fallback2() {
        TestSubscriber<Long> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.timer(1, TimeUnit.MINUTES, executor)
                .timeout(1, TimeUnit.MILLISECONDS, executor, Multi.just(1L))
                .subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1L);
    }

    @Test
    public void mainError() {
        TestSubscriber<Long> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.<Long>error(new IOException())
                .timeout(1, TimeUnit.MINUTES, executor)
                .subscribe(ts);

        ts.assertFailure(IOException.class);
    }

    @Test
    public void mainEmpty() {
        TestSubscriber<Long> ts = new TestSubscriber<>(Long.MAX_VALUE);

        Multi.<Long>empty()
                .timeout(1, TimeUnit.MINUTES, executor)
                .subscribe(ts);

        ts.assertResult();
    }

    @Test
    public void mainCanceled() throws Exception {
        TestSubscriber<Long> ts = new TestSubscriber<>(Long.MAX_VALUE);

        SubmissionPublisher<Long> sp = new SubmissionPublisher<>(Runnable::run, 128);

        Multi.create(sp)
                .timeout(1, TimeUnit.MILLISECONDS, executor)
                .subscribe(ts);

        for (int i = 0; i < 5000; i++) {
            if (!sp.hasSubscribers()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Did not cancel the main source!");
    }

    @Test
    public void fallbackBackpressure() throws Exception {
        TestSubscriber<Long> ts = new TestSubscriber<>(1L);

        SubmissionPublisher<Long> sp = new SubmissionPublisher<>(Runnable::run, 128);
        SubmissionPublisher<Long> sp2 = new SubmissionPublisher<>(Runnable::run, 128);

        Multi.create(sp)
                .timeout(100, TimeUnit.MILLISECONDS, executor, sp2)
                .subscribe(ts);

        sp.submit(1L);

        for (int i = 0; i < 5000; i++) {
            if (!sp.hasSubscribers()) {
                break;
            }
            Thread.sleep(10);
        }

        assertThat(sp.hasSubscribers(), is(false));

        ts.assertValuesOnly(1L);

        assertThat(sp2.hasSubscribers(), is(true));

        sp2.submit(2L);
        sp2.close();

        ts.assertValuesOnly(1L);

        ts.request(1)
                .assertResult(1L, 2L);

    }
}
