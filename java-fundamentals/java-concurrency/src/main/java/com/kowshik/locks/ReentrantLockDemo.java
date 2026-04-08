package com.kowshik.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock Demo - Advanced Locking Mechanism
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. What is ReentrantLock and when to use it over synchronized?
 * - More flexible than synchronized keyword
 * - Explicit lock/unlock (must use try-finally!)
 * - Supports timed lock attempts (tryLock with timeout)
 * - Supports interruptible lock acquisition
 * - Fair lock option to prevent thread starvation
 *
 * 2. Common Interview Questions:
 * - ReentrantLock vs synchronized keyword?
 * - What is a reentrant lock? (Can be acquired multiple times by same thread)
 * - Explain fair vs non-fair locks
 * - Why use try-finally with ReentrantLock?
 * - What is tryLock() and when to use it?
 * - Can you check if lock is held? (isHeldByCurrentThread())
 * - What is lockInterruptibly()?
 *
 * 3. ReentrantLock vs synchronized:
 * synchronized:
 * - Simple, less code
 * - Automatic lock release
 * - Block scoped
 * - No tryLock, no timeout
 *
 * ReentrantLock:
 * - More control and flexibility
 * - Manual lock/unlock
 * - Can span methods
 * - tryLock(), tryLock(timeout), lockInterruptibly()
 * - Fair lock option
 *
 * 4. Fair vs Non-Fair Locks:
 * - Fair: FIFO order, prevents starvation, lower throughput
 * - Non-Fair (default): Better throughput, possible starvation
 *
 * 5. Best Practices:
 * - Always use try-finally pattern
 * - Lock in try block, unlock in finally
 * - Don't forget to unlock!
 * - Use tryLock() to avoid deadlocks
 * - Consider lock fairness requirements
 *
 * 6. Common Pitfalls:
 * - Forgetting to unlock (memory leak, deadlock)
 * - Not using try-finally
 * - Catching Exception between lock and unlock
 * - Calling unlock() without lock()
 */
public class ReentrantLockDemo {
    private static final Logger log = LoggerFactory.getLogger(ReentrantLockDemo.class);

    private int counter = 0;
    private final Lock lock = new ReentrantLock();

    /**
     * Increments counter using ReentrantLock.
     * Demonstrates proper lock/unlock pattern with try-finally.
     */
    public void increment() {
        lock.lock();
        try {
            counter++;
        } finally {
            lock.unlock(); // ALWAYS unlock in finally block
        }
    }

    /**
     * Increments counter with tryLock - non-blocking attempt.
     *
     * @return true if increment was successful, false if lock unavailable
     */
    public boolean tryIncrement() {
        if (lock.tryLock()) {
            try {
                counter++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false; // Lock was not available
    }

    /**
     * Increments counter with timeout - waits up to specified time.
     *
     * @param timeout maximum time to wait
     * @param unit    time unit
     * @return true if increment was successful, false if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean tryIncrementWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
        if (lock.tryLock(timeout, unit)) {
            try {
                counter++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false; // Timeout occurred
    }

    public int getCounter() {
        lock.lock();
        try {
            return counter;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Demonstrates ReentrantLock features and usage patterns.
     *
     * @param args command line arguments
     * @throws InterruptedException if interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        log.info("=== ReentrantLock Demo ===");

        // Demo 1: Basic usage
        log.info("\nDemo 1: Basic ReentrantLock usage");
        basicUsageDemo();

        // Demo 2: tryLock() - non-blocking
        log.info("\nDemo 2: tryLock() - non-blocking attempt");
        tryLockDemo();

        // Demo 3: tryLock(timeout) - timed wait
        log.info("\nDemo 3: tryLock(timeout) - timed wait");
        timedLockDemo();

        // Demo 4: Fair lock
        log.info("\nDemo 4: Fair lock demo");
        fairLockDemo();
    }

    private static void basicUsageDemo() throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                demo.increment();
            }
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        log.info("Expected: 2000, Actual: {}", demo.getCounter());
    }

    private static void tryLockDemo() throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                if (demo.tryIncrement()) {
                    log.info("Thread-1: Successfully incremented");
                } else {
                    log.info("Thread-1: Lock not available, skipping");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                if (demo.tryIncrement()) {
                    log.info("Thread-2: Successfully incremented");
                } else {
                    log.info("Thread-2: Lock not available, skipping");
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        log.info("Final counter: {}", demo.getCounter());
    }

    private static void timedLockDemo() throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();
        Lock slowLock = new ReentrantLock();

        Thread blocker = new Thread(() -> {
            slowLock.lock();
            try {
                log.info("Blocker: Holding lock for 2 seconds");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                slowLock.unlock();
                log.info("Blocker: Released lock");
            }
        }, "Blocker");

        blocker.start();
        Thread.sleep(100); // Let blocker acquire lock first

        Thread waiter = new Thread(() -> {
            try {
                log.info("Waiter: Trying to acquire lock with 500ms timeout");
                if (slowLock.tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        log.info("Waiter: Lock acquired!");
                    } finally {
                        slowLock.unlock();
                    }
                } else {
                    log.info("Waiter: Timeout! Could not acquire lock");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Waiter");

        waiter.start();
        blocker.join();
        waiter.join();
    }

    private static void fairLockDemo() throws InterruptedException {
        log.info("Non-fair lock (default):");
        testLockFairness(false);

        Thread.sleep(1000);

        log.info("Fair lock:");
        testLockFairness(true);
    }

    private static void testLockFairness(boolean fair) throws InterruptedException {
        Lock lock = new ReentrantLock(fair);
        Runnable task = () -> {
            for (int i = 0; i < 3; i++) {
                lock.lock();
                try {
                    log.info("{} acquired lock (attempt {})", Thread.currentThread().getName(), i + 1);
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");
        Thread t3 = new Thread(task, "Thread-3");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();
    }
}

/**
 * Example demonstrating the necessity of try-finally pattern.
 */
class RawThreadExamplee {
    private static final Logger log = LoggerFactory.getLogger(RawThreadExamplee.class);

    /**
     * Demonstrates basic thread creation.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Runnable task = () -> {
            log.info("Hello from thread: {}", Thread.currentThread().getName());
        };
        Thread thread = new Thread(task);
        thread.start();
        log.info("Hello from main thread: {}", Thread.currentThread().getName());
    }
}
