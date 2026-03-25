package com.lldprep.cache.storage;

import com.lldprep.cache.exception.CacheFullException;
import com.lldprep.cache.model.CacheEntry;
import java.util.Set;

/**
 * Contract for the physical key→entry store. Swap implementations (HashMap, off-heap, etc.)
 * without touching Cache or eviction logic.
 */
public interface Storage<K, V> {
    /** Stores the entry. Throws CacheFullException if at capacity and key is not already present. */
    void add(K key, CacheEntry<V> value) throws CacheFullException;

    void remove(K key);

    /** Returns null if the key does not exist. */
    CacheEntry<V> get(K key);

    int size();

    /** Returns a snapshot-safe view of all keys (used for TTL scanning). */
    Set<K> getAllKeys();
}
