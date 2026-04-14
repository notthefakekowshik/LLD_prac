package com.lldprep.foundations.creational.singleton.good;

import java.io.*;
import java.lang.reflect.Constructor;

/**
 * SINGLETON PATTERN
 * =================
 *
 * WHY IT EXISTS:
 * Ensures a class has exactly ONE instance throughout application lifecycle
 * and provides global access to it. Critical for shared resources like
 * database connections, thread pools, caches, and configuration managers.
 *
 * PROBLEMS IT SOLVES:
 * - Multiple instances of resource-heavy objects (wasteful, inconsistent state)
 * - Global state access without passing references everywhere
 * - Race conditions when multiple threads try to create instance
 * - Serialization/deserialization breaking singleton contract
 * - Reflection attacks creating additional instances
 *
 * WHEN TO USE:
 * - Database connection pools (one pool per app)
 * - Thread pools and executor services
 * - Caches and registries (one shared cache)
 * - Configuration managers (single source of truth)
 * - Hardware interface access (printer, file system)
 * - Logging services (one logger instance)
 *
 * WHEN NOT TO USE:
 * - Unit testing becomes harder (global state)
 * - Distributed systems (one instance per JVM, not cluster)
 * - Tight coupling to singleton class (hard to mock)
 *
 * THIS DEMO SHOWS:
 * 7 different implementations with trade-offs, plus protection against
 * reflection attacks and serialization issues.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Singleton_pattern">Singleton Pattern</a>
 */
public class GoodSingletonsDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== GOOD SINGLETON IMPLEMENTATIONS ===\n");
        
        // 1. Eager Singleton
        System.out.println("1. EAGER SINGLETON:");
        System.out.println("   - Thread-safe by class loading");
        System.out.println("   - Simple, no synchronization overhead");
        EagerSingleton eager1 = EagerSingleton.getInstance();
        EagerSingleton eager2 = EagerSingleton.getInstance();
        System.out.println("   Same instance? " + (eager1 == eager2));
        eager1.doSomething();
        System.out.println();
        
        // 2. Double-Checked Locking
        System.out.println("2. DOUBLE-CHECKED LOCKING:");
        System.out.println("   - Lazy + Thread-safe + Good performance");
        System.out.println("   - Requires 'volatile' keyword");
        DoubleCheckedLockingSingleton dcl1 = DoubleCheckedLockingSingleton.getInstance();
        DoubleCheckedLockingSingleton dcl2 = DoubleCheckedLockingSingleton.getInstance();
        System.out.println("   Same instance? " + (dcl1 == dcl2));
        dcl1.doSomething();
        System.out.println();
        
        // 3. Bill Pugh (RECOMMENDED)
        System.out.println("3. BILL PUGH SINGLETON (RECOMMENDED):");
        System.out.println("   - Lazy + Thread-safe + No overhead");
        System.out.println("   - Uses static inner class");
        BillPughSingleton bill1 = BillPughSingleton.getInstance();
        BillPughSingleton bill2 = BillPughSingleton.getInstance();
        System.out.println("   Same instance? " + (bill1 == bill2));
        bill1.doSomething();
        System.out.println();
        
        // 4. Enum Singleton (Most Robust)
        System.out.println("4. ENUM SINGLETON (MOST ROBUST):");
        System.out.println("   - Handles serialization automatically");
        System.out.println("   - Reflection-proof");
        EnumSingleton enum1 = EnumSingleton.INSTANCE;
        EnumSingleton enum2 = EnumSingleton.INSTANCE;
        System.out.println("   Same instance? " + (enum1 == enum2));
        enum1.doSomething();
        enum2.doSomething();
        System.out.println("   Counter: " + enum1.getCounter());
        System.out.println();
        
        // 5. Thread-Local Singleton
        System.out.println("5. THREAD-LOCAL SINGLETON:");
        System.out.println("   - One instance per thread");
        System.out.println("   - Good for thread-scoped resources");
        ThreadLocalSingleton tlocal1 = ThreadLocalSingleton.getInstance();
        tlocal1.doSomething();
        System.out.println();
        
        // 6. Demonstrate protections
        System.out.println("6. PROTECTION TESTS (FullyProtectedSingleton):");
        testProtections();
        System.out.println();
        
        // 7. Registry Singleton
        System.out.println("7. REGISTRY SINGLETON:");
        RegistrySingleton registry = RegistrySingleton.getInstance();
        registry.register("db", RegistrySingleton.DatabaseConnection.getInstance());
        registry.register("cache", RegistrySingleton.CacheManager.getInstance());
        
        RegistrySingleton.DatabaseConnection db = registry.get("db");
        RegistrySingleton.CacheManager cache = registry.get("cache");
        db.query();
        cache.clear();
        System.out.println();
        
        System.out.println("=== ALL SINGLETON PATTERNS WORKING CORRECTLY ===");
    }
    
    private static void testProtections() throws Exception {
        FullyProtectedSingleton instance1 = FullyProtectedSingleton.getInstance();
        System.out.println("   Original instance hash: " + instance1.hashCode());
        
        // Test 1: Reflection Attack (should fail)
        try {
            Constructor<FullyProtectedSingleton> constructor = 
                FullyProtectedSingleton.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            FullyProtectedSingleton instance2 = constructor.newInstance();
            System.out.println("   REFLECTION FAILED: Created instance with hash: " + instance2.hashCode());
        } catch (Exception e) {
            System.out.println("   REFLECTION PROTECTION WORKS: " + e.getMessage());
        }
        
        // Test 2: Serialization Safety
        try {
            // Serialize
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(instance1);
            oos.close();
            
            // Deserialize
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            FullyProtectedSingleton deserialized = (FullyProtectedSingleton) ois.readObject();
            ois.close();
            
            System.out.println("   Serialized instance hash: " + instance1.hashCode());
            System.out.println("   Deserialized instance hash: " + deserialized.hashCode());
            System.out.println("   SERIALIZATION PROTECTION WORKS: " + (instance1 == deserialized));
        } catch (Exception e) {
            System.out.println("   Serialization test error: " + e.getMessage());
        }
    }
}
