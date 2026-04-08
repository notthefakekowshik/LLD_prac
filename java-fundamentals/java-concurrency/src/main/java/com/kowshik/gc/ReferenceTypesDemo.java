package com.kowshik.gc;

import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * ReferenceTypesDemo — Strong, Soft, Weak, Phantom References + Cleaner API
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What are the four reference types in Java?
 * A: Strong (default), Soft (collected before OOM), Weak (collected at next GC),
 *    Phantom (post-mortem notification only).
 *
 * Q: When would you use a SoftReference?
 * A: Memory-sensitive caches — the cached value stays alive as long as memory
 *    permits, but GC reclaims it under memory pressure (before throwing OOM).
 *
 * Q: What is WeakHashMap used for?
 * A: Canonicalization maps and caches where entries should disappear automatically
 *    when the key has no other strong references. Common use: metadata maps keyed
 *    by objects whose lifecycle you don't control.
 *
 * Q: What replaces finalize()?
 * A: java.lang.ref.Cleaner (Java 9+). Register a cleanup action that runs when
 *    the object becomes phantom-reachable. The action must NOT reference the object.
 */
public class ReferenceTypesDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: Strong Reference — default, never collected while reachable
    // ─────────────────────────────────────────────────────────────────────────
    static void strongReferenceDemo() throws InterruptedException {
        System.out.println("--- Part 1: Strong Reference ---");

        byte[] strongData = new byte[2 * 1024 * 1024]; // 2MB strong reference
        System.out.println("Strong ref: " + (strongData.length / 1024 / 1024) + "MB — will NOT be collected");
        System.gc();
        Thread.sleep(100);
        System.out.println("After GC: strong ref is still alive = " + (strongData != null));

        strongData = null; // Now eligible for collection
        System.out.println("After null: eligible for collection\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: SoftReference — collected only when JVM needs memory (before OOM)
    // Use case: memory-sensitive caches
    // ─────────────────────────────────────────────────────────────────────────
    static void softReferenceDemo() throws InterruptedException {
        System.out.println("--- Part 2: SoftReference (memory-sensitive cache) ---");

        SoftReference<byte[]> softRef = new SoftReference<>(new byte[5 * 1024 * 1024]); // 5MB

        System.gc();
        Thread.sleep(100);

        byte[] data = softRef.get();
        if (data != null) {
            System.out.println("SoftRef still alive after mild GC (JVM has plenty of memory)");
        } else {
            System.out.println("SoftRef was collected (JVM needed memory)");
        }

        // SoftReference usage pattern for a cache:
        System.out.println("\nCache pattern:");
        SoftReference<byte[]> cacheEntry = new SoftReference<>(loadExpensiveData());

        byte[] cached = cacheEntry.get();
        if (cached != null) {
            System.out.println("  Cache hit: using cached data (" + cached.length / 1024 + "KB)");
        } else {
            System.out.println("  Cache miss: GC reclaimed it — reload");
            cached = loadExpensiveData();
            cacheEntry = new SoftReference<>(cached);
        }
        System.out.println();
    }

    static byte[] loadExpensiveData() {
        return new byte[1024 * 1024]; // Simulate expensive computation/load
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 3: WeakReference — collected on the next GC after no strong ref remains
    // Use case: canonicalization maps, listener registries
    // ─────────────────────────────────────────────────────────────────────────
    static void weakReferenceDemo() throws InterruptedException {
        System.out.println("--- Part 3: WeakReference ---");

        Object obj = new Object();
        WeakReference<Object> weakRef = new WeakReference<>(obj);

        System.out.println("Before GC: weakRef.get() = " + (weakRef.get() != null ? "alive" : "null"));
        System.gc();
        Thread.sleep(100);
        System.out.println("After GC (strong ref still exists): weakRef.get() = " + (weakRef.get() != null ? "alive" : "null"));

        obj = null; // Remove strong reference
        System.gc();
        Thread.sleep(200);
        System.out.println("After obj=null + GC: weakRef.get() = " + (weakRef.get() != null ? "alive" : "null (collected!)"));

        // WeakHashMap demo
        System.out.println("\nWeakHashMap demo:");
        WeakHashMap<Object, String> weakMap = new WeakHashMap<>();
        Object key1 = new Object();
        Object key2 = new Object();
        weakMap.put(key1, "value1");
        weakMap.put(key2, "value2");
        System.out.println("  WeakHashMap size before: " + weakMap.size());

        key1 = null; // key1 has no more strong references
        System.gc();
        Thread.sleep(200);
        System.out.println("  WeakHashMap size after key1=null + GC: " + weakMap.size() + " (entry auto-removed)");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 4: PhantomReference + ReferenceQueue — post-mortem notification
    // PhantomReference.get() ALWAYS returns null — you cannot access the object
    // Used for: knowing WHEN an object is collected (for resource cleanup)
    // ─────────────────────────────────────────────────────────────────────────
    static void phantomReferenceDemo() throws InterruptedException {
        System.out.println("--- Part 4: PhantomReference + ReferenceQueue ---");

        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Object obj = new Object();
        PhantomReference<Object> phantomRef = new PhantomReference<>(obj, queue);

        System.out.println("phantomRef.get() always returns: " + phantomRef.get()); // Always null

        obj = null; // Remove strong reference — object becomes phantom-reachable
        System.gc();
        Thread.sleep(200);

        // Check if the reference was enqueued (object collected)
        java.lang.ref.Reference<?> enqueued = queue.poll();
        if (enqueued != null) {
            System.out.println("PhantomReference enqueued — object was collected!");
            System.out.println("This is the signal to clean up associated native resources.");
        } else {
            System.out.println("Not enqueued yet (GC timing is non-deterministic)");
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 5: Cleaner API (Java 9+) — modern replacement for finalize()
    // The cleanup action MUST NOT hold a reference to the object being cleaned
    // ─────────────────────────────────────────────────────────────────────────

    // CRITICAL: CleanupState must be a static nested class (or standalone).
    // It must NOT hold a reference to NativeResource — that would prevent GC.
    private static class CleanupState implements Runnable {
        private final int id;
        CleanupState(int id) { this.id = id; }

        @Override
        public void run() {
            System.out.println("  [Cleaner] Cleaning up native resource id=" + id);
            // In real code: close native handle, release OS resource, etc.
        }
    }

    static class NativeResource implements AutoCloseable {
        private static final Cleaner CLEANER = Cleaner.create();
        private final int resourceId;
        private final Cleaner.Cleanable cleanable;

        NativeResource(int id) {
            this.resourceId = id;
            // Register cleanup action (CleanupState does NOT reference 'this')
            this.cleanable = CLEANER.register(this, new CleanupState(id));
            System.out.println("  [NativeResource] created id=" + resourceId);
        }

        @Override
        public void close() {
            System.out.println("  [NativeResource] explicit close() id=" + resourceId);
            cleanable.clean(); // Explicit cleanup — preferred path
        }
    }

    static void cleanerDemo() throws InterruptedException {
        System.out.println("--- Part 5: Cleaner API (modern finalize() replacement) ---");

        // Path 1: explicit close via try-with-resources (preferred)
        try (NativeResource r1 = new NativeResource(1)) {
            System.out.println("  Using resource 1...");
        } // close() called here

        // Path 2: forgot to close — Cleaner runs as safety net when GC collects it
        NativeResource r2 = new NativeResource(2);
        r2 = null; // Forgot to close — Cleaner will eventually clean up
        System.gc();
        Thread.sleep(300);
        System.out.println("  Resource 2 cleanup handled by Cleaner (safety net)\n");
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Reference Types Demo ===\n");
        System.out.println("Note: GC is non-deterministic. Results may vary.");
        System.out.println("Run with -Xmx32m to see SoftReference collected under pressure.\n");

        strongReferenceDemo();
        softReferenceDemo();
        weakReferenceDemo();
        phantomReferenceDemo();
        cleanerDemo();

        System.out.println("=== Done ===");
    }
}
