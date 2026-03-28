package com.lldprep.threadpool.policy;

import com.lldprep.threadpool.CustomThreadPool;

/**
 * Strategy Pattern: Interface for handling task rejection when queue is full.
 * 
 * Different policies provide different backpressure strategies:
 * - AbortPolicy: Fail-fast with exception
 * - CallerRunsPolicy: Backpressure by running in caller's thread
 * - DiscardPolicy: Silently drop task
 * - DiscardOldestPolicy: Drop oldest task and retry
 */
public interface RejectionPolicy {
    
    /**
     * Handles rejection of a task that cannot be accepted by the thread pool.
     * 
     * @param task The task that was rejected
     * @param pool The thread pool that rejected the task
     */
    void reject(Runnable task, CustomThreadPool pool);
}
