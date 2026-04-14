package com.lldprep.foundations.creational.singleton.good;

import java.util.HashMap;
import java.util.Map;

/**
 * GOOD: Singleton Registry Pattern
 * 
 * BEST FOR: When you need multiple singletons of similar types,
 * or runtime-configurable singleton instances.
 * 
 * PROS:
 * - Flexible - can register/retrieve multiple singleton types
 * - Runtime configuration
 * - Useful for plugin architectures
 * - Lazy initialization per type
 * 
 * CONS:
 * - More complex
 * - Registry itself needs to be singleton
 * - Potential memory leak if not managed
 */
public class RegistrySingleton {
    private static final RegistrySingleton REGISTRY = new RegistrySingleton();
    private final Map<String, Object> instances = new HashMap<>();
    
    private RegistrySingleton() {}
    
    public static RegistrySingleton getInstance() {
        return REGISTRY;
    }
    
    // Register a singleton instance
    public synchronized void register(String name, Object instance) {
        if (!instances.containsKey(name)) {
            instances.put(name, instance);
        }
    }
    
    // Get a registered singleton
    @SuppressWarnings("unchecked")
    public synchronized <T> T get(String name) {
        return (T) instances.get(name);
    }
    
    // Check if singleton exists
    public synchronized boolean contains(String name) {
        return instances.containsKey(name);
    }
    
    // Example: Register and retrieve singletons
    public static class DatabaseConnection {
        private static final DatabaseConnection INSTANCE = new DatabaseConnection();
        private DatabaseConnection() {}
        public static DatabaseConnection getInstance() { return INSTANCE; }
        public void query() { System.out.println("Database query executed"); }
    }
    
    public static class CacheManager {
        private static final CacheManager INSTANCE = new CacheManager();
        private CacheManager() {}
        public static CacheManager getInstance() { return INSTANCE; }
        public void clear() { System.out.println("Cache cleared"); }
    }
}
