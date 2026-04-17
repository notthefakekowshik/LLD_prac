package com.lldprep.systems.threadpool.policy;

import com.lldprep.systems.threadpool.CustomThreadPool;
import com.lldprep.systems.threadpool.exception.RejectedExecutionException;

/**
 * Rejection policy that throws RejectedExecutionException.
 * 
 * Use case: Fail-fast behavior when system is overloaded.
 * Caller must handle the exception and decide what to do.
 */
public class AbortPolicy implements RejectionPolicy {
    
    @Override
    public void reject(Runnable task, CustomThreadPool pool) {
        throw new RejectedExecutionException(
            "Task " + task.toString() + " rejected from " + pool.toString()
        );
    }
}
