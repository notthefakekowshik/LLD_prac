package com.kowshik.threads;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DoubleCheckedLockingDemo — Why `volatile` Matters in DCL
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What is Double-Checked Locking and why does it need `volatile`?
 * A: DCL avoids synchronization on every getInstance() call by checking `instance == null`
 *    twice — once without lock (fast path) and once with lock (slow path). The `volatile`
 *    keyword prevents instruction reordering: without it, the JVM may assign the
 *    `instance` reference BEFORE the constructor finishes (step 3 before step 2 in
 *    the object creation sequence). Another thread can then see a partially constructed
 *    object.
 *
 * Q: What are the 3 steps of object creation, and how can they reorder?
 * A: 1) Allocate heap memory for the object
 *    2) Initialize the object (run constructor, set fields)
 *    3) Assign `instance` reference to the allocated memory
 *    Without `volatile`, step 3 can happen BEFORE step 2 (store-store reordering).
 *    Thread B checks `instance != null` → true, reads the reference, gets uninitialized state.
 *
 * Q: How does `volatile` prevent this?
 * A: `volatile` inserts a StoreStore memory barrier between step 2 and step 3.
 *    The barrier guarantees: all stores before the barrier complete before any
 *    stores after the barrier. So step 2 (initialize) completes before step 3 (assign).
 *
 * Q: Does `synchronized` alone fix this? Why not?
 * A: `synchronized` only guarantees mutual exclusion — Thread B waits at the lock.
 *    But the FIRST null check (`if (instance == null)`) is OUTSIDE the synchronized block.
 *    Thread B can enter the fast path, see a non-null but partially constructed `instance`,
 *    and return it. `synchronized` doesn't insert memory barriers outside its scope.
 *
 * Q: What are the alternatives to DCL?
 * A: 1) Eager initialization (static final field) — simplest, JVM guarantees thread-safe
 *    2) Bill Pugh holder class — lazy init via classloader lock (recommended)
 *    3) Enum singleton — compiler-enforced, serialization-proof (most robust)
 *    4) DCL with volatile — when lazy init + high perf after first access is needed
 */
