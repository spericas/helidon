/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.common;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

class LazyValueImpl<T> implements LazyValue<T> {
    private volatile Semaphore theLock;
    private static final VarHandle THE_LOCK;
    private static final VarHandle LOADED;

    static {
       try {
          THE_LOCK = MethodHandles.lookup().findVarHandle(LazyValueImpl.class, "theLock", Semaphore.class);
          LOADED = MethodHandles.lookup().findVarHandle(LazyValueImpl.class, "loaded", int.class);
       } catch(Exception e) {
          throw new Error("Expected this to succeed", e);
       }
    }

    private T value;

    private Supplier<T> delegate;
    private volatile int loaded;

    private final static int DONE = -1;
    private final static int INIT = 0;
    private final static int WORKING = INIT + 1;

    LazyValueImpl(T value) {
        this.value = value;
        this.loaded = DONE;
    }

    LazyValueImpl(Supplier<T> supplier) {
        this.delegate = supplier;
    }

    @Override
    public boolean isLoaded() {
        return loaded == DONE;
    }

    @Override
    public T get() {
        int l = loaded;
        if (l == DONE) {
            return value;
        }

        Semaphore sema = theLock;
        while(l != DONE && !LOADED.compareAndSet(this, INIT, WORKING)) {
           // of those who lost the race to grab loaded in state INIT, one manages
           // to set theLock
           if (sema == null) {
              THE_LOCK.compareAndSet(this, null, new Semaphore(0));
              sema = theLock;
           }

           l = loaded;

           if (l == WORKING) {
               sema.acquireUninterruptibly();
               l = loaded;
           }
        }

        try {
            if (l == DONE) {
                return value;
            }
            l = INIT;
            value = delegate.get();
            delegate = null;
            l = DONE;
            loaded = DONE;
        } finally {
            if (l == INIT) {
               // must be leaving exceptionally - delegate.get() may throw
               loaded = INIT;
            }
            // assert: if theLock is null, the successful CAS of theLock is in the future;
            //         but after such CAS there will be a check of loaded, which will observe
            //         that it is not WORKING, and not enter acquire()
            sema = theLock;
            if (sema != null) {
                sema.release();
            }
        }

        return value;
    }
}
