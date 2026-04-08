package com.kowshik.patterns;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Concurrent Cache Demo - High-Performance Caching Implementation
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. Why build a custom concurrent cache?
 *    - Control over eviction policies
 *    - Automatic expiration handling
 *    - Statistics tracking
 *    - Thread-safe without ConcurrentHashMap limitations
 *
 * 2. Common Interview Questions:
 *    - How to implement thread-safe cache?
 *    - Cache eviction strategies (LRU, LFU, TTL)
 *    - Difference between ConcurrentHashMap and synchronized Map?
 *    - How to handle cache stampede?
 *    - Explain cache-aside vs write-through patterns
 *    - How to prevent memory leaks in cache?
 *
 * 3. Cache Eviction Policies:
 *    - LRU (Least Recently Used): Remove oldest accessed
 *    - LFU (Least Frequently Used): Remove least accessed
 *    - TTL (Time To Live): Expire after time
 *    - Size-based: Remove when size limit reached
 *
 * 4. Concurrency Strategies:
 *    - ReadWriteLock: Multiple readers, single writer
 *    - ConcurrentHashMap: Lock striping for better performance
 *    - Atomic operations for counters
 *    - Background cleanup threads
 *
 * 5. Real-world Use Cases:
 *    - Database query results
 *    - API response caching
 *    - Session management
 *    - Configuration data
 *
 * Demonstrates a concurrent cache implementation with:
 * - Read-write lock for thread-safe access
 * - Automatic entry expiration
 * - Cache statistics
 * - Background cleanup of expired entries
 * - Custom eviction policies
 */
public class ConcurrentCacheDemo<K, V> {
    private final Map<K, CacheEntry<V>> cache;
    private final ReadWriteLock lock;
    private final ScheduledExecutorService cleanupExecutor;
    private final long defaultExpirationMs;
    private final int maxSize;
    private final CacheStats stats;

    /**
     * Represents a single cache entry with value and expiration metadata.
     *
     * @param <V> the type of the cached value
     */
    static class CacheEntry<V> {
        final V value;
        final long expirationTime;
        final long creationTime;

        CacheEntry(V value, long expirationTimeMs) {
            this.value = value;
            this.creationTime = System.currentTimeMillis();
            this.expirationTime = this.creationTime + expirationTimeMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    /**
     * Tracks cache performance metrics (hits, misses, evictions).
     */
    static class CacheStats {
        private final ConcurrentHashMap<String, Long> metrics = new ConcurrentHashMap<>();

        void incrementHits() {
            metrics.merge("hits", 1L, Long::sum);
        }

        void incrementMisses() {
            metrics.merge("misses", 1L, Long::sum);
        }

        void incrementEvictions() {
            metrics.merge("evictions", 1L, Long::sum);
        }

        Map<String, Long> getMetrics() {
            return new ConcurrentHashMap<>(metrics);
        }
    }

    public ConcurrentCacheDemo(int maxSize, long defaultExpirationMs) {
        this.cache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.maxSize = maxSize;
        this.defaultExpirationMs = defaultExpirationMs;
        this.stats = new CacheStats();

        // Setup periodic cleanup of expired entries
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-cleanup-thread");
            t.setDaemon(true);
            return t;
        });

        scheduleCleanup();
    }

    public V get(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry == null) {
                stats.incrementMisses();
                return null;
            }

            if (entry.isExpired()) {
                stats.incrementMisses();
                // Schedule async removal of expired entry
                CompletableFuture.runAsync(() -> remove(key));
                return null;
            }

            stats.incrementHits();
            return entry.value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        put(key, value, defaultExpirationMs);
    }

    public void put(K key, V value, long expirationMs) {
        lock.writeLock().lock();
        try {
            ensureCapacity();
            cache.put(key, new CacheEntry<>(value, expirationMs));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void ensureCapacity() {
        if (cache.size() >= maxSize) {
            // Evict oldest entry
            K oldestKey = cache.entrySet().stream()
                .min((e1, e2) -> Long.compare(
                    e1.getValue().creationTime,
                    e2.getValue().creationTime))
                .map(Map.Entry::getKey)
                .orElse(null);

            if (oldestKey != null) {
                cache.remove(oldestKey);
                stats.incrementEvictions();
            }
        }
    }

    public V remove(K key) {
        lock.writeLock().lock();
        try {
            CacheEntry<V> entry = cache.remove(key);
            return entry != null ? entry.value : null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void scheduleCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            lock.writeLock().lock();
            try {
                cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            } finally {
                lock.writeLock().unlock();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public Map<String, Long> getStats() {
        return stats.getMetrics();
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
    }

    // Example usage
    public static void main(String[] args) throws InterruptedException {
        ConcurrentCacheDemo<String, String> cache = new ConcurrentCacheDemo<>(100, 5000);

        // Simulate concurrent access
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 10; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    // Writer thread
                    cache.put("key" + id, "value" + id);
                    TimeUnit.MILLISECONDS.sleep(100);

                    // Reader thread
                    String value = cache.get("key" + id);
                    System.out.println("Read value: " + value);

                    TimeUnit.MILLISECONDS.sleep(6000); // Wait for expiration
                    value = cache.get("key" + id);
                    System.out.println("After expiration: " + value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Cache stats: " + cache.getStats());
        cache.shutdown();
    }
}
