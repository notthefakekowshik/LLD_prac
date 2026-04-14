package com.lldprep.foundations.creational.singleton.bad;

/**
 * BAD: Broken Double-Checked Locking (Pre-Java 5)
 * 
 * PROBLEM: Before Java 5 memory model, this could return partially constructed object.
 * The 'instance' reference could be assigned before the constructor completes
 * due to instruction reordering by compiler/JVM.
 * 
 * In Java 5+, this is fixed with 'volatile' keyword (shown in good package).
 */
public class BrokenDoubleCheckedLocking {
    // MISSING: volatile keyword!
    private static BrokenDoubleCheckedLocking instance;
    
    private BrokenDoubleCheckedLocking() {}
    
    // BROKEN: Without volatile, another thread might see partially constructed object
    public static BrokenDoubleCheckedLocking getInstance() {
        if (instance == null) {                      // First check (no locking)
            synchronized (BrokenDoubleCheckedLocking.class) {
                if (instance == null) {              // Second check (with locking)
                    instance = new BrokenDoubleCheckedLocking(); // NOT ATOMIC!
                    // Object creation: 1) Allocate memory, 2) Initialize, 3) Assign reference
                    // Without volatile, step 3 might happen before step 2!
                }
            }
        }
        return instance;
    }
}
