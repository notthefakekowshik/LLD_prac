package com.lldprep.systems.lfucache;

import com.lldprep.systems.lfucache.exception.CacheException;
import com.lldprep.systems.lfucache.model.CacheEntry;
import com.lldprep.systems.lfucache.policy.EvictionPolicy;
import com.lldprep.systems.lfucache.writebehind.WriteBehindBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * LFU cache with per-entry TTL and write-behind persistence.
 *
 * Three concerns are delegated to injected collaborators (DIP):
 *   EvictionPolicy    — which key to remove when full (Strategy pattern)
 *   WriteBehindBuffer — async flush to BackingStore after every put
 *   TTL cleanup       — background daemon that sweeps expired entries
 *
 * Concurrency: a single `synchronized` monitor on this instance guards both the
 * store map and the eviction policy's internal state. The write-behind buffer
 * is lock-free on the enqueue path (BlockingQueue.offer).
 */
public class LFUCache<K, V> {

    private final int capacity;
    private final Map<K, CacheEntry<V>> store;
    private final EvictionPolicy<K> evictionPolicy;
    private final WriteBehindBuffer<K, V> writeBehindBuffer;
    private final ScheduledExecutorService ttlSweep;

    public LFUCache(int capacity,
                    EvictionPolicy<K> evictionPolicy,
                    WriteBehindBuffer<K, V> writeBehindBuffer,
                    long ttlSweepIntervalMillis) {
        if (capacity <= 0) throw new CacheException("Capacity must be > 0");
        this.capacity = capacity;
        this.store = new ConcurrentHashMap<>();
        this.evictionPolicy = evictionPolicy;
        this.writeBehindBuffer = writeBehindBuffer;
        this.ttlSweep = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lfu-ttl-sweep");
            t.setDaemon(true);
            return t;
        });
        ttlSweep.scheduleAtFixedRate(
            this::evictExpiredEntries,
            ttlSweepIntervalMillis,
            ttlSweepIntervalMillis,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Inserts or updates a key-value pair with a per-entry TTL.
     *
     * Update path: value is replaced in-place; frequency is incremented (not reset to 1).
     * Insert path: evicts LFU entry if at capacity, then inserts with freq=1.
     * Write-behind: enqueued immediately after every put regardless of insert vs update.
     */
    public synchronized void put(K key, V value, long ttlMillis) {
        if (store.containsKey(key)) {
            store.get(key).setValue(value);
            evictionPolicy.keyAccessed(key); // freq++, not reset
        } else {
            if (store.size() >= capacity) {
                K victim = evictionPolicy.evictKey();
                if (victim == null) throw new CacheException("Eviction returned null at capacity " + capacity);
                store.remove(victim);
            }
            store.put(key, new CacheEntry<>(value, ttlMillis));
            evictionPolicy.keyAccessed(key); // freq=1, minFreq reset to 1
        }
        writeBehindBuffer.enqueue(key, value);
    }

    /** Returns null on cache miss, absent key, or expired entry. */
    public synchronized V get(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null) return null;
        if (entry.isExpired()) {
            System.out.println("  [TTL] Key expired on access: " + key);
            evict(key);
            return null;
        }
        evictionPolicy.keyAccessed(key);
        return entry.getValue();
    }

    public synchronized void remove(K key) {
        evict(key);
    }

    public synchronized int size() {
        return store.size();
    }

    private void evict(K key) {
        store.remove(key);
        evictionPolicy.removeKey(key);
    }

    // CRITICAL SECTION — shared mutable state: store + eviction policy
    private synchronized void evictExpiredEntries() {
        List<K> snapshot = new ArrayList<>(store.keySet());
        for (K key : snapshot) {
            CacheEntry<V> entry = store.get(key);
            if (entry != null && entry.isExpired()) {
                System.out.println("  [TTL-SWEEP] Evicting: " + key);
                evict(key);
            }
        }
    }

    public void shutdown() {
        ttlSweep.shutdown();
        writeBehindBuffer.shutdown();
    }
}
