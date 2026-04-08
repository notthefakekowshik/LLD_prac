package com.kowshik.atomics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * CASDemo — Compare-And-Swap Internals, ABA Problem, and AtomicStampedReference
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What is CAS and how does it work?
 * A: A single atomic CPU instruction (CMPXCHG on x86). Reads a value, compares
 *    to expected, writes new value ONLY if they match — all in one indivisible step.
 *
 * Q: What is the ABA problem?
 * A: Thread reads value A. Another thread changes A→B→A. Original thread's CAS
 *    succeeds even though the state changed beneath it.
 *
 * Q: How do you fix ABA?
 * A: AtomicStampedReference — pairs the reference with a monotonically increasing
 *    stamp (version number). CAS requires BOTH reference and stamp to match.
 *
 * Q: When is CAS better than synchronized? When is it worse?
 * A: Better: single-variable, low-to-moderate contention (no OS mutex, no context switch).
 *    Worse: high contention (retry loop spins), multi-variable invariants (use synchronized).
 */
public class CASDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: Manual CAS loop — how AtomicInteger.incrementAndGet() works
    // ─────────────────────────────────────────────────────────────────────────
    static void casLoop() {
        System.out.println("--- Part 1: Manual CAS Loop ---");
        AtomicInteger counter = new AtomicInteger(0);

        // This is exactly what incrementAndGet() does internally:
        int current;
        int retries = 0;
        do {
            current = counter.get();          // Step 1: read
            retries++;
        } while (!counter.compareAndSet(current, current + 1)); // Step 2: CAS

        System.out.println("Final value: " + counter.get() + " (retries: " + retries + ")");

        // Under high concurrency, retry count can be significant:
        AtomicInteger sharedCounter = new AtomicInteger(0);
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    sharedCounter.incrementAndGet(); // CAS-based, no lock
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        System.out.println("10 threads × 1000 increments = " + sharedCounter.get() + " (expected 10000)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: ABA Problem
    // ─────────────────────────────────────────────────────────────────────────

    // A simple linked list node for the ABA demo
    static class Node {
        final int value;
        Node next;
        Node(int v) { this.value = v; }
        @Override public String toString() { return "Node(" + value + ")"; }
    }

    static void demonstrateABA() throws InterruptedException {
        System.out.println("\n--- Part 2: ABA Problem ---");

        Node nodeA = new Node(1);
        Node nodeB = new Node(2);
        nodeA.next = nodeB;

        AtomicReference<Node> head = new AtomicReference<>(nodeA);

        System.out.println("Initial head: " + head.get());
        System.out.println("Thread 1 reads head = " + head.get() + " and is about to CAS...");

        // Simulate Thread 2 doing A→B→A before Thread 1's CAS executes:
        Node savedA = head.get();

        // Thread 2: pop A, pop B, push A back
        head.compareAndSet(nodeA, nodeB); // A → B
        head.compareAndSet(nodeB, nodeA); // B → A (!)

        System.out.println("Thread 2 changed: A→B then B→A. Head is back to: " + head.get());
        System.out.println("Node B is now LOST (orphaned).");

        // Thread 1's CAS: expects A, sees A — SUCCEEDS despite state change
        boolean success = head.compareAndSet(savedA, new Node(99));
        System.out.println("Thread 1 CAS succeeded? " + success + " (ABA — it should have failed!)");
        System.out.println("Result: nodeB (" + nodeB + ") was silently lost\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 3: Fix ABA with AtomicStampedReference
    // Each CAS requires BOTH the reference AND a version stamp to match
    // ─────────────────────────────────────────────────────────────────────────
    static void fixABAWithStamp() throws InterruptedException {
        System.out.println("--- Part 3: ABA Fix with AtomicStampedReference ---");

        Node nodeA = new Node(1);
        Node nodeB = new Node(2);
        nodeA.next = nodeB;

        // Initial stamp = 0
        AtomicStampedReference<Node> stampedHead =
                new AtomicStampedReference<>(nodeA, 0);

        int[] stampHolder = new int[1];
        Node savedA = stampedHead.get(stampHolder);
        int savedStamp = stampHolder[0]; // stamp = 0
        System.out.println("Thread 1 reads head=" + savedA + " stamp=" + savedStamp);

        // Thread 2: A→B (stamp 0→1) then B→A (stamp 1→2)
        stampedHead.compareAndSet(nodeA, nodeB, 0, 1); // stamp 0→1
        stampedHead.compareAndSet(nodeB, nodeA, 1, 2); // stamp 1→2

        Node current = stampedHead.get(stampHolder);
        System.out.println("Thread 2 cycled A→B→A. Current head=" + current + " stamp=" + stampHolder[0]);

        // Thread 1 tries CAS with old reference (nodeA) AND old stamp (0)
        // Even though reference matches, STAMP doesn't (it's now 2)
        boolean success = stampedHead.compareAndSet(savedA, new Node(99), savedStamp, savedStamp + 1);
        System.out.println("Thread 1 CAS succeeded? " + success + " (correctly FAILED — ABA prevented!)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 4: compareAndSet return value semantics
    // ─────────────────────────────────────────────────────────────────────────
    static void casSemantics() {
        System.out.println("\n--- Part 4: CAS Semantics ---");
        AtomicInteger ai = new AtomicInteger(10);

        // Success: expected matches current
        boolean r1 = ai.compareAndSet(10, 20);
        System.out.println("CAS(10, 20) on value 10 → " + r1 + ", new value: " + ai.get());

        // Failure: expected does NOT match current (current is 20, not 10)
        boolean r2 = ai.compareAndSet(10, 30);
        System.out.println("CAS(10, 30) on value 20 → " + r2 + ", value unchanged: " + ai.get());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== CAS Demo ===\n");

        casLoop();
        demonstrateABA();
        fixABAWithStamp();
        casSemantics();

        System.out.println("\n=== Done ===");
    }
}
