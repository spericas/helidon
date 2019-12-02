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

package io.helidon.common;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Class ThreadLocalCopier.
 */
public class ThreadLocalCopier {
    private static final Logger LOGGER = Logger.getLogger(ThreadLocalCopier.class.getName());

    final Thread origin;
    final Predicate<Object> filter;

    /**
     * Creates a copier.
     *
     * @param origin Origin thread.
     * @param filter Predicate to filter thread locals.
     */
    public ThreadLocalCopier(Thread origin, Predicate<Object> filter) {
        this.origin = origin;
        this.filter = filter;
    }

    /**
     * Uses reflection to copy thread locals from {@code origin} into current thread.
     *
     * ThreadLocalMap originMap = origin.threadLocals;
     * ThreadLocalMap destinationMap = destination.threadLocals;
     * Array table = originMap.table;
     * for (i = 0; i < table.length; i++) {
     *   Entry entry = table.get(i);
     *   if (entry != null) {
     *     ThreadLocal threadLocal = entry.get();
     *     Object value = entry.value);
     *     destinationMap.set(threadLocal, value);
     *   }
     * }
     */
    public List<ThreadLocal<?>> copy() throws Exception {
        Thread destination = Thread.currentThread();
        List<ThreadLocal<?>> threadLocals = new ArrayList<>();
        Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
        threadLocalsField.setAccessible(true);

        Object originMap = threadLocalsField.get(origin);
        if (originMap == null) {
            return Collections.emptyList();
        }
        Object destinationMap = threadLocalsField.get(destination);

        Field tableField = Class.forName("java.lang.ThreadLocal$ThreadLocalMap").getDeclaredField("table");
        tableField.setAccessible(true);
        Object table = tableField.get(originMap);

        Method createMap = Class.forName("java.lang.ThreadLocal")
                .getDeclaredMethod("createMap", Thread.class, Object.class);
        createMap.setAccessible(true);

        int length = Array.getLength(table);
        for (int i = 0; i < length; i++) {
            Object entry = Array.get(table, i);
            if (entry != null) {
                Method getMethod = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry")
                        .getMethod("get");
                getMethod.setAccessible(true);
                ThreadLocal<?> threadLocal = (ThreadLocal<?>) getMethod.invoke(entry);

                Field valueField = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry")
                        .getDeclaredField("value");
                valueField.setAccessible(true);
                Object value = valueField.get(entry);

                if (value != null && filter.test(value)) {
                    if (destinationMap == null) {
                        createMap.invoke(threadLocal, destination, value);
                        destinationMap = threadLocalsField.get(destination);
                    } else {
                        Method setMethod = Class.forName("java.lang.ThreadLocal$ThreadLocalMap")
                                .getDeclaredMethod("set", ThreadLocal.class, Object.class);
                        setMethod.setAccessible(true);
                        setMethod.invoke(destinationMap, threadLocal, value);
                    }
                    threadLocals.add(threadLocal);
                    LOGGER.info(() -> "Copying: " + threadLocal + " " + value + " to " + destination);
                }
            }
        }

        return threadLocals;
    }
}
