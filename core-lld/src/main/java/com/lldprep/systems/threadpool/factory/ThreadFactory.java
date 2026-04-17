package com.lldprep.threadpool.factory;

/**
 * Factory Pattern: Interface for creating and configuring threads.
 * 
 * Allows customization of:
 * - Thread naming strategy
 * - Daemon status
 * - Thread priority
 * - UncaughtExceptionHandler
 */
public interface ThreadFactory {
    
    /**
     * Creates a new thread for the given runnable.
     * 
     * @param r The runnable to execute in the thread
     * @return A new, unstarted thread
     */
    Thread newThread(Runnable r);
}
