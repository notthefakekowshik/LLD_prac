package com.lldprep.foundations.creational.singleton.good;

/**
 * GOOD: Eager Initialization Singleton
 * 
 * BEST FOR: When singleton is always needed, or creation is lightweight.
 * 
 * PROS:
 * - Thread-safe by JVM class loading
 * - Simple and easy to understand
 * - No synchronization overhead
 * 
 * CONS:
 * - Instance created even if never used
 * - Cannot handle exceptions in constructor elegantly
 */
public class EagerSingleton {
    // Created when class is loaded - guaranteed thread-safe by JVM
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    
    private EagerSingleton() {
        // Prevent reflection attacks
        if (INSTANCE != null) {
            throw new IllegalStateException("Instance already exists!");
        }
    }
    
    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
    
    // Protect against cloning
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton cannot be cloned");
    }
    
    // Protect against serialization
    protected Object readResolve() {
        return INSTANCE;
    }
    
    public void doSomething() {
        System.out.println("EagerSingleton doing work");
    }
}
