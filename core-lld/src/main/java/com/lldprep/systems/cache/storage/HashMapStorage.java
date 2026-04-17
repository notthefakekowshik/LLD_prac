package com.lldprep.systems.cache.storage;

import com.lldprep.systems.cache.exception.CacheFullException;
import com.lldprep.systems.cache.model.CacheEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMap-backed storage. Thread-safe for individual map operations.
 * Capacity enforcement is handled here; eviction decisions are the policy's responsibility.
 */
public class HashMapStorage<K, V> implements Storage<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<V>> storageMap;
    private final int capacity;

    public HashMapStorage(int capacity) {
        this.capacity = capacity;
        this.storageMap = new ConcurrentHashMap<>();
    }

    @Override
    public void add(K key, CacheEntry<V> value) throws CacheFullException {
        if (storageMap.size() >= capacity && !storageMap.containsKey(key)) {
            throw new CacheFullException("Cache is full. Capacity: " + capacity);
        }
        storageMap.put(key, value);
    }

    @Override
    public void remove(K key) {
        storageMap.remove(key);
    }

    @Override
    public CacheEntry<V> get(K key) {
        return storageMap.get(key);
    }

    @Override
    public int size() {
        return storageMap.size();
    }

    @Override
    public Set<K> getAllKeys() {
        return new HashSet<>(storageMap.keySet()); // snapshot to avoid CME during TTL scan
    }
}
