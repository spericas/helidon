/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.common.socket;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

class IdleInputStreamTest {

    @Test
    void testIdleInputStream() throws ExecutionException, InterruptedException {
        Barrier endBarrier = new Barrier();
        Barrier startBarrier = new Barrier();
        BarrierInputStream bis = new BarrierInputStream(startBarrier, endBarrier);
        IdleInputStream iis = new IdleInputStream(bis, "socketId", "socketId");

        iis.idle();
        startBarrier.waitOn();      // waits on idle thread to start
        iis.endIdle();
        endBarrier.waitOn();        // waits on idle thread interruption
    }

    static class BarrierInputStream extends InputStream {

        private final Barrier endBarrier;
        private final Barrier startBarrier;

        BarrierInputStream(Barrier startBarrier, Barrier endBarrier) {
            this.startBarrier = startBarrier;
            this.endBarrier = endBarrier;
        }

        @Override
        public int read() throws IOException {
            try {
                startBarrier.retract();
                Barrier nextBarrier = new Barrier();
                nextBarrier.waitOn();
                return 0;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                endBarrier.retract();
                return 0;
            }
        }
    }

    /**
     * A barrier is used to force a thread to wait (block) until it is retracted.
     */
    private static class Barrier {
        private final CompletableFuture<Void> future = new CompletableFuture<>();

        void waitOn() throws ExecutionException, InterruptedException {
            future.get();
        }

        void retract() {
            future.complete(null);
        }
    }
}
