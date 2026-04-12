package com.kowshik.future;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Generic fan-out aggregator: submits N independent tasks concurrently and
 * collects results, separating successes from failures.
 *
 * <p><b>Generics design:</b>
 * <ul>
 *   <li>{@code T} — the result type of each task (e.g., {@code Double} for prices,
 *       {@code String} for API responses).</li>
 *   <li>Tasks are supplied as {@code Supplier<? extends T>} (PECS — producer extends):
 *       the supplier <em>produces</em> a value that is at least a {@code T}, which lets
 *       callers pass {@code Supplier<BigDecimal>} where {@code T = Number}.</li>
 * </ul>
 *
 * <p><b>Concurrency design:</b>
 * <ul>
 *   <li>Each task runs in a {@link CompletableFuture} on the provided executor.</li>
 *   <li>Per-task timeout via {@code orTimeout} — a timed-out task is counted as a
 *       failure with {@link TimeoutException} as the cause; other tasks are unaffected.</li>
 *   <li>{@code CompletableFuture.allOf(...).join()} provides the happens-before fence:
 *       all writes from worker threads are visible to the collecting thread before
 *       results are read.</li>
 *   <li>Result lists are populated after all futures complete — no shared mutable state
 *       during concurrent execution.</li>
 * </ul>
 *
 * INTERVIEW PREP:
 * ==============
 * Q: Why use Supplier<? extends T> instead of Supplier<T>?
 * A: PECS — "Producer Extends, Consumer Super". A Supplier *produces* values.
 *    Using `? extends T` lets the API accept Supplier<BigDecimal> when T=Number,
 *    making the aggregator reusable across a type hierarchy without casting.
 *
 * Q: Why not use CompletableFuture.allOf().get() instead of .join()?
 * A: .join() throws CompletionException (unchecked) instead of ExecutionException
 *    (checked), making it cleaner inside lambda chains. Since we already handle
 *    per-task failures individually via .handle(), the outer .join() should never
 *    throw — it's safe here.
 *
 * Q: What happens if the executor is shut down before tasks complete?
 * A: supplyAsync will throw RejectedExecutionException immediately. The caller
 *    owns the executor lifecycle — this class does not shut it down.
 *
 * Q: Is this thread-safe to call from multiple threads simultaneously?
 * A: Yes. The aggregator has no mutable instance state. Each call to aggregate()
 *    creates its own local list of futures and result containers.
 *
 * @param <T> the common result type for all tasks in a single aggregation call
 */
public class AsyncResultAggregator<T> {

    private final ExecutorService executor;
    private final long taskTimeoutMs;

    /**
     * @param executor      the thread pool to run tasks on — caller manages lifecycle
     * @param taskTimeoutMs per-task timeout in milliseconds; tasks exceeding this are
     *                      marked as failures with {@link TimeoutException}
     */
    public AsyncResultAggregator(ExecutorService executor, long taskTimeoutMs) {
        this.executor = executor;
        this.taskTimeoutMs = taskTimeoutMs;
    }

    /**
     * Submits all tasks concurrently and returns once all have completed or timed out.
     *
     * <p>PECS in action: {@code Supplier<? extends T>} — each supplier produces a
     * value assignable to {@code T}. The returned {@link AggregatedResult} only
     * exposes {@code List<T>}, so the wildcard is safely erased at the boundary.
     *
     * @param tasks list of suppliers to execute concurrently
     * @return aggregated successes and failures, never null
     */
    public AggregatedResult<T> aggregate(List<? extends Supplier<? extends T>> tasks) {
        // Phase 1 — fan-out: create one CompletableFuture per task
        // Each future completes with either (success, null) or (null, throwable)
        // via .handle() — so allOf never sees a failed future itself.
        List<CompletableFuture<TaskOutcome<T>>> futures = new ArrayList<>(tasks.size());

        for (int i = 0; i < tasks.size(); i++) {
            final int index = i;
            Supplier<? extends T> task = tasks.get(i);

            CompletableFuture<TaskOutcome<T>> future = CompletableFuture
                    // CRITICAL SECTION — supplyAsync submits to the executor
                    .supplyAsync(task::get, executor)
                    // Per-task timeout: if the supplier doesn't return in time,
                    // the future completes exceptionally with TimeoutException
                    .orTimeout(taskTimeoutMs, TimeUnit.MILLISECONDS)
                    // .handle() always fires — on success AND on failure.
                    // It converts both paths into a uniform TaskOutcome, so
                    // allOf() below never sees a failed stage.
                    .handle((result, ex) -> {
                        if (ex == null) {
                            return TaskOutcome.<T>success(index, result);
                        }
                        // CompletableFuture wraps checked exceptions in CompletionException
                        Throwable rootCause = (ex instanceof CompletionException && ex.getCause() != null)
                                ? ex.getCause()
                                : ex;
                        return TaskOutcome.<T>failure(index, rootCause);
                    });

            futures.add(future);
        }

        // Phase 2 — barrier: wait for ALL futures (none can fail now — .handle absorbed errors)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Phase 3 — collect: read results sequentially after the happens-before fence
        List<T> successes = new ArrayList<>();
        List<AggregatedResult.TaskFailure> failures = new ArrayList<>();

        for (CompletableFuture<TaskOutcome<T>> future : futures) {
            TaskOutcome<T> outcome = future.join(); // already done, no blocking
            if (outcome.isSuccess()) {
                successes.add(outcome.getResult());
            } else {
                failures.add(new AggregatedResult.TaskFailure(outcome.getIndex(), outcome.getCause()));
            }
        }

        return new AggregatedResult<>(successes, failures);
    }

    // -------------------------------------------------------------------------
    // Private helper — uniform outcome wrapper to avoid raw Optional/null juggling

    private static final class TaskOutcome<T> {
        private final int index;
        private final T result;
        private final Throwable cause;

        private TaskOutcome(int index, T result, Throwable cause) {
            this.index  = index;
            this.result = result;
            this.cause  = cause;
        }

        static <T> TaskOutcome<T> success(int index, T result) {
            return new TaskOutcome<>(index, result, null);
        }

        static <T> TaskOutcome<T> failure(int index, Throwable cause) {
            return new TaskOutcome<>(index, null, cause);
        }

        boolean isSuccess() { return cause == null; }
        int getIndex()      { return index; }
        T getResult()       { return result; }
        Throwable getCause(){ return cause; }
    }
}
