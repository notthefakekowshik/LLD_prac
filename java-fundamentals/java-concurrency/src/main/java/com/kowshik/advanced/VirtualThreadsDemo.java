package com.kowshik.advanced;

/*
 * NOTE: This file is currently commented out because it uses Java 21 Preview Features 
 * (Structured Concurrency - JEP 453/480).
 * 
 * To run this code, you need:
 * 1. Java 21 or later installed.
 * 2. Maven compiler configured with <compilerArgs><arg>--enable-preview</arg></compilerArgs>
 * 3. JVM run with --enable-preview flag.
 * 
 * Since preview features are subject to change and require specific environment flags 
 * that might break standard builds, this code is kept here for reference/interview prep 
 * but disabled for the main build.
 */

/*
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VirtualThreadsDemo — Java 21 Project Loom: Virtual Threads & Structured Concurrency
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What are virtual threads and why do they matter?
 * A: JVM-managed threads (not OS threads). Blocking a virtual thread unmounts it from
 *    the carrier platform thread — the carrier is freed to run other virtual threads.
 *    Result: millions of concurrent I/O tasks with minimal platform threads.
 *
 * Q: What is pinning and why is it bad?
 * A: A virtual thread inside a synchronized block cannot unmount from its carrier.
 *    The carrier is blocked for the entire duration — defeating Loom's purpose.
 *    Fix: replace synchronized with ReentrantLock.
 *
 * Q: When should you NOT use virtual threads?
 * A: CPU-bound tasks (no blocking = no benefit), code with heavy synchronized usage
 *    (pinning blocks carriers), tasks that MUST NOT migrate between carriers
 *    (ThreadLocal assumptions can break if semantics depend on carrier identity).
 *
 * Q: What is structured concurrency?
 * A: StructuredTaskScope enforces that all forked tasks complete (or are cancelled)
 *    before the scope closes. Prevents task leaks. Exception propagation is cleaner
 *    than CompletableFuture.allOf().
 * /
public class VirtualThreadsDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: Creating virtual threads
    // ─────────────────────────────────────────────────────────────────────────
    static void creationDemo() throws InterruptedException {
        System.out.println("--- Part 1: Virtual Thread Creation ---");

        // Method 1: Thread.ofVirtual()
        Thread vt1 = Thread.ofVirtual().name("vt-named").start(() ->
                System.out.printf("  [%s] isVirtual=%b%n",
                        Thread.currentThread().getName(),
                        Thread.currentThread().isVirtual()));
        vt1.join();

        // Method 2: Virtual thread executor (one virtual thread per submitted task)
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            exec.submit(() -> System.out.printf("  [%s] via executor, isVirtual=%b%n",
                    Thread.currentThread().getName(),
                    Thread.currentThread().isVirtual()));
        } // auto-shutdown + waits for submitted tasks
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: Scale — 100,000 virtual threads doing I/O-like work
    // Platform threads would exhaust memory; virtual threads handle this trivially
    // ─────────────────────────────────────────────────────────────────────────
    static void scaleDemo() throws InterruptedException {
        System.out.println("\n--- Part 2: Scale — 10,000 concurrent virtual threads ---");

        int taskCount = 10_000;
        AtomicInteger completed = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                exec.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofMillis(100)); // Simulates I/O wait
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } // Blocks until all tasks finish

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("  Completed: %d tasks in %dms (would take %ds sequentially)%n",
                completed.get(), elapsed, taskCount / 10);
        // With platform threads: 10,000 × 1MB stacks = 10GB. JVM would crash.
        // With virtual threads: runs in milliseconds with minimal memory.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 3: Pinning — the main gotcha
    // A synchronized block prevents the virtual thread from unmounting
    // ─────────────────────────────────────────────────────────────────────────
    static void pinningDemo() throws InterruptedException {
        System.out.println("\n--- Part 3: Pinning Demo ---");

        // JVM flag to detect pinning: -Djdk.tracePinnedThreads=full
        // For demo purposes we just illustrate the pattern:

        Object monitor = new Object();

        // BAD: synchronized block + blocking I/O = pinning
        Thread pinned = Thread.ofVirtual().name("pinned-vt").start(() -> {
            synchronized (monitor) {
                try {
                    // This pins the virtual thread to its carrier!
                    // The carrier cannot serve other virtual threads during this sleep.
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // GOOD: ReentrantLock + blocking I/O = NO pinning
        java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
        Thread unpinned = Thread.ofVirtual().name("unpinned-vt").start(() -> {
            lock.lock();
            try {
                // Virtual thread can unmount from carrier during this sleep!
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        pinned.join();
        unpinned.join();
        System.out.println("  Pinned (synchronized) and Unpinned (ReentrantLock) examples complete.");
        System.out.println("  In production: use -Djdk.tracePinnedThreads=full to detect pinning.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 4: Structured Concurrency — tasks scoped to their parent
    // All forked tasks are guaranteed to finish before scope.close() returns
    // ─────────────────────────────────────────────────────────────────────────
    static void structuredConcurrencyDemo() throws InterruptedException {
        System.out.println("\n--- Part 4: Structured Concurrency ---");

        // ShutdownOnFailure: if any subtask fails, cancel all others
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            StructuredTaskScope.Subtask<String> userTask =
                    scope.fork(() -> fetchUser(1));
            StructuredTaskScope.Subtask<String> ordersTask =
                    scope.fork(() -> fetchOrders(1));

            scope.join();           // Wait for both subtasks to finish (or one to fail)
            scope.throwIfFailed();  // Propagate exception if any subtask failed

            System.out.printf("  User: %s%n", userTask.get());
            System.out.printf("  Orders: %s%n", ordersTask.get());
            // When scope closes: any running tasks are cancelled. No leaks.

        } catch (Exception e) {
            System.out.println("  Structured scope failed: " + e.getMessage());
        }

        // ShutdownOnSuccess: return the first successful result, cancel others
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            scope.fork(() -> { Thread.sleep(300); return "replica-1 result"; });
            scope.fork(() -> { Thread.sleep(100); return "replica-2 result (faster)"; });
            scope.fork(() -> { Thread.sleep(200); return "replica-3 result"; });

            scope.join();
            System.out.println("  First result: " + scope.result());
            // replica-2 wins; replica-1 and replica-3 are cancelled
        } catch (Exception e) {
            System.out.println("  Race failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simulated I/O calls (replace with real HTTP/DB in production)
    // ─────────────────────────────────────────────────────────────────────────
    static String fetchUser(int id) throws InterruptedException {
        Thread.sleep(150); // Simulates DB query
        return "User{id=" + id + ", name='Alice'}";
    }

    static String fetchOrders(int id) throws InterruptedException {
        Thread.sleep(200); // Simulates HTTP call
        return "Orders{userId=" + id + ", count=5}";
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Virtual Threads Demo (Java 21) ===\n");

        creationDemo();
        scaleDemo();
        pinningDemo();
        structuredConcurrencyDemo();

        System.out.println("\n=== Done ===");
    }
}
*/
