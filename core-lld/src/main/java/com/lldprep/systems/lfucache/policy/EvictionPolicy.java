package com.lldprep.systems.lfucache.policy;

/**
 * Strategy interface for cache eviction. Plug in any algorithm without touching LFUCache.
 */
public interface EvictionPolicy<K> {
    /** Called on every read or write. Used to track access order or frequency. */
    void keyAccessed(K key);

    /** Returns the key to evict next, removing it from internal tracking. */
    K evictKey();

    /** Removes a key from tracking — called on TTL expiry or manual remove(). */
    void removeKey(K key);
}
