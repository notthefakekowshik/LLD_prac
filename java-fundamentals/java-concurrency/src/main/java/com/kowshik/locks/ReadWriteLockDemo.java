package com.kowshik.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ReadWriteLock Demo - Concurrent Read/Write Access Tutorial
 *
 * ReadWriteLock allows multiple concurrent readers OR one exclusive writer.
 * Provides better performance than synchronized when reads are more frequent.
 *
 * Key Points:
 * - Multiple threads can hold read lock simultaneously
 * - Only one thread can hold write lock (exclusive)
 * - Write lock excludes both readers and writers
 * - Read lock blocks only writers
 *
 * Use Cases:
 * - Caching systems
 * - Configuration data (frequent reads, rare updates)
 * - Shared data structures with read-heavy workloads
 * - Resource registries
 *
 * Writer Starvation:
 * - Occurs when a continuous stream of readers acquire the read lock.
 * - Because readers don't block other readers, a waiting writer can be delayed
 * indefinitely.
 * - ReentrantReadWriteLock can be configured with a fairness policy (fair=true)
 * to prevent this by granting locks in arrival order, though it may reduce
 * overall throughput.
 */
public class ReadWriteLockDemo {

    private static final Logger logger = LoggerFactory.getLogger(ReadWriteLockDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== ReadWriteLock Demo ===\n");

        // Demo 1: Basic read/write lock
        basicReadWriteLockDemo();

        Thread.sleep(2000);

        // Demo 2: Cache implementation
        cacheDemo();

        Thread.sleep(2000);

        // Demo 3: Writer Starvation
        writerStarvationDemo();
    }

    /**
     * Demo 1: Basic read/write lock behavior
     */
    private static void basicReadWriteLockDemo() throws InterruptedException {
        logger.info("Demo 1: Basic ReadWriteLock behavior");

        SharedResource resource = new SharedResource();
        ExecutorService executor = Executors.newFixedThreadPool(6);

        // Submit 4 readers
        for (int i = 1; i <= 4; i++) {
            final int readerId = i;
            executor.submit(() -> {
                resource.read(readerId);
            });
        }

        Thread.sleep(500); // Let readers start

        // Submit 2 writers
        for (int i = 1; i <= 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                resource.write(writerId, "data-" + writerId);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        logger.info("");
    }

    /**
     * Demo 2: Cache implementation with ReadWriteLock
     */
    private static void cacheDemo() throws InterruptedException {
        logger.info("Demo 2: Thread-safe cache with ReadWriteLock");

        ThreadSafeCache<String, String> cache = new ThreadSafeCache<>();
        ExecutorService executor = Executors.newFixedThreadPool(8);

        // Populate cache
        cache.put("user:1", "Alice");
        cache.put("user:2", "Bob");

        // Many readers
        for (int i = 1; i <= 5; i++) {
            final int readerId = i;
            executor.submit(() -> {
                String key = "user:" + (readerId % 2 == 0 ? 1 : 2);
                String value = cache.get(key, readerId);
                logger.info("Reader {} got value: {}", readerId, value);
            });
        }

        // Few writers
        for (int i = 1; i <= 3; i++) {
            final int writerId = i;
            executor.submit(() -> {
                cache.put("user:" + writerId, "User-" + writerId);
                logger.info("Writer {} updated cache", writerId);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Demo 3: Demonstrates Writer Starvation
     * A continuous stream of readers can block a writer indefinitely
     * if the ReadWriteLock is not fair.
     */
    private static void writerStarvationDemo() throws InterruptedException {
        logger.info("Demo 3: Writer Starvation Demonstration");

        // Non-fair lock allows readers to jump ahead of a waiting writer
        ReadWriteLock lock = new ReentrantReadWriteLock(false);
        ExecutorService executor = Executors.newCachedThreadPool();

        // 1. Start a fleet of readers that constantly acquire and release the read lock
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    lock.readLock().lock();
                    try {
                        Thread.sleep(10); // Hold lock to overlap with other readers
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } finally {
                        lock.readLock().unlock();
                    }
                }
            });
        }

        // Give readers a moment to start overlapping
        Thread.sleep(100);

        logger.info("Writer trying to acquire write lock...");
        long startTime = System.currentTimeMillis();

        // 2. Start a single writer
        Thread writerThread = new Thread(() -> {
            lock.writeLock().lock();
            try {
                long waitTime = System.currentTimeMillis() - startTime;
                logger.info("Writer FINALLY acquired lock after {} ms!", waitTime);
            } finally {
                lock.writeLock().unlock();
            }
        });

        writerThread.start();

        // Wait up to 2 seconds to see if the writer gets the lock
        writerThread.join(2000);

        if (writerThread.isAlive()) {
            logger.warn("Writer is STILL waiting after {} ms! This is WRITER STARVATION.",
                    (System.currentTimeMillis() - startTime));
            writerThread.interrupt(); // Clean up the waiting writer
        }

        executor.shutdownNow();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        logger.info("");
    }

    /**
     * Shared resource with ReadWriteLock
     */
    static class SharedResource {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private String data = "initial-data";

        public void read(int readerId) {
            lock.readLock().lock();
            try {
                logger.info("Reader {} acquired read lock. Reading: {}", readerId, data);
                Thread.sleep(1000); // Simulate read operation
                logger.info("Reader {} finished reading", readerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.readLock().unlock();
                logger.info("Reader {} released read lock", readerId);
            }
        }

        public void write(int writerId, String newData) {
            lock.writeLock().lock();
            try {
                logger.info("Writer {} acquired write lock. Writing: {}", writerId, newData);
                Thread.sleep(1500); // Simulate write operation
                this.data = newData;
                logger.info("Writer {} finished writing", writerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.writeLock().unlock();
                logger.info("Writer {} released write lock", writerId);
            }
        }
    }

    /**
     * Thread-safe cache implementation using ReadWriteLock
     */
    static class ThreadSafeCache<K, V> {
        private final Map<K, V> cache = new HashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        public V get(K key, int readerId) {
            lock.readLock().lock();
            try {
                logger.info("Reader {} acquiring read lock for key: {}", readerId, key);
                Thread.sleep(500); // Simulate cache lookup
                return cache.get(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void put(K key, V value) {
            lock.writeLock().lock();
            try {
                logger.info("Writer acquiring write lock for key: {}", key);
                Thread.sleep(800); // Simulate cache update
                cache.put(key, value);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public V computeIfAbsent(K key, java.util.function.Function<K, V> mappingFunction) {
            // Check with read lock first
            lock.readLock().lock();
            try {
                V value = cache.get(key);
                if (value != null) {
                    return value;
                }
            } finally {
                lock.readLock().unlock();
            }

            // Upgrade to write lock if needed
            lock.writeLock().lock();
            try {
                // Double-check after acquiring write lock
                V value = cache.get(key);
                if (value == null) {
                    value = mappingFunction.apply(key);
                    cache.put(key, value);
                }
                return value;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void clear() {
            lock.writeLock().lock();
            try {
                cache.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int size() {
            lock.readLock().lock();
            try {
                return cache.size();
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * Advanced: Statistics tracking with ReadWriteLock
     */
    static class Statistics {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private long readCount = 0;
        private long writeCount = 0;

        public long getReadCount() {
            lock.readLock().lock();
            try {
                return readCount;
            } finally {
                lock.readLock().unlock();
            }
        }

        public long getWriteCount() {
            lock.readLock().lock();
            try {
                return writeCount;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void incrementReadCount() {
            lock.writeLock().lock();
            try {
                readCount++;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void incrementWriteCount() {
            lock.writeLock().lock();
            try {
                writeCount++;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
