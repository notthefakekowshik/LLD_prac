package com.lldprep.systems.threadpool;

import java.util.concurrent.BlockingQueue;

/**
 * Worker thread that continuously polls tasks from the queue and executes them.
 * 
 * Lifecycle:
 * 1. RUNNING: Poll tasks from queue and execute
 * 2. SHUTDOWN: Finish queued tasks, then exit
 * 3. TERMINATED: Exit immediately (via interrupt)
 * 
 * Thread Safety:
 * - Reads state from FixedThreadPool (volatile via AtomicReference)
 * - Updates metrics via thread-safe methods
 * - Handles InterruptedException for graceful shutdown
 * 
 * Exception Handling:
 * - Task exceptions are caught and logged, worker continues
 * - InterruptedException during take() triggers shutdown check
 * - Fatal errors are logged via UncaughtExceptionHandler
 */
public class WorkerThread implements Runnable {
    
    private final FixedThreadPool pool;
    private final BlockingQueue<Runnable> taskQueue;
    private volatile Thread currentThread;
    
    public WorkerThread(FixedThreadPool pool) {
        this.pool = pool;
        this.taskQueue = pool.getTaskQueue();
    }
    
    @Override
    public void run() {
        currentThread = Thread.currentThread();
        
        try {
            while (true) {
                PoolState state = pool.getState();
                
                if (state == PoolState.SHUTDOWN && taskQueue.isEmpty()) {
                    break;
                }
                
                if (state == PoolState.TERMINATED) {
                    break;
                }
                
                try {
                    Runnable task = taskQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    
                    if (task != null) {
                        executeTask(task);
                    }
                    
                } catch (InterruptedException e) {
                    if (pool.getState() == PoolState.SHUTDOWN || pool.getState() == PoolState.TERMINATED) {
                        System.out.println("[" + Thread.currentThread().getName() + "] Interrupted during shutdown.");
                        break;
                    }
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            pool.onWorkerExit();
            System.out.println("[" + Thread.currentThread().getName() + "] Worker thread exiting.");
        }
    }
    
    private void executeTask(Runnable task) {
        pool.incrementActiveCount();
        
        try {
            beforeExecute(task);
            task.run();
            afterExecute(task, null);
        } catch (Throwable t) {
            afterExecute(task, t);
            System.err.println("[" + Thread.currentThread().getName() + "] Task execution failed: " + t.getMessage());
            t.printStackTrace();
        } finally {
            pool.decrementActiveCount();
            pool.incrementCompletedTaskCount();
        }
    }
    
    protected void beforeExecute(Runnable task) {
    }
    
    protected void afterExecute(Runnable task, Throwable t) {
    }
    
    public void interrupt() {
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }
}
