package com.lldprep.threadpool;

/**
 * Enumeration representing the lifecycle states of a thread pool.
 * 
 * State transitions:
 * RUNNING → SHUTDOWN → TERMINATED
 * RUNNING → TERMINATED (via shutdownNow)
 * 
 * State behaviors:
 * - RUNNING: Accept new tasks, execute queued tasks
 * - SHUTDOWN: Reject new tasks, execute queued tasks, wait for completion
 * - TERMINATED: Reject new tasks, no tasks executing, all threads stopped
 */
public enum PoolState {
    
    /**
     * Pool is accepting new tasks and executing queued tasks.
     */
    RUNNING,
    
    /**
     * Pool is not accepting new tasks but is executing queued tasks.
     * Initiated by shutdown() call.
     */
    SHUTDOWN,
    
    /**
     * Pool has completed all tasks and all worker threads have terminated.
     */
    TERMINATED
}