public class DoubleCheckedLockingDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           DOUBLE-CHECKED LOCKING DEEP DIVE DEMO              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        part1_WhyDCLExists();
        part2_BrokenDCLSimulation();
        part3_CorrectDCLWithVolatile();
        part4_AlternativesComparison();
        part5_MemoryBarrierUnderTheHood();
    }

    // ================================================================
    // PART 1: Why DCL exists — the performance story
    // ================================================================
    static void part1_WhyDCLExists() {
        System.out.println("┌─ PART 1: Why DCL Exists — The Performance Story ──────────────┐");

        // Synchronized-every-call singleton
        SynchronizedSingleton simpleLocked = new SynchronizedSingleton();
        long syncStart = System.nanoTime();
        for (int i = 0; i < 10_000_000; i++) {
            simpleLocked.get();
        }
        long syncNanos = (System.nanoTime() - syncStart);

        // DCL singleton (volatile)
        DCLSingleton dcl = new DCLSingleton();
        long dclStart = System.nanoTime();
        for (int i = 0; i < 10_000_000; i++) {
            dcl.get();
        }
        long dclNanos = (System.nanoTime() - dclStart);

        System.out.println("  10M get() calls:");
        System.out.println("    Synchronized every call: " + (syncNanos / 1_000_000) + "ms");
        System.out.println("    DCL (lock only first time): " + (dclNanos / 1_000_000) + "ms");
        System.out.println("  DCL avoids lock acquisition on every call after init.");
        System.out.println();
    }

    // ================================================================
    // PART 2: Broken DCL — demonstrate the reordering problem
    // ================================================================
    static void part2_BrokenDCLSimulation() throws InterruptedException {
        System.out.println("┌─ PART 2: Broken DCL — The Reordering Phantom ─────────────────┐");

        // This deliberately uses a class WITHOUT `volatile` on `instance`.
        // We stress it with many threads to show that DCL without volatile
        // can return partially constructed objects.
        AtomicInteger corruptedCount = new AtomicInteger(0);
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        BrokenSingleton.reset();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                BrokenSingleton obj = BrokenSingleton.getInstance();
                if (obj != null && !obj.isFullyInitialized()) {
                    corruptedCount.incrementAndGet();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("  Concurrent calls to BrokenSingleton (NO volatile):");
        System.out.println("    Corrupted instances seen: " + corruptedCount.get() + " / " + threads);
        if (corruptedCount.get() > 0) {
            System.out.println("    ✗ Partially constructed objects returned!");
            System.out.println("    Reason: Without volatile, reference assignment can reorder");
            System.out.println("            before constructor finishes.");
        } else {
            System.out.println("    No corruption this run — JVM reordering is non-deterministic.");
            System.out.println("    On x86 (TSO memory model), stores are not reordered, so this");
            System.out.println("    rarely breaks. But on ARM/PowerPC (weak memory model), it WILL.");
            System.out.println("    The Java spec does NOT guarantee TSO — don't rely on x86 luck.");
        }
        System.out.println();
    }

    // ================================================================
    // PART 3: Correct DCL — volatile ensures happens-before
    // ================================================================
    static void part3_CorrectDCLWithVolatile() throws InterruptedException {
        System.out.println("┌─ PART 3: Correct DCL — `volatile` Saves It ───────────────────┐");

        AtomicInteger corruptedCount = new AtomicInteger(0);
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        VolatileSingleton.reset();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                VolatileSingleton obj = VolatileSingleton.getInstance();
                if (obj != null && !obj.isFullyInitialized()) {
                    corruptedCount.incrementAndGet();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("  Concurrent calls to VolatileSingleton (WITH volatile):");
        System.out.println("    Corrupted instances seen: " + corruptedCount.get() + " / " + threads);
        System.out.println("    ✓ Zero corruption — volatile prevents reordering.");
        System.out.println("  volatile creates a StoreStore barrier after constructor.");
        System.out.println();
    }

    // ================================================================
    // PART 4: Singleton Alternatives — Bill Pugh, Enum
    // ================================================================
    static void part4_AlternativesComparison() {
        System.out.println("┌─ PART 4: Singleton Alternatives ──────────────────────────────┐");

        // 1. Eager — simplest, JVM guarantees thread-safety of static init
        EagerSingleton eager = EagerSingleton.INSTANCE;
        System.out.println("  1. Eager (static final field):");
        System.out.println("     - Thread-safe by JVM class-loading semantics");
        System.out.println("     - No volatile, no sync — zero runtime overhead");
        System.out.println("     - Downside: always created even if never used");
        System.out.println("     - Use when: init cost is trivial, or always needed");

        // 2. Bill Pugh — lazy + thread-safe + no sync cost
        BillPughSingleton billPugh = BillPughSingleton.getInstance();
        System.out.println("  2. Bill Pugh (holder class):");
        System.out.println("     - Lazy init via JVM classloader lock");
        System.out.println("     - No volatile, no sync blocks in code — zero overhead");
        System.out.println("     - ✓ Recommended for most lazy singleton use cases");
        System.out.println("     - Class Holder is loaded only when getInstance() is first called");

        // 3. Enum — compiler-enforced, reflection-proof, serializable
        EnumSingleton enumSing = EnumSingleton.INSTANCE;
        EnumSingleton enumSing2 = EnumSingleton.INSTANCE;
        System.out.println("  3. Enum singleton:");
        System.out.println("     - Compiler-enforced single instance (JLS §8.9)");
        System.out.println("     - Reflection attack: constructor.newInstance() throws");
        System.out.println("       IllegalArgumentException — JVM blocks it at the verifier level");
        System.out.println("     - Serialization: readResolve is automatic — no special handling");
        System.out.println("     - Use when: absolute safety (reflection/serialization) matters");
        System.out.println("     - Same instance? " + (enumSing == enumSing2 ? "✓ yes" : "✗ no"));
        System.out.println();
    }

    // ================================================================
    // PART 5: Memory Barrier — What Actually Happens at the CPU Level
    // ================================================================
    static void part5_MemoryBarrierUnderTheHood() {
        System.out.println("┌─ PART 5: Memory Barriers — Under the Hood ─────────────────────┐");
        System.out.println();
        System.out.println("  Object creation without volatile:");
        System.out.println("    Thread A:");
        System.out.println("      1. ptr = allocate(MySingleton.class)     // allocate memory");
        System.out.println("      2. ptr.field1 = 42                        // run constructor");
        System.out.println("      3. instance = ptr                         // publish reference");
        System.out.println("      ↑ Step 3 can reorder BEFORE step 2!");
        System.out.println();
        System.out.println("    Thread B:");
        System.out.println("      if (instance != null) {  // reads step 3 BEFORE step 2 done");
        System.out.println("        return instance;       // returns object with field1 = 0!");
        System.out.println("      }");
        System.out.println();
        System.out.println("  Object creation WITH volatile:");
        System.out.println("    Thread A:");
        System.out.println("      1. ptr = allocate(MySingleton.class)");
        System.out.println("      2. ptr.field1 = 42");
        System.out.println("      → StoreStore barrier (no store can cross this point)");
        System.out.println("      → StoreLoad barrier  (subsequent reads see stores before)");
        System.out.println("      3. instance = ptr");
        System.out.println();
        System.out.println("    Thread B: (volatile read = LoadLoad + LoadStore barrier)");
        System.out.println("      if (instance != null) {    // volatile read — sees all stores before");
        System.out.println("        return instance;         // field1 is guaranteed to be 42");
        System.out.println("      }");
        System.out.println();
        System.out.println("  On x86: StoreStore is essentially free (TSO model)");
        System.out.println("  StoreLoad = mfence/lock addl instruction → ~30-90 cycles");
        System.out.println("  On ARM: Both barriers emit explicit dmb (data memory barrier)");
        System.out.println("  This is why volatile reads cost ~1 cache miss + potential pipeline flush.");
        System.out.println();
    }

    // ── Helper classes for PART 1 ──

    static class SynchronizedSingleton {
        private Object instance;
        synchronized Object get() {
            if (instance == null) { instance = new Object(); }
            return instance;
        }
    }

    static class DCLSingleton {
        private volatile Object instance;
        Object get() {
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) { instance = new Object(); }
                }
            }
            return instance;
        }
    }

    // ── Helper classes for PART 2 & 3 ──

    /**
     * Deliberately BROKEN: no `volatile` on `instance`.
     * On weak memory models, callers may see a partially constructed object
     * (array not yet filled, but reference already published).
     */
    static class BrokenSingleton {
        private static BrokenSingleton instance;  // NO volatile — deliberately broken
        private final int[] data;
        private boolean initialized;

        private BrokenSingleton() {
            data = new int[100];
            for (int i = 0; i < data.length; i++) {
                data[i] = i;
            }
            initialized = true;
        }

        static BrokenSingleton getInstance() {
            if (instance == null) {               // 1st check — UNSAFE without volatile
                synchronized (BrokenSingleton.class) {
                    if (instance == null) {       // 2nd check
                        instance = new BrokenSingleton();
                    }
                }
            }
            return instance;
        }

        boolean isFullyInitialized() {
            if (!initialized) { return false; }
            for (int i = 0; i < data.length; i++) {
                if (data[i] != i) { return false; }
            }
            return true;
        }

        static void reset() { instance = null; }
    }

    /**
     * CORRECT: `volatile` on `instance` prevents instruction reordering.
     */
    static class VolatileSingleton {
        private static volatile VolatileSingleton instance;  // volatile — correct
        private final int[] data;
        private boolean initialized;

        private VolatileSingleton() {
            data = new int[100];
            for (int i = 0; i < data.length; i++) {
                data[i] = i;
            }
            initialized = true;
        }

        static VolatileSingleton getInstance() {
            if (instance == null) {
                synchronized (VolatileSingleton.class) {
                    if (instance == null) {
                        instance = new VolatileSingleton();
                    }
                }
            }
            return instance;
        }

        boolean isFullyInitialized() {
            if (!initialized) { return false; }
            for (int i = 0; i < data.length; i++) {
                if (data[i] != i) { return false; }
            }
            return true;
        }

        static void reset() { instance = null; }
    }

    // ── Helper classes for PART 4 ──

    static class EagerSingleton {
        static final EagerSingleton INSTANCE = new EagerSingleton();
        private EagerSingleton() {}
    }

    static class BillPughSingleton {
        private BillPughSingleton() {}
        private static class Holder {
            static final BillPughSingleton INSTANCE = new BillPughSingleton();
        }
        static BillPughSingleton getInstance() {
            return Holder.INSTANCE;  // Class Holder loaded on first access
        }
    }

    enum EnumSingleton {
        INSTANCE;
        private final int[] data = new int[100];
        EnumSingleton() {
            for (int i = 0; i < data.length; i++) { data[i] = i; }
        }
    }
}
