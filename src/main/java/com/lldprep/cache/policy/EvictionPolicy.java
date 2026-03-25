package com.lldprep.cache.policy;

public interface EvictionPolicy<K> {
    void keyAccessed(K key);
    K evictKey();
    void removeKey(K key); // Added for manual removal (e.g., TTL expiration)
}
