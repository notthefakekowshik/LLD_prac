package com.lldprep.foundations.creational.singleton.bad;

import java.io.Serializable;

/**
 * BAD: Singleton implementing Serializable without readResolve()
 * 
 * PROBLEM: Deserialization creates a new instance, breaking singleton contract.
 */
public class SerializableSingleton implements Serializable {
    private static final long serialVersionUID = 1L;
    private static SerializableSingleton instance;
    
    private SerializableSingleton() {}
    
    public static SerializableSingleton getInstance() {
        if (instance == null) {
            instance = new SerializableSingleton();
        }
        return instance;
    }
    
    // MISSING: readResolve() method!
    // Without this, deserialization creates new instance
}
