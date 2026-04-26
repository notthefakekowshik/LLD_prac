package com.kowshik.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SemaphoreVsMutexDemo
 *
 * Demonstrates the key difference between Semaphore(1) and ReentrantLock
 * as a mutex:
 *
 *   ReentrantLock  — TRUE mutex: only the thread that locked it can unlock it.
 *                    Attempting to unlock from a different thread throws
 *                    IllegalMonitorStateException.
 *
 *   Semaphore(1)   — SIGNALING mechanism: ANY thread can call release(),
 *                    even one that never called acquire(). This breaks the
 *                    "owner must release" invariant of a true mutex and can
 *                    corrupt shared state.
 *
 * Three scenarios shown:
 *   1. ReentrantLock — correct mutual exclusion, owner-enforced unlock.
 *   2. Semaphore(1) as a signaling mechanism — producer/consumer handoff.
 *   3. Semaphore(1) ownership bug — a third-party thread releases the
 *      semaphore early, letting two threads inside the "critical section"
 *      simultaneously, causing a visible data race.
 */
public class SemaphoreVsMutexDemo {

    private static final Logger log = LoggerFactory.getLogger(SemaphoreVsMutexDemo.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("==============================");
        log.info("Scenario 1: ReentrantLock — true mutex, owner-enforced");
        log.info("==============================");
        reentrantLockOwnershipDemo();

        Thread.sleep(500);

        log.info("\n==============================");
        log.info("Scenario 2: Semaphore(1) used correctly — signaling (producer/consumer)");
        log.info("==============================");
        semaphoreSignalingDemo();

        Thread.sleep(500);

        log.info("\n==============================");
        log.info("Scenario 3: Semaphore(1) ownership bug — third thread releases early");
        log.info("==============================");
        semaphoreOwnershipBugDemo();
    }

    // -------------------------------------------------------------------------
    // Scenario 1: ReentrantLock — only the owner can unlock
    // -------------------------------------------------------------------------

    private static void reentrantLockOwnershipDemo() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();

        Thread worker = new Thread(() -> {
            lock.lock();
            log.info("[Worker] Acquired ReentrantLock — doing critical work for 300ms...");
            try {
                Thread.sleep(300);
                log.info("[Worker] Done. Releasing lock.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();  // only the owner releases — correct
            }
        }, "Worker");

        Thread thief = new Thread(() -> {
            log.info("[Thief]  Trying to unlock a ReentrantLock it never acquired...");
            try {
                lock.unlock();  // not the owner → throws
                log.info("[Thief]  Unlocked successfully (THIS SHOULD NOT HAPPEN)");
            } catch (IllegalMonitorStateException e) {
                log.info("[Thief]  Caught IllegalMonitorStateException — cannot steal a ReentrantLock. ✓");
            }
        }, "Thief");

        worker.start();
        Thread.sleep(100);  // let worker acquire the lock first
        thief.start();

        worker.join();
        thief.join();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Semaphore(1) used correctly as a signaling mechanism
    //
    // A classic binary semaphore handoff: producer signals consumer.
    // This is the LEGITIMATE use of Semaphore(1) — not as a mutex.
    // -------------------------------------------------------------------------

    private static void semaphoreSignalingDemo() throws InterruptedException {
        // Start with 0 permits — consumer blocks until producer signals
        Semaphore signal = new Semaphore(0);
        int[] data = {0};

        Thread producer = new Thread(() -> {
            log.info("[Producer] Producing data...");
            data[0] = 42;
            log.info("[Producer] Data ready: {}. Signaling consumer.", data[0]);
            signal.release();  // producer releases — consumer it was waiting for
        }, "Producer");

        Thread consumer = new Thread(() -> {
            try {
                log.info("[Consumer] Waiting for data...");
                signal.acquire();  // blocks until producer calls release()
                log.info("[Consumer] Received signal. Data = {}  ✓", data[0]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        consumer.start();
        Thread.sleep(200);
        producer.start();

        producer.join();
        consumer.join();

        log.info("[Result]  Semaphore(1) as a one-shot signal — works correctly here.");
        log.info("          Note: consumer released the lock (producer), not the one that acquired it.");
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Semaphore(1) ownership bug
    //
    // Thread-A acquires Semaphore(1). Thread-B (a completely unrelated thread)
    // calls release() on it — bumping permits to 1 again while Thread-A still
    // thinks it is "inside" the critical section.
    // Thread-C then acquires the permit and BOTH A and C are inside the
    // critical section simultaneously — a visible data race.
    // -------------------------------------------------------------------------

    private static void semaphoreOwnershipBugDemo() throws InterruptedException {
        Semaphore mutex = new Semaphore(1);  // intended to act like a mutex
        int[] sharedCounter = {0};

        log.info("[Bug Demo] sharedCounter starts at 0.");
        log.info("[Bug Demo] Both Thread-A and Thread-C will increment it 3 times each.");
        log.info("[Bug Demo] Expected final value = 6. Due to the race, result may differ.\n");

        Thread threadA = new Thread(() -> {
            try {
                mutex.acquire();
                log.info("[Thread-A] Acquired semaphore. Starting critical section.");
                for (int i = 0; i < 3; i++) {
                    int old = sharedCounter[0];
                    Thread.sleep(50);             // simulate work; window for Thread-C to sneak in
                    sharedCounter[0] = old + 1;
                    log.info("[Thread-A] incremented counter → {}", sharedCounter[0]);
                }
                log.info("[Thread-A] Done. Releasing semaphore.");
                mutex.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-A");

        Thread intruder = new Thread(() -> {
            try {
                Thread.sleep(80);  // wait until Thread-A is mid-critical-section
                log.info("[Intruder] Calling release() on a semaphore it NEVER acquired!");
                mutex.release();   // Semaphore allows this — no ownership check
                log.info("[Intruder] Released. Permits available = {} (should be 0 while A is inside!)",
                        mutex.availablePermits());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Intruder");

        Thread threadC = new Thread(() -> {
            try {
                Thread.sleep(100);  // start slightly after intruder releases
                if (mutex.tryAcquire()) {
                    log.info("[Thread-C] *** Acquired semaphore while Thread-A is STILL inside! ***");
                    log.info("[Thread-C] Two threads in critical section simultaneously — DATA RACE!");
                    for (int i = 0; i < 3; i++) {
                        int old = sharedCounter[0];
                        Thread.sleep(50);
                        sharedCounter[0] = old + 1;
                        log.info("[Thread-C] incremented counter → {}", sharedCounter[0]);
                    }
                    log.info("[Thread-C] Done. Releasing semaphore.");
                    mutex.release();
                } else {
                    log.info("[Thread-C] Could not acquire semaphore — no bug triggered this run.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-C");

        threadA.start();
        intruder.start();
        threadC.start();

        threadA.join();
        intruder.join();
        threadC.join();

        log.info("\n[Result]  Final counter value = {} (expected 6 if no race)", sharedCounter[0]);
        log.info("[Result]  With ReentrantLock, the Intruder thread would have thrown");
        log.info("[Result]  IllegalMonitorStateException and Thread-C would never enter.");
    }
}
