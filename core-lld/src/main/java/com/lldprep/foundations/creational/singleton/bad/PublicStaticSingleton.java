package com.lldprep.foundations.creational.singleton.bad;

/**
 * BAD: Public Static Field Singleton
 * 
 * PROBLEMS:
 * 1. Instance created eagerly - no lazy loading
 * 2. No control over instantiation timing
 * 3. Cannot handle exceptions during construction gracefully
 * 4. Reflection can still create new instances (no protection)
 * 5. No serialization safety
 */
public class PublicStaticSingleton {
    // Public field - anyone can access, but also exposes implementation detail
    public static final PublicStaticSingleton INSTANCE = new PublicStaticSingleton();
    
    private PublicStaticSingleton() {
        System.out.println("PublicStaticSingleton instance created");
    }
    
    public void doSomething() {
        System.out.println("Doing something");
    }
}
