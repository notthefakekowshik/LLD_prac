package com.lldprep.cache;

import com.lldprep.cache.exception.CacheFullException;
import com.lldprep.cache.model.CacheEntry;
import com.lldprep.cache.policy.EvictionPolicy;
import com.lldprep.cache.storage.Storage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe in-memory cache with pluggable eviction policy (Strategy pattern) and TTL support.
 *
 * <pre>
 * Design:
 *   - Storage    : holds the actual key→entry map (HashMapStorage)
 *   - EvictionPolicy : decides which key to evict when full (LRU, LFU, ...)
 *   - TTL cleanup : background daemon thread removes expired entries periodically
 *
 * classDiagram
 *   class Cache~K,V~ {
 *     -Storage~K,V~ storage
 *     -EvictionPolicy~K~ evictionPolicy
 *     -long ttlMillis
 *     +put(K, V)
 *     +get(K) V
 *     +remove(K)
 *     +shutdown()
 *   }
 *   Cache --> Storage
 *   Cache --> EvictionPolicy
 *   Storage <|.. HashMapStorage
 *   EvictionPolicy <|.. LRUEvictionPolicy
 *   EvictionPolicy <|.. LFUEvictionPolicy
 * </pre>
 */
public class Cache<K, V> {

    private final Storage<K, V> storage;
    private final EvictionPolicy<K> evictionPolicy;
    private final long ttlMillis;
    private final ScheduledExecutorService cleanupService;

    public Cache(Storage<K, V> storage, EvictionPolicy<K> evictionPolicy, long ttlMillis) {
        this.storage = storage;
        this.evictionPolicy = evictionPolicy;
        this.ttlMillis = ttlMillis;
        this.cleanupService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-cleanup-thread");
            t.setDaemon(true);
            return t;
        });
        cleanupService.scheduleAtFixedRate(this::evictExpiredEntries, ttlMillis, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /** Synchronized so the cleanup thread and put/get don't race on the same entries. */
    private synchronized void evictExpiredEntries() {
        // Snapshot keys to avoid mutating the live set mid-iteration
        List<K> keys = new ArrayList<>(storage.getAllKeys());
        for (K key : keys) {
            CacheEntry<V> entry = storage.get(key);
            if (entry != null && isExpired(entry)) {
                System.out.println("[TTL] Evicting expired key: " + key);
                remove(key);
            }
        }
    }

    private boolean isExpired(CacheEntry<V> entry) {
        return Instant.now().isAfter(entry.getCreatedAt().plusMillis(ttlMillis));
    }

    /** Adds or updates a key. Evicts one entry first if the cache is full. */
    public synchronized void put(K key, V value) {
        CacheEntry<V> entry = new CacheEntry<>(value);
        try {
            storage.add(key, entry);
        } catch (CacheFullException e) {
            K evictedKey = evictionPolicy.evictKey();
            if (evictedKey == null) throw new IllegalStateException("Cache full and no eviction candidate");
            System.out.println("[EVICT] Removing key: " + evictedKey);
            storage.remove(evictedKey);
            storage.add(key, entry); // guaranteed to succeed — one slot freed
        }
        evictionPolicy.keyAccessed(key);
    }

    /** Returns the value, or null if the key is absent or expired. */
    public synchronized V get(K key) {
        CacheEntry<V> entry = storage.get(key);
        if (entry == null) return null;

        if (isExpired(entry)) {
            System.out.println("[TTL] Key expired on access: " + key);
            remove(key);
            return null;
        }

        entry.updateLastAccessed();
        evictionPolicy.keyAccessed(key);
        return entry.getValue();
    }

    /** Removes a key from both storage and the eviction policy's tracking. */
    public synchronized void remove(K key) {
        storage.remove(key);
        evictionPolicy.removeKey(key);
    }

    /** Shuts down the background cleanup thread. Call when the cache is no longer needed. */
    public void shutdown() {
        cleanupService.shutdown();
    }
}
