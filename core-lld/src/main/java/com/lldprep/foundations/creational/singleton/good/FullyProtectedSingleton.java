package com.lldprep.foundations.creational.singleton.good;

import java.io.Serializable;

/**
 * GOOD: Fully Protected Singleton (All Defenses)
 * 
 * Shows all protections needed for a robust singleton:
 * - Thread safety (Bill Pugh approach)
 * - Reflection protection
 * - Serialization protection
 * - Clone protection
 */
public class FullyProtectedSingleton implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    
    private FullyProtectedSingleton() {
        // DEFENSE 1: Prevent reflection attacks
        if (SingletonHolder.INSTANCE != null) {
            throw new IllegalStateException("Instance already exists! Use getInstance()");
        }
        
        // DEFENSE 2: Optional - prevent even first reflective access
        // StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // if (stackTrace.length > 2 && stackTrace[2].getClassName().equals(
        //         java.lang.reflect.Constructor.class.getName())) {
        //     throw new IllegalStateException("Reflection not allowed");
        // }
    }
    
    // Bill Pugh approach for thread-safe lazy initialization
    private static class SingletonHolder {
        private static final FullyProtectedSingleton INSTANCE = new FullyProtectedSingleton();
    }
    
    public static FullyProtectedSingleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    // DEFENSE 3: Prevent cloning
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cloning singleton is not allowed");
    }
    
    // DEFENSE 4: Prevent deserialization from creating new instance
    protected Object readResolve() {
        // Return the existing instance instead of new one
        return getInstance();
    }
    
    public void doSomething() {
        System.out.println("FullyProtectedSingleton doing work securely");
    }
}
