package com.lldprep.foundations.creational.singleton.bad;

/**
 * BAD: Synchronized Method Singleton - CORRECT BUT SLOW
 * 
 * PROBLEM: Performance bottleneck. Every call to getInstance() acquires a lock,
 * even when instance already exists. Expensive in high-concurrency scenarios.
 */
public class SynchronizedMethodSingleton {
    private static SynchronizedMethodSingleton instance;
    
    private SynchronizedMethodSingleton() {}
    
    // Expensive: Locks on every call, even when instance exists
    public static synchronized SynchronizedMethodSingleton getInstance() {
        if (instance == null) {
            instance = new SynchronizedMethodSingleton();
        }
        return instance;
    }
    
    public void doSomething() {
        System.out.println("Doing something");
    }
}
