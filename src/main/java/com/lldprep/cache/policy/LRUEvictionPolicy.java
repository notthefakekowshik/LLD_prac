package com.lldprep.cache.policy;

import java.util.LinkedHashSet;

/**
 * Least Recently Used eviction: evicts the key that has not been accessed for the longest time.
 *
 * LinkedHashSet preserves insertion order. On every access, the key is moved to the tail.
 * The head is always the least recently used candidate.
 */
public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {

    private final LinkedHashSet<K> keyOrder = new LinkedHashSet<>();

    @Override
    public synchronized void keyAccessed(K key) {
        keyOrder.remove(key); // remove from current position
        keyOrder.add(key);    // re-insert at tail (most recently used)
    }

    @Override
    public synchronized K evictKey() {
        if (keyOrder.isEmpty()) return null;
        K lruKey = keyOrder.iterator().next(); // head = least recently used
        keyOrder.remove(lruKey);
        return lruKey;
    }

    @Override
    public synchronized void removeKey(K key) {
        keyOrder.remove(key);
    }
}
