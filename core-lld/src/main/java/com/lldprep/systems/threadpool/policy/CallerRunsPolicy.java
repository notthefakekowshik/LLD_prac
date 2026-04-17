package com.lldprep.systems.threadpool.policy;

import com.lldprep.systems.threadpool.CustomThreadPool;

/**
 * Rejection policy that executes the task in the caller's thread.
 * 
 * Use case: Provides natural backpressure - caller is slowed down
 * by having to execute the task itself, preventing queue overflow.
 * 
 * Trade-off: Caller thread is blocked during task execution.
 */
public class CallerRunsPolicy implements RejectionPolicy {
    
    @Override
    public void reject(Runnable task, CustomThreadPool pool) {
        if (!pool.isShutdown()) {
            task.run();
        }
    }
}
