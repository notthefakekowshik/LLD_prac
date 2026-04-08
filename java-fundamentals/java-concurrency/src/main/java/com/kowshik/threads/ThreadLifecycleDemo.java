package com.kowshik.threads;

import java.util.concurrent.locks.LockSupport;

/**
 * ThreadLifecycleDemo — All 6 Thread States with Real Transitions
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What are the 6 thread states?
 * A: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED
 *
 * Q: What is the difference between BLOCKED and WAITING?
 * A: BLOCKED = waiting to acquire a synchronized monitor lock.
 *    WAITING  = waiting for a signal (notify/unpark/join complete).
 *    Both show up differently in thread dumps (jstack).
 *
 * Q: What causes a thread to go from RUNNABLE to WAITING?
 * A: Object.wait(), Thread.join(), LockSupport.park()
 *
 * Q: What causes TIMED_WAITING?
 * A: Thread.sleep(ms), Object.wait(ms), Thread.join(ms), LockSupport.parkNanos()
 */
public class ThreadLifecycleDemo {

    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Thread Lifecycle Demo — All 6 States ===\n");

        demoAllStates();

        System.out.println("\n=== Demo Complete ===");
    }

    static void demoAllStates() throws InterruptedException {

        // ── State 1: NEW ──────────────────────────────────────────────────────
        // Thread created but not started
        Thread t = new Thread(() -> {
            // This body runs in RUNNABLE
            System.out.println("  [worker] running (RUNNABLE on CPU)");
        });
        System.out.println("State 1 — NEW: " + t.getState()); // NEW

        // ── State 2: RUNNABLE ─────────────────────────────────────────────────
        // After start(), thread may be on CPU or waiting for CPU (both = RUNNABLE)
        t.start();
        System.out.println("State 2 — RUNNABLE: " + t.getState()); // RUNNABLE (probably)
        t.join();

        // ── State 6: TERMINATED ───────────────────────────────────────────────
        System.out.println("State 6 — TERMINATED: " + t.getState()); // TERMINATED

        // ── State 3: BLOCKED ──────────────────────────────────────────────────
        // Thread waiting to acquire a synchronized lock
        System.out.println("\n--- BLOCKED demo ---");
        demoBlocked();

        // ── State 4: WAITING ──────────────────────────────────────────────────
        // Thread waiting indefinitely for notify/unpark/join
        System.out.println("\n--- WAITING demo ---");
        demoWaiting();

        // ── State 5: TIMED_WAITING ────────────────────────────────────────────
        // Thread waiting with a timeout
        System.out.println("\n--- TIMED_WAITING demo ---");
        demoTimedWaiting();
    }

    // ── BLOCKED: thread tries to enter a synchronized block held by another ──
    static void demoBlocked() throws InterruptedException {
        Object lock = new Object();

        Thread blocker = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(500); // Hold the lock for 500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "blocker");

        Thread blocked = new Thread(() -> {
            synchronized (lock) { // Will BLOCK here until blocker releases
                System.out.println("  [blocked-thread] acquired lock");
            }
        }, "blocked");

        blocker.start();
        Thread.sleep(50); // Let blocker acquire the lock first
        blocked.start();
        Thread.sleep(50); // Let blocked attempt to acquire
        System.out.println("  blocked-thread state: " + blocked.getState()); // BLOCKED

        blocker.join();
        blocked.join();
    }

    // ── WAITING: thread calls Object.wait() and waits for notify ─────────────
    static void demoWaiting() throws InterruptedException {
        Object lock = new Object();

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(); // Releases lock, enters WAITING
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("  [waiter] was notified, continuing");
        }, "waiter");

        waiter.start();
        Thread.sleep(100); // Let waiter enter wait()
        System.out.println("  waiter state: " + waiter.getState()); // WAITING

        synchronized (lock) {
            lock.notify(); // Wake up the waiter
        }
        waiter.join();
    }

    // ── TIMED_WAITING: thread calls sleep(ms) ────────────────────────────────
    static void demoTimedWaiting() throws InterruptedException {
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(1000); // TIMED_WAITING for 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "sleeper");

        sleeper.start();
        Thread.sleep(100); // Let sleeper enter sleep
        System.out.println("  sleeper state: " + sleeper.getState()); // TIMED_WAITING
        sleeper.join();
    }
}
