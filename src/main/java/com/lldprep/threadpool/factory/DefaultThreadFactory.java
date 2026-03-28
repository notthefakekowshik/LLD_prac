package com.lldprep.threadpool.factory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of ThreadFactory with configurable options.
 * 
 * Features:
 * - Sequential thread naming (pool-1-thread-1, pool-1-thread-2, ...)
 * - Configurable daemon status
 * - Configurable thread priority
 * - Optional UncaughtExceptionHandler
 */
public class DefaultThreadFactory implements ThreadFactory {
    
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemon;
    private final int priority;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    
    public DefaultThreadFactory() {
        this("pool-" + poolNumber.getAndIncrement(), false, Thread.NORM_PRIORITY, null);
    }
    
    public DefaultThreadFactory(String poolName) {
        this(poolName, false, Thread.NORM_PRIORITY, null);
    }
    
    public DefaultThreadFactory(String poolName, boolean daemon) {
        this(poolName, daemon, Thread.NORM_PRIORITY, null);
    }
    
    public DefaultThreadFactory(String poolName, boolean daemon, int priority, 
                                Thread.UncaughtExceptionHandler exceptionHandler) {
        this.namePrefix = poolName + "-thread-";
        this.daemon = daemon;
        this.priority = priority;
        this.exceptionHandler = exceptionHandler;
    }
    
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        thread.setDaemon(daemon);
        thread.setPriority(priority);
        
        if (exceptionHandler != null) {
            thread.setUncaughtExceptionHandler(exceptionHandler);
        } else {
            thread.setUncaughtExceptionHandler((t, e) -> {
                System.err.println("Uncaught exception in thread " + t.getName() + ": " + e.getMessage());
                e.printStackTrace();
            });
        }
        
        return thread;
    }
}
