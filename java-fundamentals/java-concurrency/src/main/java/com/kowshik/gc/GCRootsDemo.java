package com.kowshik.gc;

import java.util.ArrayList;
import java.util.List;

/**
 * GCRootsDemo — Reachability, GC Roots, and Why Reference Counting Fails
 *
 * INTERVIEW PREP:
 * ==============
 * Q: How does the JVM decide what is garbage?
 * A: Tracing from GC roots. Any object NOT reachable from a GC root (via any chain
 *    of strong references) is eligible for collection. Reference counting is NOT used.
 *
 * Q: What are GC roots?
 * A: Active thread stacks (local vars), static fields of loaded classes,
 *    JNI references, synchronized objects currently held.
 *
 * Q: Why doesn't Java use reference counting?
 * A: Circular references: A → B → A. Both have count > 0 but neither is reachable
 *    from any root. Reference counting would never collect them → memory leak.
 */
public class GCRootsDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // GC Root Type 1: Static field — lives as long as the class is loaded
    // ─────────────────────────────────────────────────────────────────────────
    private static List<byte[]> staticRoot = new ArrayList<>();

    static class Node {
        String name;
        Node next; // Used for circular reference demo
        byte[] payload;

        Node(String name, int payloadKb) {
            this.name = name;
            this.payload = new byte[payloadKb * 1024];
        }

        @Override public String toString() { return "Node(" + name + ")"; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: What keeps an object alive — reachability chains
    // ─────────────────────────────────────────────────────────────────────────
    static void reachabilityDemo() throws InterruptedException {
        System.out.println("--- Part 1: Reachability Chains ---");

        // Chain: staticRoot (GC root) → list → Node A → Node B
        // All nodes are reachable — none can be collected
        Node a = new Node("A", 10);
        Node b = new Node("B", 10);
        a.next = b;
        staticRoot.add(a.payload); // staticRoot keeps a.payload alive

        System.out.println("Node A and B: reachable via staticRoot chain");
        System.gc(); // Hint to GC — A and B survive (still reachable)
        Thread.sleep(100);
        System.out.println("After GC hint: A and B still reachable (staticRoot holds them)");

        // Break the chain
        staticRoot.clear(); // Remove from static root
        a = null;           // Remove local (stack) reference
        b = null;

        System.out.println("After clearing: A and B are unreachable — eligible for GC");
        System.gc();
        Thread.sleep(100);
        System.out.println("GC could now collect A and B\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: GC Root Type 2 — Local variables on the thread stack
    // ─────────────────────────────────────────────────────────────────────────
    static void localVariableRoot() throws InterruptedException {
        System.out.println("--- Part 2: Local Variable as GC Root ---");

        // 'local' is on this thread's stack — it IS a GC root while this method is active
        byte[] local = new byte[5 * 1024 * 1024]; // 5MB
        System.out.println("local[]: 5MB, reachable (on stack)");
        System.gc();
        Thread.sleep(100);
        System.out.println("After GC hint: local[] still alive (stack reference)");

        local = null; // Remove stack reference → no longer reachable
        System.gc();
        Thread.sleep(100);
        System.out.println("After local=null: 5MB is now eligible for collection\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 3: Why reference counting fails — circular references
    // Java uses TRACING GC, not reference counting, for exactly this reason
    // ─────────────────────────────────────────────────────────────────────────
    static void circularReferenceDemo() throws InterruptedException {
        System.out.println("--- Part 3: Circular References (why ref-counting fails) ---");

        // A → B → A (circular)
        Node a = new Node("A-circular", 1);
        Node b = new Node("B-circular", 1);
        a.next = b;
        b.next = a; // Circular!

        System.out.println("A and B form a cycle: A.next=B, B.next=A");
        System.out.println("With reference counting: both would have count=1 → NEVER collected");
        System.out.println("With tracing GC: neither reachable from any root → collected correctly");

        // Remove all external references to the cycle
        a = null;
        b = null;
        System.gc();
        Thread.sleep(100);
        System.out.println("After a=null, b=null: JVM tracing GC collects the cycle\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 4: Object resurrection via finalize() — why finalize() is dangerous
    // ─────────────────────────────────────────────────────────────────────────
    static volatile Node zombie = null;

    @SuppressWarnings("deprecation")
    static class ResurrectableNode {
        String name;
        ResurrectableNode(String name) { this.name = name; }

        @Override
        protected void finalize() {
            System.out.println("  [finalize] " + name + " — resurrecting itself!");
            zombie = new Node(name + "-zombie", 0); // Stores reference in static — prevents GC!
            // This is why finalize() is deprecated: unpredictable, can leak objects
        }
    }

    static void resurrectionDemo() throws InterruptedException {
        System.out.println("--- Part 4: Object Resurrection (finalize() danger) ---");
        System.out.println("  (finalize() is deprecated — shown here to explain WHY)");

        ResurrectableNode r = new ResurrectableNode("doomed");
        r = null; // Remove reference — eligible for GC
        System.gc();
        Thread.sleep(500); // Give finalizer thread time to run

        if (zombie != null) {
            System.out.println("  Zombie node created by finalize(): " + zombie.name);
            System.out.println("  Object was 'dead' but resurrected — demonstrates why finalize() is banned\n");
        } else {
            System.out.println("  finalize() hasn't run yet (no timing guarantee)\n");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== GC Roots Demo ===\n");

        reachabilityDemo();
        localVariableRoot();
        circularReferenceDemo();
        resurrectionDemo();

        System.out.println("=== Done ===");
        System.out.println("Run with: -verbose:gc to see GC events in output");
    }
}
