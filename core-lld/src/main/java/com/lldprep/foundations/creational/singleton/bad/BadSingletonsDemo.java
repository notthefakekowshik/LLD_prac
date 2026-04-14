package com.lldprep.foundations.creational.singleton.bad;

/**
 * Client demonstrating all the Singleton anti-patterns and their problems.
 */
public class BadSingletonsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== BAD SINGLETON IMPLEMENTATIONS ===\n");
        
        System.out.println("1. BASIC LAZY SINGLETON:");
        System.out.println("   - NOT THREAD SAFE");
        System.out.println("   - Race condition can create multiple instances");
        System.out.println("   - Use: SynchronizedMethod or DoubleCheckedLocking\n");
        
        System.out.println("2. PUBLIC STATIC FIELD:");
        System.out.println("   - No lazy loading (eager instantiation)");
        System.out.println("   - No exception handling during construction");
        System.out.println("   - Reflection attacks work");
        System.out.println("   - Serialization issues\n");
        
        System.out.println("3. SYNCHRONIZED METHOD:");
        System.out.println("   - CORRECT but SLOW");
        System.out.println("   - Performance bottleneck on every getInstance() call");
        System.out.println("   - Use: DoubleCheckedLocking instead\n");
        
        System.out.println("4. BROKEN DOUBLE-CHECKED LOCKING:");
        System.out.println("   - Missing 'volatile' keyword (pre-Java 5)");
        System.out.println("   - Can return partially constructed objects");
        System.out.println("   - Use: Add volatile keyword\n");
        
        System.out.println("5. NO REFLECTION PROTECTION:");
        System.out.println("   - Reflection can call private constructor");
        System.out.println("   - setAccessible(true) bypasses access control");
        System.out.println("   - Use: Throw exception if instance exists\n");
        
        System.out.println("6. CLONEABLE SINGLETON:");
        System.out.println("   - clone() creates new instance");
        System.out.println("   - Use: Override clone() to throw exception\n");
        
        System.out.println("7. SERIALIZABLE WITHOUT readResolve():");
        System.out.println("   - Deserialization creates new instance");
        System.out.println("   - Use: Implement readResolve() method\n");
        
        System.out.println("=== ALL THESE PROBLEMS ARE FIXED IN 'good' PACKAGE ===");
    }
}
