package com.lldprep.systems.lfucache.policy;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * O(1) LFU eviction — evicts the least-frequently used key.
 * Tie-break: LRU (oldest key within the same frequency bucket is evicted first).
 *
 * Why O(1): three data structures work together:
 *   keyFreq      : key → current frequency
 *   freqBuckets  : frequency → insertion-ordered set of keys at that frequency
 *   minFreq      : tracks the lowest occupied frequency bucket at all times
 *
 * Invariant: after any put() of a NEW key, minFreq is always 1.
 *            After any get(), minFreq may increase by 1.
 *
 * vs existing LFUEvictionPolicy: that uses Collections.min() — O(n) scan.
 * This impl is O(1) for all three operations: keyAccessed, evictKey, removeKey.
 */
public class O1LFUEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Map<K, Integer> keyFreq = new HashMap<>();
    private final Map<Integer, LinkedHashSet<K>> freqBuckets = new HashMap<>();
    private int minFreq = 0;

    @Override
    public synchronized void keyAccessed(K key) {
        int freq = keyFreq.getOrDefault(key, 0);

        if (freq > 0) {
            // Remove from current bucket; advance minFreq if that bucket is now empty
            LinkedHashSet<K> bucket = freqBuckets.get(freq);
            bucket.remove(key);
            if (bucket.isEmpty()) {
                freqBuckets.remove(freq);
                if (minFreq == freq) minFreq = freq + 1;
            }
        } else {
            // New key insertion: minimum frequency resets to 1
            // CRITICAL SECTION — minFreq invariant must hold after every new insert
            minFreq = 1;
        }

        keyFreq.put(key, freq + 1);
        freqBuckets.computeIfAbsent(freq + 1, k -> new LinkedHashSet<>()).add(key);
    }

    @Override
    public synchronized K evictKey() {
        LinkedHashSet<K> minBucket = freqBuckets.get(minFreq);
        if (minBucket == null || minBucket.isEmpty()) return null;
        K victim = minBucket.iterator().next(); // LRU tiebreak: oldest entry in bucket
        removeKey(victim);
        return victim;
    }

    @Override
    public synchronized void removeKey(K key) {
        Integer freq = keyFreq.remove(key);
        if (freq == null) return;
        LinkedHashSet<K> bucket = freqBuckets.get(freq);
        if (bucket != null) {
            bucket.remove(key);
            if (bucket.isEmpty()) freqBuckets.remove(freq);
        }
    }
}
