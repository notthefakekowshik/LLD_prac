package com.lldprep.foundations.creational.singleton.bad;

/**
 * BAD: Singleton implementing Cloneable without protection
 * 
 * PROBLEM: clone() method can create copy of singleton, breaking uniqueness.
 */
public class CloneableSingleton implements Cloneable {
    private static CloneableSingleton instance;
    
    private CloneableSingleton() {}
    
    public static CloneableSingleton getInstance() {
        if (instance == null) {
            instance = new CloneableSingleton();
        }
        return instance;
    }
    
    // DANGER: clone() creates new instance!
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();  // Creates new object - breaks singleton!
    }
}
