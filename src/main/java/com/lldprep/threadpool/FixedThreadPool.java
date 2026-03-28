package com.lldprep.threadpool;

import com.lldprep.threadpool.exception.RejectedExecutionException;
import com.lldprep.threadpool.factory.DefaultThreadFactory;
import com.lldprep.threadpool.factory.ThreadFactory;
import com.lldprep.threadpool.future.CustomFuture;
import com.lldprep.threadpool.future.FutureTask;
import com.lldprep.threadpool.policy.AbortPolicy;
import com.lldprep.threadpool.policy.RejectionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Production-grade implementation of CustomThreadPool with fixed number of threads.
 * 
 * Design Patterns Applied:
 * 1. Strategy Pattern - Pluggable RejectionPolicy for handling queue overflow
 * 2. Factory Pattern - Customizable ThreadFactory for thread creation
 * 3. Builder Pattern - Complex construction with validation
 * 4. State Pattern - Lifecycle management (RUNNING → SHUTDOWN → TERMINATED)
 * 
 * SOLID Principles:
 * - SRP: FixedThreadPool manages lifecycle, WorkerThread executes tasks
 * - OCP: New rejection policies/thread factories can be added without modification
 * - DIP: Depends on RejectionPolicy and ThreadFactory interfaces
 * 
 * Thread Safety:
 * - AtomicReference for state transitions
 * - BlockingQueue for thread-safe task queue
 * - AtomicInteger/AtomicLong for metrics
 * - CountDownLatch for termination signaling
 * 
 * Time Complexity: O(1) for submit (amortized), O(k) for shutdown where k = pool size
 * Space Complexity: O(k + n) where k = pool size, n = queue capacity
 */
public class FixedThreadPool implements CustomThreadPool {
    
    private final BlockingQueue<Runnable> taskQueue;
    private final List<WorkerThread> workers;
    private final AtomicReference<PoolState> state;
    private final RejectionPolicy rejectionPolicy;
    private final ThreadFactory threadFactory;
    private final int poolSize;
    
    private final AtomicInteger activeCount;
    private final AtomicLong completedTaskCount;
    private final CountDownLatch terminationLatch;
    
    private FixedThreadPool(Builder builder) {
        this.poolSize = builder.poolSize;
        this.taskQueue = builder.queueCapacity > 0 
            ? new LinkedBlockingQueue<>(builder.queueCapacity)
            : new LinkedBlockingQueue<>();
        this.rejectionPolicy = builder.rejectionPolicy;
        this.threadFactory = builder.threadFactory;
        
        this.state = new AtomicReference<>(PoolState.RUNNING);
        this.activeCount = new AtomicInteger(0);
        this.completedTaskCount = new AtomicLong(0);
        this.terminationLatch = new CountDownLatch(poolSize);
        
        this.workers = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            WorkerThread worker = new WorkerThread(this);
            workers.add(worker);
            Thread thread = threadFactory.newThread(worker);
            thread.start();
        }
    }
    
    @Override
    public void submit(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        
        if (state.get() != PoolState.RUNNING) {
            rejectionPolicy.reject(task, this);
            return;
        }
        
        if (!taskQueue.offer(task)) {
            rejectionPolicy.reject(task, this);
        }
    }
    
    @Override
    public <T> CustomFuture<T> submit(Callable<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        
        FutureTask<T> futureTask = new FutureTask<>(task);
        submit((Runnable) futureTask);
        return futureTask;
    }
    
    @Override
    public void shutdown() {
        if (state.compareAndSet(PoolState.RUNNING, PoolState.SHUTDOWN)) {
            System.out.println("[FixedThreadPool] Initiating graceful shutdown. No new tasks accepted.");
        }
    }
    
    @Override
    public List<Runnable> shutdownNow() {
        state.set(PoolState.SHUTDOWN);
        
        List<Runnable> pendingTasks = new ArrayList<>();
        taskQueue.drainTo(pendingTasks);
        
        System.out.println("[FixedThreadPool] Immediate shutdown. Discarded " + pendingTasks.size() + " pending tasks.");
        
        for (WorkerThread worker : workers) {
            worker.interrupt();
        }
        
        return pendingTasks;
    }
    
    @Override
    public boolean isShutdown() {
        return state.get() != PoolState.RUNNING;
    }
    
    @Override
    public boolean isTerminated() {
        return state.get() == PoolState.TERMINATED;
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminationLatch.await(timeout, unit);
    }
    
    @Override
    public int getActiveCount() {
        return activeCount.get();
    }
    
    @Override
    public long getCompletedTaskCount() {
        return completedTaskCount.get();
    }
    
    @Override
    public ThreadPoolMetrics getMetrics() {
        return new ThreadPoolMetrics(
            state.get(),
            poolSize,
            activeCount.get(),
            completedTaskCount.get(),
            taskQueue.size()
        );
    }
    
    BlockingQueue<Runnable> getTaskQueue() {
        return taskQueue;
    }
    
    PoolState getState() {
        return state.get();
    }
    
    void incrementActiveCount() {
        activeCount.incrementAndGet();
    }
    
    void decrementActiveCount() {
        activeCount.decrementAndGet();
    }
    
    void incrementCompletedTaskCount() {
        completedTaskCount.incrementAndGet();
    }
    
    void onWorkerExit() {
        terminationLatch.countDown();
        
        if (terminationLatch.getCount() == 0) {
            state.set(PoolState.TERMINATED);
            System.out.println("[FixedThreadPool] All workers terminated. Pool is now TERMINATED.");
        }
    }
    
    /**
     * Builder Pattern for FixedThreadPool construction.
     * 
     * Provides fluent API for configuring:
     * - Pool size (required)
     * - Queue capacity (optional, default unbounded)
     * - Rejection policy (optional, default AbortPolicy)
     * - Thread factory (optional, default DefaultThreadFactory)
     */
    public static class Builder {
        
        private int poolSize;
        private int queueCapacity = 0;
        private RejectionPolicy rejectionPolicy = new AbortPolicy();
        private ThreadFactory threadFactory = new DefaultThreadFactory();
        
        public Builder poolSize(int poolSize) {
            if (poolSize <= 0) {
                throw new IllegalArgumentException("Pool size must be positive, got: " + poolSize);
            }
            this.poolSize = poolSize;
            return this;
        }
        
        public Builder queueCapacity(int queueCapacity) {
            if (queueCapacity < 0) {
                throw new IllegalArgumentException("Queue capacity cannot be negative, got: " + queueCapacity);
            }
            this.queueCapacity = queueCapacity;
            return this;
        }
        
        public Builder rejectionPolicy(RejectionPolicy rejectionPolicy) {
            if (rejectionPolicy == null) {
                throw new IllegalArgumentException("Rejection policy cannot be null");
            }
            this.rejectionPolicy = rejectionPolicy;
            return this;
        }
        
        public Builder threadFactory(ThreadFactory threadFactory) {
            if (threadFactory == null) {
                throw new IllegalArgumentException("Thread factory cannot be null");
            }
            this.threadFactory = threadFactory;
            return this;
        }
        
        public FixedThreadPool build() {
            if (poolSize <= 0) {
                throw new IllegalStateException("Pool size must be set before building");
            }
            return new FixedThreadPool(this);
        }
    }
}
