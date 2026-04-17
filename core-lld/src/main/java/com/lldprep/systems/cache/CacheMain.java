package com.lldprep.systems.cache;

import com.lldprep.systems.cache.policy.LFUEvictionPolicy;
import com.lldprep.systems.cache.policy.LRUEvictionPolicy;
import com.lldprep.systems.cache.storage.HashMapStorage;

/**
 * Demonstrates all functional requirements of the Cache:
 *   1. Basic put/get/remove
 *   2. LRU eviction when the cache is full
 *   3. TTL expiry (lazy on access + background cleanup)
 *   4. LFU eviction — Evolve step (swapped in with zero changes to Cache core)
 */
public class CacheMain {

    public static void main(String[] args) throws InterruptedException {
        demo1_BasicOperations();
        demo2_LRUEviction();
        demo3_TTLExpiry();
        demo4_LFUEviction();
    }

    // -------------------------------------------------------------------------

    private static void demo1_BasicOperations() {
        section("Demo 1: Basic put / get / remove");

        Cache<String, String> cache = new Cache<>(
                new HashMapStorage<>(5),
                new LRUEvictionPolicy<>(),
                60_000 // 60s TTL — won't expire during this demo
        );

        cache.put("city", "Bangalore");
        cache.put("lang", "Java");

        print("get city  →", cache.get("city"));   // Bangalore
        print("get lang  →", cache.get("lang"));    // Java
        print("get other →", cache.get("other"));   // null

        cache.remove("city");
        print("after remove, get city →", cache.get("city")); // null

        cache.shutdown();
    }

    private static void demo2_LRUEviction() {
        section("Demo 2: LRU Eviction (capacity = 3)");

        Cache<String, Integer> cache = new Cache<>(
                new HashMapStorage<>(3),
                new LRUEvictionPolicy<>(),
                60_000
        );

        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        // Access order (tail = most recent): A → B → C

        cache.get("A"); // A is now most recently used; order: B → C → A
        cache.get("B"); // order: C → A → B

        // Cache is full. Adding D should evict C (least recently used)
        cache.put("D", 4);

        print("get C →", cache.get("C")); // null — evicted
        print("get A →", cache.get("A")); // 1
        print("get B →", cache.get("B")); // 2
        print("get D →", cache.get("D")); // 4

        cache.shutdown();
    }

    private static void demo3_TTLExpiry() throws InterruptedException {
        section("Demo 3: TTL Expiry (TTL = 2 seconds)");

        Cache<String, String> cache = new Cache<>(
                new HashMapStorage<>(5),
                new LRUEvictionPolicy<>(),
                2_000 // 2s TTL
        );

        cache.put("token", "abc123");
        print("immediate get →", cache.get("token")); // abc123

        System.out.println("Sleeping 3 seconds...");
        Thread.sleep(3_000);

        // Lazy expiry check on access
        print("after 3s get →", cache.get("token")); // null — expired

        cache.shutdown();
    }

    private static void demo4_LFUEviction() {
        section("Demo 4: LFU Eviction — Evolve Step (capacity = 3)");
        System.out.println("Swapping LRUEvictionPolicy → LFUEvictionPolicy. Zero changes to Cache.\n");

        Cache<String, Integer> cache = new Cache<>(
                new HashMapStorage<>(3),
                new LFUEvictionPolicy<>(), // only this line changes
                60_000
        );

        cache.put("X", 10);
        cache.put("Y", 20);
        cache.put("Z", 30);

        // Access X once, Y five times, Z twice → X is least frequent
        cache.get("X");                                          // X: 1
        cache.get("Y"); cache.get("Y"); cache.get("Y");
        cache.get("Y"); cache.get("Y");                          // Y: 5
        cache.get("Z"); cache.get("Z");                          // Z: 2

        // Cache is full. Adding W should evict X (frequency = 1)
        cache.put("W", 40);

        print("get X →", cache.get("X")); // null — evicted (least frequent)
        print("get Y →", cache.get("Y")); // 20
        print("get Z →", cache.get("Z")); // 30
        print("get W →", cache.get("W")); // 40

        cache.shutdown();
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static void print(String label, Object value) {
        System.out.println("  " + label + " " + value);
    }
}
