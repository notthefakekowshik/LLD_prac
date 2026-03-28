package com.lldprep.threadpool.policy;

import com.lldprep.threadpool.CustomThreadPool;

/**
 * Rejection policy that discards the oldest unexecuted task and retries.
 * 
 * Use case: When newer tasks are more important than older ones
 * (e.g., real-time data processing where stale data is less valuable).
 * 
 * Note: This implementation is simplified. Full implementation would
 * require access to the queue to remove the oldest task.
 */
public class DiscardOldestPolicy implements RejectionPolicy {
    
    @Override
    public void reject(Runnable task, CustomThreadPool pool) {
        if (!pool.isShutdown()) {
            pool.submit(task);
        }
    }
}
