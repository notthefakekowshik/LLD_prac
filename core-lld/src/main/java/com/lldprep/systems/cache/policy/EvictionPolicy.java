package com.lldprep.systems.cache.policy;

/**
 * Strategy interface for cache eviction. Implement this to plug in any eviction algorithm
 * (LRU, LFU, FIFO, ...) without touching the Cache core.
 */
public interface EvictionPolicy<K> {
    /** Called every time a key is read or written. Used to track access order/frequency. */
    void keyAccessed(K key);

    /** Returns the key that should be evicted next, and removes it from internal tracking. */
    K evictKey();

    /** Removes a key from tracking (called on manual removal or TTL expiry). */
    void removeKey(K key);
}
