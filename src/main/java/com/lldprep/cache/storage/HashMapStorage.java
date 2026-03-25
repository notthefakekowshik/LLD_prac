package com.lldprep.cache.storage;

import com.lldprep.cache.exception.CacheFullException;
import com.lldprep.cache.model.CacheEntry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

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
            throw new CacheFullException("Cache is full at capacity " + capacity);
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
        return storageMap.keySet();
    }
}
