package com.lldprep.foundations.creational.singleton.good;

/**
 * GOOD: Bill Pugh Singleton (Static Inner Class)
 * 
 * BEST FOR: Most cases - this is the recommended approach!
 * 
 * PROS:
 * - Lazy initialization (class loaded only when getInstance called)
 * - Thread-safe by JVM class loading mechanism
 * - No synchronization overhead
 * - Clean and readable code
 * - Most efficient approach
 * 
 * CONS:
 * - None significant
 */
public class BillPughSingleton {
    
    private BillPughSingleton() {
        // Prevent reflection attacks
        if (SingletonHolder.INSTANCE != null) {
            throw new IllegalStateException("Instance already exists!");
        }
    }
    
    // Static inner class - loaded only when getInstance() is called
    private static class SingletonHolder {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }
    
    public static BillPughSingleton getInstance() {
        return SingletonHolder.INSTANCE;
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
        System.out.println("BillPughSingleton doing work (RECOMMENDED APPROACH)");
    }
}
