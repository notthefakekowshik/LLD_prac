package com.lldprep.foundations.creational.singleton.bad;

/**
 * BAD: Basic Lazy Singleton - NOT THREAD SAFE
 * 
 * PROBLEM: Race condition in multi-threaded environment.
 * Two threads can simultaneously pass the null check and create two instances.
 */
public class BasicLazySingleton {
    private static BasicLazySingleton instance;
    
    private BasicLazySingleton() {
        System.out.println("BasicLazySingleton instance created");
    }
    
    // DANGER: Not synchronized - thread unsafe!
    public static BasicLazySingleton getInstance() {
        if (instance == null) {  // Thread A and B both see null here
            instance = new BasicLazySingleton();  // Both create instances!
        }
        return instance;
    }
    
    public void doSomething() {
        System.out.println("Doing something with hash: " + this.hashCode());
    }
}
