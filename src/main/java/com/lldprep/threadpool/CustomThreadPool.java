package com.lldprep.threadpool;

import com.lldprep.threadpool.future.CustomFuture;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Interface representing a custom thread pool executor.
 * Does not use java.util.concurrent.Executor.
 * 
 * Design Patterns Applied:
 * - Strategy Pattern: Pluggable rejection policies
 * - Factory Pattern: Customizable thread creation
 * - Observer Pattern: Future-based result retrieval
 * 
 * SOLID Principles:
 * - SRP: Interface focused solely on task execution and lifecycle
 * - OCP: Extensible via strategy patterns
 * - DIP: Clients depend on this abstraction, not implementations
 */
public interface CustomThreadPool {

    /**
     * Submits a Runnable task for execution.
     * 
     * @param task the runnable task
     * @throws com.lldprep.threadpool.exception.RejectedExecutionException if task cannot be accepted
     */
    void submit(Runnable task);
    
    /**
     * Submits a Callable task for execution and returns a Future representing the result.
     * 
     * @param <T> the type of the task's result
     * @param task the callable task
     * @return a Future representing pending completion of the task
     * @throws com.lldprep.threadpool.exception.RejectedExecutionException if task cannot be accepted
     */
    <T> CustomFuture<T> submit(Callable<T> task);

    /**
     * Initiates an orderly shutdown.
     * Previously submitted tasks are executed, but no new tasks will be accepted.
     * This method does not wait for previously submitted tasks to complete execution.
     */
    void shutdown();

    /**
     * Attempts to stop all actively executing tasks and halts the
     * processing of waiting tasks.
     * 
     * @return list of tasks that were awaiting execution
     */
    List<Runnable> shutdownNow();
    
    /**
     * Returns true if this pool has been shut down.
     * 
     * @return true if the thread pool has been shut down
     */
    boolean isShutdown();
    
    /**
     * Returns true if all tasks have completed following shut down.
     * 
     * @return true if all tasks have completed following shut down
     */
    boolean isTerminated();
    
    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever happens first.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if this pool terminated and false if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
    
    /**
     * Returns the approximate number of threads that are actively executing tasks.
     * 
     * @return the number of active threads
     */
    int getActiveCount();
    
    /**
     * Returns the approximate total number of tasks that have completed execution.
     * 
     * @return the number of completed tasks
     */
    long getCompletedTaskCount();
    
    /**
     * Returns a snapshot of current pool metrics for monitoring.
     * 
     * @return ThreadPoolMetrics containing current state and statistics
     */
    ThreadPoolMetrics getMetrics();
}
