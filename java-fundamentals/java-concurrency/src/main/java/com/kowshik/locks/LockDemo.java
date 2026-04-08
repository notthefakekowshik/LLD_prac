package com.kowshik.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-level Demonstration of Java Locking Mechanisms.
 *
 * This class illustrates the fundamental differences between:
 * 1. Intrinsic Locks (monitor locks using the 'synchronized' keyword)
 * 2. Extrinsic Locks (java.util.concurrent.locks.* interfaces)
 */
public class LockDemo {
    private static final Logger log = LoggerFactory.getLogger(LockDemo.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("=== Java Locking Mechanisms Demo ===\n");

        log.info("--- 1. Intrinsic Lock (Synchronized) ---");
        demonstrateIntrinsicLock();

        Thread.sleep(1000);

        log.info("\n--- 2. Extrinsic Lock (ReentrantLock) ---");
        demonstrateExtrinsicLock();

        Thread.sleep(1000);

        log.info("\n--- 3. Extrinsic Lock (ReadWriteLock) ---");
        demonstrateReadWriteLock();
    }

    /**
     * Demonstrates an Intrinsic Lock using the 'synchronized' keyword.
     * 
     * Properties:
     * - Implicitly acquired and released at the beginning/end of the synchronized
     * block/method.
     * - Cannot be interrupted while waiting to acquire.
     * - No try-lock capability (cannot poll to see if lock is available).
     * - Less flexible, but syntax is simple and less prone to user error (like
     * forgetting to unlock).
     */
    private static void demonstrateIntrinsicLock() throws InterruptedException {
        Object intrinsicLock = new Object();

        Runnable task = () -> {
            log.info("{} trying to acquire intrinsic lock...", Thread.currentThread().getName());
            synchronized (intrinsicLock) {
                log.info("{} acquired intrinsic lock!", Thread.currentThread().getName());
                try {
                    Thread.sleep(500); // Simulate work while holding the lock
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.info("{} releasing intrinsic lock.", Thread.currentThread().getName());
            }
        };

        runTasksConcurrently(task, 2);
    }

    /**
     * Demonstrates an Extrinsic Lock using ReentrantLock.
     * 
     * Properties:
     * - Explicitly acquired (lock()) and released (unlock()).
     * - Unlock must ALWAYS be in a finally block to prevent deadlocks if an
     * exception occurs.
     * - Supports advanced features: tryLock(), timed tryLock(), interruptible
     * locks, and fairness policies.
     * - More flexible but requires careful coding.
     */
    private static void demonstrateExtrinsicLock() throws InterruptedException {
        Lock extrinsicLock = new ReentrantLock();

        Runnable task = () -> {
            log.info("{} trying to acquire extrinsic lock...", Thread.currentThread().getName());
            extrinsicLock.lock(); // Explicit acquire
            try {
                log.info("{} acquired extrinsic lock!", Thread.currentThread().getName());
                Thread.sleep(500); // Simulate work while holding the lock
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // EXTREMELY IMPORTANT: Always unlock in a finally block!
                extrinsicLock.unlock();
                log.info("{} released extrinsic lock.", Thread.currentThread().getName());
            }
        };

        runTasksConcurrently(task, 2);
    }

    /**
     * Demonstrates an Extrinsic Lock using ReadWriteLock.
     * 
     * Properties:
     * - Separates locks into Read (shared) and Write (exclusive).
     * - Multiple threads can read simultaneously if no thread is writing.
     * - Only one thread can write, blocking all other readers and writers.
     * - Excellent for systems where reads vastly outnumber writes (e.g., caches).
     */
    private static void demonstrateReadWriteLock() throws InterruptedException {
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        Runnable readTask = () -> {
            log.info("{} trying to acquire READ lock...", Thread.currentThread().getName());
            readWriteLock.readLock().lock();
            try {
                log.info("{} acquired READ lock (Shared access)!", Thread.currentThread().getName());
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readWriteLock.readLock().unlock();
                log.info("{} released READ lock.", Thread.currentThread().getName());
            }
        };

        Runnable writeTask = () -> {
            log.info("{} trying to acquire WRITE lock...", Thread.currentThread().getName());
            readWriteLock.writeLock().lock();
            try {
                log.info("{} acquired WRITE lock (Exclusive access)!", Thread.currentThread().getName());
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                readWriteLock.writeLock().unlock();
                log.info("{} released WRITE lock.", Thread.currentThread().getName());
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Start two readers (they will read concurrently)
        executor.submit(readTask);
        executor.submit(readTask);

        Thread.sleep(100); // Ensure readers start first

        // Start a writer (will block until readers finish)
        executor.submit(writeTask);

        Thread.sleep(100);

        // Start another reader (will block until writer finishes)
        executor.submit(readTask);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void runTasksConcurrently(Runnable task, int numThreads) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            executor.submit(task);
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
