package com.kowshik.future;

import java.util.Collections;
import java.util.List;

/**
 * Immutable container for the result of a fan-out aggregation.
 *
 * <p>Generics note: {@code T} is the result type of each individual task.
 * Constructed only by {@link AsyncResultAggregator} — callers read it, never build it.
 *
 * @param <T> the type of a single successful result
 *
 * INTERVIEW PREP:
 * ==============
 * Q: Why is this class immutable?
 * A: Results are produced by concurrent tasks and handed off to the caller once
 *    all futures complete. Making the container immutable avoids the need for any
 *    post-construction synchronization — a clean happens-before guarantee is already
 *    provided by CompletableFuture.allOf(...).join().
 *
 * Q: Why store Throwable instead of Exception?
 * A: CompletableFuture wraps failures in CompletionException (which extends
 *    RuntimeException). The root cause can be any Throwable. Storing Throwable
 *    preserves full fidelity without losing type information.
 */
public final class AggregatedResult<T> {

    private final List<T> successes;
    private final List<TaskFailure> failures;

    AggregatedResult(List<T> successes, List<TaskFailure> failures) {
        this.successes = Collections.unmodifiableList(successes);
        this.failures  = Collections.unmodifiableList(failures);
    }

    public List<T> getSuccesses() {
        return successes;
    }

    public List<TaskFailure> getFailures() {
        return failures;
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    public int totalTasks() {
        return successes.size() + failures.size();
    }

    @Override
    public String toString() {
        return "AggregatedResult{successes=" + successes.size()
                + ", failures=" + failures.size()
                + ", total=" + totalTasks() + "}";
    }

    // -------------------------------------------------------------------------

    /**
     * Captures the task index and root cause for a failed task.
     */
    public static final class TaskFailure {
        private final int taskIndex;
        private final Throwable cause;

        TaskFailure(int taskIndex, Throwable cause) {
            this.taskIndex = taskIndex;
            this.cause = cause;
        }

        public int getTaskIndex() {
            return taskIndex;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return "TaskFailure{index=" + taskIndex + ", cause=" + cause.getMessage() + "}";
        }
    }
}
