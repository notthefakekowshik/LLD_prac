package com.lldprep.foundations.creational.singleton.good;

/**
 * GOOD: Enum Singleton - The Most Robust Implementation
 * 
 * BEST FOR: When you need absolute guarantee of singleton contract.
 * Effective Java recommends this as the best approach.
 * 
 * PROS:
 * - Serialization handled automatically by JVM
 * - Reflection cannot break it (enum constructors are special)
 * - Thread-safe by JVM
 * - Guaranteed one instance per enum constant
 * - Prevents multiple instantiation attacks
 * 
 * CONS:
 * - Not lazy (loaded when enum is referenced)
 * - Less flexible (cannot extend other classes)
 * - Some consider it less intuitive
 */
public enum EnumSingleton {
    INSTANCE;
    
    // Can have fields and methods
    private int counter = 0;
    
    // Instance method
    public void doSomething() {
        counter++;
        System.out.println("EnumSingleton doing work, counter: " + counter);
    }
    
    public int getCounter() {
        return counter;
    }
}
