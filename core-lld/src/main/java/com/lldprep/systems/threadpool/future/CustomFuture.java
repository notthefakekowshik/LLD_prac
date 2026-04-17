package com.lldprep.systems.threadpool.future;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

/**
 * Interface representing the result of an asynchronous computation.
 * 
 * Provides methods to:
 * - Check if computation is complete
 * - Wait for completion and retrieve result
 * - Cancel the computation
 * 
 * @param <T> The result type returned by the Future's get method
 */
public interface CustomFuture<T> {
    
    /**
     * Attempts to cancel execution of this task.
     * 
     * @param mayInterruptIfRunning true if the thread executing this task
     *        should be interrupted; otherwise, in-progress tasks are allowed to complete
     * @return false if the task could not be cancelled (already completed, already cancelled,
     *         or could not be cancelled for some other reason); true otherwise
     */
    boolean cancel(boolean mayInterruptIfRunning);
    
    /**
     * Returns true if this task was cancelled before it completed normally.
     * 
     * @return true if this task was cancelled before it completed
     */
    boolean isCancelled();
    
    /**
     * Returns true if this task completed (normally, exceptionally, or via cancellation).
     * 
     * @return true if this task completed
     */
    boolean isDone();
    
    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     * 
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     */
    T get() throws InterruptedException, ExecutionException;
    
    /**
     * Waits if necessary for at most the given time for the computation to complete,
     * and then retrieves its result, if available.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
