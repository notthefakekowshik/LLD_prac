package com.kowshik.synchronization;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DeadlockDemo — Deadlock Creation, Detection, and Avoidance
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What are the four conditions for deadlock?
 * A: Mutual exclusion, hold-and-wait, no preemption, circular wait.
 *    ALL four must hold. Breaking ANY one prevents deadlock.
 *
 * Q: How do you detect deadlock in production?
 * A: jstack <pid> — JVM prints "Found one Java-level deadlock" with full stack traces.
 *    Also: ThreadMXBean.findDeadlockedThreads() programmatically.
 *
 * Q: How do you prevent deadlock?
 * A: (1) Lock ordering — always acquire in the same order.
 *    (2) tryLock with timeout — give up if you can't get the second lock.
 *    (3) Lock hierarchy — assign numeric levels, always acquire lower first.
 *
 * Q: What is livelock?
 * A: Threads are running but making no progress — they keep politely yielding to each
 *    other. Fix: add randomness to retry intervals.
 */
public class DeadlockDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: DEADLOCK — circular lock acquisition
    // Do NOT run in production — this will deadlock permanently
    // ─────────────────────────────────────────────────────────────────────────
    static final Object LOCK_A = new Object();
    static final Object LOCK_B = new Object();

    static void demonstrateDeadlock() throws InterruptedException {
        System.out.println("--- Part 1: Deadlock Creation ---");
        System.out.println("Thread 1 acquires A, then tries B.");
        System.out.println("Thread 2 acquires B, then tries A.");
        System.out.println("→ Deadlock in ~100ms. Main will interrupt after 2s.\n");

        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("[T1] acquired LOCK_A");
                try { Thread.sleep(100); } catch (InterruptedException e) { return; }
                System.out.println("[T1] waiting for LOCK_B...");
                synchronized (LOCK_B) {
                    System.out.println("[T1] acquired LOCK_B — DONE");
                }
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("[T2] acquired LOCK_B");
                try { Thread.sleep(100); } catch (InterruptedException e) { return; }
                System.out.println("[T2] waiting for LOCK_A...");
                synchronized (LOCK_A) {
                    System.out.println("[T2] acquired LOCK_A — DONE");
                }
            }
        }, "Thread-2");

        t1.start();
        t2.start();

        Thread.sleep(2000); // Wait 2s then interrupt to break out of demo
        t1.interrupt();
        t2.interrupt();
        System.out.println("[main] interrupted deadlocked threads\n");
        t1.join(500);
        t2.join(500);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: AVOIDANCE via Lock Ordering
    // Always acquire LOCK_A before LOCK_B — in ALL threads
    // ─────────────────────────────────────────────────────────────────────────
    static void avoidanceViaOrdering() throws InterruptedException {
        System.out.println("--- Part 2: Avoidance via Lock Ordering ---");

        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {          // Always A first
                System.out.println("[T1] acquired A");
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                synchronized (LOCK_B) {      // Then B
                    System.out.println("[T1] acquired B — done");
                }
            }
        }, "ordered-T1");

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_A) {          // Also A first (same order as T1)
                System.out.println("[T2] acquired A");
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                synchronized (LOCK_B) {
                    System.out.println("[T2] acquired B — done");
                }
            }
        }, "ordered-T2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("Lock ordering: no deadlock!\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 3: AVOIDANCE via tryLock with timeout (ReentrantLock)
    // If you can't get the second lock within timeout, release the first and retry
    // ─────────────────────────────────────────────────────────────────────────
    static final ReentrantLock RLOCK_A = new ReentrantLock();
    static final ReentrantLock RLOCK_B = new ReentrantLock();

    static boolean tryAcquireBoth(String threadName) throws InterruptedException {
        while (true) {
            boolean gotA = RLOCK_A.tryLock(100, TimeUnit.MILLISECONDS);
            if (!gotA) {
                System.out.println("[" + threadName + "] couldn't get A, retrying...");
                continue;
            }
            try {
                boolean gotB = RLOCK_B.tryLock(100, TimeUnit.MILLISECONDS);
                if (!gotB) {
                    System.out.println("[" + threadName + "] got A but not B — releasing A, retrying...");
                    continue; // finally block releases A
                }
                try {
                    // Have both locks
                    System.out.println("[" + threadName + "] acquired both — doing work");
                    Thread.sleep(50);
                    return true;
                } finally {
                    RLOCK_B.unlock();
                }
            } finally {
                RLOCK_A.unlock();
            }
        }
    }

    static void avoidanceViaTryLock() throws InterruptedException {
        System.out.println("--- Part 3: Avoidance via tryLock with timeout ---");

        Thread t1 = new Thread(() -> {
            try { tryAcquireBoth("TryLock-T1"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Thread t2 = new Thread(() -> {
            try { tryAcquireBoth("TryLock-T2"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("tryLock: no deadlock!\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 4: Programmatic deadlock detection via ThreadMXBean
    // ─────────────────────────────────────────────────────────────────────────
    static void detectDeadlock() {
        System.out.println("--- Part 4: Programmatic Deadlock Detection ---");
        java.lang.management.ThreadMXBean tmx =
                java.lang.management.ManagementFactory.getThreadMXBean();
        long[] deadlockedIds = tmx.findDeadlockedThreads();
        if (deadlockedIds == null) {
            System.out.println("No deadlocks detected (expected — we broke out of the demo).");
        } else {
            java.lang.management.ThreadInfo[] infos = tmx.getThreadInfo(deadlockedIds);
            System.out.println("DEADLOCKED THREADS:");
            for (java.lang.management.ThreadInfo info : infos) {
                System.out.println("  " + info.getThreadName() + " → waiting for: " +
                        info.getLockName());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Deadlock Demo ===\n");

        demonstrateDeadlock();
        avoidanceViaOrdering();
        avoidanceViaTryLock();
        detectDeadlock();

        System.out.println("=== Done ===");
    }
}
