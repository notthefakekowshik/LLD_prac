package com.kowshik.gc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * MemoryLeakPatternsDemo — The 5 Most Common Java Memory Leak Patterns
 *
 * INTERVIEW PREP:
 * ==============
 * Q: Can you have a memory leak in Java if GC manages memory?
 * A: Yes. GC collects UNREACHABLE objects. Memory leaks happen when objects are
 *    REACHABLE but logically useless — held in a collection or field that's never
 *    cleared. GC cannot know they're logically dead.
 *
 * Q: Name the most common memory leak patterns.
 * A: (1) Static collection never cleared, (2) unclosed resources,
 *    (3) listeners not deregistered, (4) ThreadLocal not removed in thread pools,
 *    (5) inner class holding outer class reference.
 *
 * Q: How do you diagnose a memory leak in production?
 * A: Heap dump (`jmap -dump` or `-XX:+HeapDumpOnOutOfMemoryError`), analyze with
 *    Eclipse MAT or VisualVM — look for the largest object graph roots.
 */
public class MemoryLeakPatternsDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern 1: Static collection that grows without bound
    // THE most common leak. Static field is a GC root — objects never collected.
    // ─────────────────────────────────────────────────────────────────────────
    static class Pattern1_StaticCollection {
        // LEAK: static list accumulates request data forever
        private static final List<byte[]> requestCache = new ArrayList<>();

        static void handleRequest(byte[] data) {
            requestCache.add(data); // Added but never removed!
        }

        static void fixedHandleRequest(Map<String, byte[]> boundedCache, String key, byte[] data) {
            // FIX: Use a bounded structure (LRU cache, ConcurrentHashMap with eviction)
            if (boundedCache.size() < 1000) {
                boundedCache.put(key, data);
            }
        }
    }

    static void pattern1Demo() {
        System.out.println("--- Pattern 1: Static Collection (simulated) ---");
        System.out.println("  BAD:  static List grows forever — GC root holds all entries");
        System.out.println("  FIX:  use bounded cache (e.g., LinkedHashMap with removeEldestEntry)");
        System.out.println("  FIX:  use WeakHashMap if entries should expire with their keys");
        System.out.println("  FIX:  use Caffeine/Guava cache with size/time eviction\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern 2: Event listener / callback not deregistered
    // ─────────────────────────────────────────────────────────────────────────
    interface EventListener {
        void onEvent(String event);
    }

    static class EventBus {
        private final List<EventListener> listeners = new ArrayList<>();

        void subscribe(EventListener listener) {
            listeners.add(listener);  // Strong reference to listener
        }

        void unsubscribe(EventListener listener) {
            listeners.remove(listener); // Must call this to free memory
        }

        void publish(String event) {
            listeners.forEach(l -> l.onEvent(event));
        }
    }

    static class ShortLivedSubscriber implements EventListener {
        private final byte[] data = new byte[1024 * 1024]; // 1MB per subscriber
        private final String name;

        ShortLivedSubscriber(String name) { this.name = name; }

        @Override
        public void onEvent(String event) {
            System.out.printf("  [%s] received: %s%n", name, event);
        }
    }

    static void pattern2Demo() {
        System.out.println("--- Pattern 2: Event Listener Not Deregistered ---");
        EventBus bus = new EventBus();

        // LEAK: subscriber goes out of scope but bus holds strong reference
        for (int i = 0; i < 5; i++) {
            ShortLivedSubscriber sub = new ShortLivedSubscriber("sub-" + i);
            bus.subscribe(sub);
            // sub goes out of scope here — but bus still holds it!
            // 5 subscribers × 1MB = 5MB can never be collected
        }
        System.out.println("  BAD:  " + countApprox(bus) + " subscribers still held by bus (never unsubscribed)");

        // FIX 1: Explicitly unsubscribe
        EventBus fixedBus = new EventBus();
        ShortLivedSubscriber sub = new ShortLivedSubscriber("managed");
        fixedBus.subscribe(sub);
        fixedBus.publish("event");
        fixedBus.unsubscribe(sub); // Clean up!
        System.out.println("  FIX1: unsubscribe() called — listener released");

        // FIX 2: Use WeakReference-based listener list
        System.out.println("  FIX2: WeakReference<EventListener> in list — auto-removed when listener GC'd\n");
    }

    static int countApprox(EventBus bus) {
        // Using reflection would be needed in real code — approximate for demo
        return 5;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern 3: ThreadLocal not removed in thread pool
    // Thread pool threads are reused — ThreadLocalMap entries accumulate
    // ─────────────────────────────────────────────────────────────────────────
    static final ThreadLocal<byte[]> requestContext = new ThreadLocal<>();

    static void pattern3Demo() {
        System.out.println("--- Pattern 3: ThreadLocal Not Removed in Thread Pool ---");

        // LEAK: thread pool thread gets requestContext set, never removed
        // On next request, thread reuses the old value OR memory accumulates
        Runnable leakyTask = () -> {
            requestContext.set(new byte[512 * 1024]); // 512KB per request
            // ... do work ...
            // FORGOT: requestContext.remove() → value stays on thread forever
        };

        // FIX: always remove in finally
        Runnable fixedTask = () -> {
            requestContext.set(new byte[512 * 1024]);
            try {
                // ... do work ...
            } finally {
                requestContext.remove(); // ALWAYS remove in finally
            }
        };

        System.out.println("  BAD:  ThreadLocal.set() without remove() in thread pool");
        System.out.println("        Thread holds 512KB forever (thread lives as long as pool)");
        System.out.println("  FIX:  always call ThreadLocal.remove() in finally block");

        // Verify fix:
        fixedTask.run();
        System.out.println("  After fixedTask: requestContext.get() = " + requestContext.get() + " (null = cleaned up)\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern 4: Non-static inner class holding outer class reference
    // ─────────────────────────────────────────────────────────────────────────
    static void pattern4Demo() {
        System.out.println("--- Pattern 4: Inner Class Holding Outer Class Reference ---");
        System.out.println("  BAD:  non-static inner class holds implicit 'OuterClass.this' reference");
        System.out.println("        OuterClass cannot be GC'd as long as InnerClass is reachable");
        System.out.println();

        // Classic Android example (applies to any framework):
        System.out.println("  Example: Activity.new AsyncTask() { ... }");
        System.out.println("    AsyncTask (non-static inner) holds reference to Activity");
        System.out.println("    Activity rotated/destroyed but AsyncTask still running");
        System.out.println("    Activity can't be GC'd → memory leak");
        System.out.println();
        System.out.println("  FIX:  use static nested class + WeakReference to outer if needed:");
        System.out.println("        static class MyTask extends AsyncTask {");
        System.out.println("            WeakReference<Activity> actRef;");
        System.out.println("            void onPostExecute() {");
        System.out.println("                Activity act = actRef.get(); // null if GC'd");
        System.out.println("                if (act != null) act.updateUI();");
        System.out.println("            }");
        System.out.println("        }\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pattern 5: equals/hashCode not overridden — HashMap grows without bound
    // Objects that should be equal are treated as distinct keys
    // ─────────────────────────────────────────────────────────────────────────
    static class BadKey {
        private final int id;
        BadKey(int id) { this.id = id; }
        // Missing equals() and hashCode()!
        // Every new BadKey(1) is a different key in the HashMap
    }

    static class GoodKey {
        private final int id;
        GoodKey(int id) { this.id = id; }

        @Override public boolean equals(Object o) {
            return o instanceof GoodKey && ((GoodKey) o).id == this.id;
        }
        @Override public int hashCode() { return Integer.hashCode(id); }
    }

    static void pattern5Demo() {
        System.out.println("--- Pattern 5: Missing equals/hashCode — Map Grows Without Bound ---");

        Map<BadKey, String> leakyMap = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            leakyMap.put(new BadKey(1), "value"); // Each new BadKey(1) is a DIFFERENT key!
        }
        System.out.println("  BAD:  Map with BadKey(no equals/hashCode): size=" + leakyMap.size() + " (should be 1!)");

        Map<GoodKey, String> fixedMap = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            fixedMap.put(new GoodKey(1), "value"); // All GoodKey(1) map to same entry
        }
        System.out.println("  FIX:  Map with GoodKey(proper equals/hashCode): size=" + fixedMap.size() + " (correct)\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bonus: Diagnosing memory leaks in production
    // ─────────────────────────────────────────────────────────────────────────
    static void diagnosisTips() {
        System.out.println("--- Diagnosis Tips ---");
        System.out.println("1. Monitor heap usage over time — steady growth after GC = leak");
        System.out.println("2. JVM flags: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heap.hprof");
        System.out.println("3. Manual dump: jmap -dump:format=b,file=heap.hprof <pid>");
        System.out.println("4. Analyze with: Eclipse MAT (free), VisualVM, IntelliJ Profiler");
        System.out.println("5. In MAT: 'Leak Suspects Report' → shows largest object graph roots");
        System.out.println("6. Look for: unexpectedly large byte[], char[], Object[] retained sets");
    }

    public static void main(String[] args) {
        System.out.println("=== Memory Leak Patterns Demo ===\n");

        pattern1Demo();
        pattern2Demo();
        pattern3Demo();
        pattern4Demo();
        pattern5Demo();
        diagnosisTips();

        System.out.println("\n=== Done ===");
    }
}
