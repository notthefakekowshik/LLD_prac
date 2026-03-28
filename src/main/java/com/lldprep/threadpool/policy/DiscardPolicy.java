package com.lldprep.threadpool.policy;

import com.lldprep.threadpool.CustomThreadPool;

/**
 * Rejection policy that silently discards the rejected task.
 * 
 * Use case: When task loss is acceptable (e.g., real-time metrics,
 * non-critical logging) and you want to avoid blocking or exceptions.
 * 
 * Warning: Tasks are lost without notification. Use with caution.
 */
public class DiscardPolicy implements RejectionPolicy {
    
    @Override
    public void reject(Runnable task, CustomThreadPool pool) {
    }
}
