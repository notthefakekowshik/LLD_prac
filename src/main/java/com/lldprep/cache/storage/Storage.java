package com.lldprep.cache.storage;

import com.lldprep.cache.exception.CacheFullException;
import com.lldprep.cache.model.CacheEntry;
import java.util.Set;

public interface Storage<K, V> {
    void add(K key, CacheEntry<V> value) throws CacheFullException;
    void remove(K key);
    CacheEntry<V> get(K key);
    int size();
    Set<K> getAllKeys(); // Added to allow scanning for TTL expiration
}
