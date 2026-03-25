package com.lldprep.cache.policy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Least Frequently Used eviction: evicts the key that has been accessed the fewest times.
 *
 * Evolve step — added without modifying Cache, Storage, or LRUEvictionPolicy.
 * This demonstrates OCP: the eviction algorithm is swapped by injecting a different strategy.
 *
 * Trade-off vs LRU: LFU keeps "popular" items longer, but a key accessed heavily early
 * and then forgotten will linger. LRU handles recency better; LFU handles frequency better.
 */
public class LFUEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Map<K, Integer> frequencyMap = new HashMap<>();

    @Override
    public synchronized void keyAccessed(K key) {
        frequencyMap.merge(key, 1, Integer::sum); // increment access count
    }

    @Override
    public synchronized K evictKey() {
        if (frequencyMap.isEmpty()) return null;
        // Find the entry with the minimum frequency
        K lfuKey = Collections.min(frequencyMap.entrySet(), Map.Entry.comparingByValue()).getKey();
        frequencyMap.remove(lfuKey);
        return lfuKey;
    }

    @Override
    public synchronized void removeKey(K key) {
        frequencyMap.remove(key);
    }
}
