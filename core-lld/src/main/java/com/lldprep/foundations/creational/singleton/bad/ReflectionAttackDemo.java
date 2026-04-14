package com.lldprep.foundations.creational.singleton.bad;

import java.lang.reflect.Constructor;

/**
 * BAD: No protection against Reflection attacks
 * 
 * PROBLEM: Reflection can bypass private constructors completely.
 * Any Singleton without reflection protection can be broken.
 */
public class ReflectionAttackDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== DEMONSTRATING REFLECTION ATTACK ===\n");
        
        // Get the singleton instance normally
        BasicLazySingleton instance1 = BasicLazySingleton.getInstance();
        System.out.println("Instance 1 hash: " + instance1.hashCode());
        
        // Use reflection to create another instance
        Constructor<BasicLazySingleton> constructor = BasicLazySingleton.class.getDeclaredConstructor();
        constructor.setAccessible(true);  // Bypass private!
        BasicLazySingleton instance2 = constructor.newInstance();
        
        System.out.println("Instance 2 hash: " + instance2.hashCode());
        System.out.println("Are they same? " + (instance1 == instance2));
        // Output: false! Singleton broken!
        
        System.out.println("\n=== REFLECTION DESTROYED SINGLETON ===");
    }
}
