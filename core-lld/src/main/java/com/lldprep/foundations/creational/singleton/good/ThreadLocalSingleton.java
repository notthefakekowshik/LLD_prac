package com.lldprep.foundations.creational.singleton.good;

/**
 * GOOD: Thread-Local Singleton
 * 
 * BEST FOR: When you need one instance per thread (not global singleton).
 * This is useful for thread-scoped singletons like database connections.
 * 
 * PROS:
 * - No synchronization needed
 * - Each thread has its own instance
 * - Thread-safe by design
 * - Good for thread-scoped resources
 * 
 * CONS:
 * - Not a true global singleton (one per thread, not one per JVM)
 * - Memory overhead (one instance per thread)
 */
public class ThreadLocalSingleton {
    // ThreadLocal ensures each thread gets its own instance
    private static final ThreadLocal<ThreadLocalSingleton> threadLocalInstance = 
        ThreadLocal.withInitial(ThreadLocalSingleton::new);
    
    private ThreadLocalSingleton() {
        System.out.println("Creating ThreadLocalSingleton for thread: " + 
            Thread.currentThread().getName());
    }
    
    public static ThreadLocalSingleton getInstance() {
        return threadLocalInstance.get();
    }
    
    // Clean up when thread ends
    public static void remove() {
        threadLocalInstance.remove();
    }
    
    public void doSomething() {
        System.out.println("ThreadLocalSingleton doing work on thread: " + 
            Thread.currentThread().getName());
    }
}
