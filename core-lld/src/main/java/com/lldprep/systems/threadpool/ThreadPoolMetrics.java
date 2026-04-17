package com.lldprep.systems.threadpool;

/**
 * Immutable snapshot of thread pool metrics for monitoring and debugging.
 * 
 * Provides visibility into:
 * - Current pool state
 * - Active thread count
 * - Completed task count
 * - Queue size
 */
public class ThreadPoolMetrics {
    
    private final PoolState state;
    private final int poolSize;
    private final int activeCount;
    private final long completedTaskCount;
    private final int queueSize;
    
    public ThreadPoolMetrics(PoolState state, int poolSize, int activeCount, 
                            long completedTaskCount, int queueSize) {
        this.state = state;
        this.poolSize = poolSize;
        this.activeCount = activeCount;
        this.completedTaskCount = completedTaskCount;
        this.queueSize = queueSize;
    }
    
    public PoolState getState() {
        return state;
    }
    
    public int getPoolSize() {
        return poolSize;
    }
    
    public int getActiveCount() {
        return activeCount;
    }
    
    public long getCompletedTaskCount() {
        return completedTaskCount;
    }
    
    public int getQueueSize() {
        return queueSize;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ThreadPoolMetrics{state=%s, poolSize=%d, activeCount=%d, completedTaskCount=%d, queueSize=%d}",
            state, poolSize, activeCount, completedTaskCount, queueSize
        );
    }
}
