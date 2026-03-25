package com.lldprep.cache;

import com.lldprep.cache.exception.CacheFullException;
import com.lldprep.cache.model.CacheEntry;
import com.lldprep.cache.policy.EvictionPolicy;
import com.lldprep.cache.storage.Storage;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        startCleanupTask();
    }

    private void startCleanupTask() {
        cleanupService.scheduleAtFixedRate(this::evictExpiredEntries, ttlMillis, ttlMillis, TimeUnit.MILLISECONDS);
    }

    private void evictExpiredEntries() {
        System.out.println("Running background TTL cleanup...");
        for (K key : storage.getAllKeys()) {
            CacheEntry<V> entry = storage.get(key);
            if (entry != null && isExpired(entry)) {
                System.out.println("Evicting expired key: " + key);
                remove(key);
            }
        }
    }

    private boolean isExpired(CacheEntry<V> entry) {
        return Instant.now().isAfter(entry.getCreatedAt().plusMillis(ttlMillis));
    }

    public synchronized void put(K key, V value) {
        CacheEntry<V> entry = new CacheEntry<>(value);
        try {
            storage.add(key, entry);
            evictionPolicy.keyAccessed(key);
        } catch (CacheFullException e) {
            System.out.println("Cache full, evicting...");
            K evictedKey = evictionPolicy.evictKey();
            if (evictedKey != null) {
                storage.remove(evictedKey);
                put(key, value); // Recursive call after eviction
            }
        }
    }

    public synchronized V get(K key) {
        CacheEntry<V> entry = storage.get(key);
        if (entry == null) return null;

        if (isExpired(entry)) {
            System.out.println("Key expired during access: " + key);
            remove(key);
            return null;
        }

        entry.updateLastAccessed();
        evictionPolicy.keyAccessed(key);
        return entry.getValue();
    }

    public synchronized void remove(K key) {
        storage.remove(key);
        evictionPolicy.removeKey(key);
    }

    public void shutdown() {
        cleanupService.shutdown();
    }
}
