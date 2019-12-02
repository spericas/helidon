package io.helidon.common;

import java.util.List;
import java.util.function.Predicate;

/**
 * Class ThreadLocalRunnable.
 */
public class ThreadLocalRunnable implements Runnable {

    private final Runnable task;
    private final ThreadLocalCopier threadLocalCopier;

    public ThreadLocalRunnable(Runnable task, Thread origin, Predicate<Object> predicate) {
        this.task = task;
        threadLocalCopier = new ThreadLocalCopier(origin, predicate);
    }

    public void run() {
        List<ThreadLocal<?>> vars;
        try {
            vars = threadLocalCopier.copy();
        } catch (Exception e) {
            throw new RuntimeException("Problems copying thread locals", e);
        }
        try {
            task.run();
        } finally {
            for (ThreadLocal<?> var : vars) {
                var.remove();
            }
        }
    }
}