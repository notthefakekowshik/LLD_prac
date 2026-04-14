package com.lldprep.foundations.creational.singleton.good;

/**
 * GOOD: Double-Checked Locking Singleton (Java 5+)
 * 
 * BEST FOR: When lazy initialization is needed with high performance.
 * 
 * PROS:
 * - Lazy initialization
 * - Thread-safe
 * - Good performance (synchronized only on first access)
 * 
 * CONS:
 * - More complex code
 * - Requires 'volatile' keyword (Java 5+)
 */
public class DoubleCheckedLockingSingleton {
    // CRITICAL: volatile prevents instruction reordering
    private static volatile DoubleCheckedLockingSingleton instance;
    
    private DoubleCheckedLockingSingleton() {
        // Prevent reflection attacks
        if (instance != null) {
            throw new IllegalStateException("Instance already exists!");
        }
    }
    
    public static DoubleCheckedLockingSingleton getInstance() {
        // First check without locking (performance)
        if (instance == null) {
            synchronized (DoubleCheckedLockingSingleton.class) {
                // Second check with locking (correctness)
                if (instance == null) {
                    instance = new DoubleCheckedLockingSingleton();
                }
            }
        }
        return instance;
    }
    
    // Protect against cloning
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton cannot be cloned");
    }
    
    // Protect against serialization
    protected Object readResolve() {
        return getInstance();
    }
    
    public void doSomething() {
        System.out.println("DoubleCheckedLockingSingleton doing work");
    }
}
