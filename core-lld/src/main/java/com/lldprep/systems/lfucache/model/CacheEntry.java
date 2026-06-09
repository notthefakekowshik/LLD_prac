package com.lldprep.systems.lfucache.model;

/**
 * Wraps a cached value with per-entry TTL support.
 * Value is mutable so cache.put() on an existing key can update in-place without eviction.
 */
public final class CacheEntry<V> {

    private V value;
    private final long expiresAt; // epoch millis; -1 = no expiry

    public CacheEntry(V value, long ttlMillis) {
        this.value = value;
        this.expiresAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : -1;
    }

    public boolean isExpired() {
        return expiresAt >= 0 && System.currentTimeMillis() > expiresAt;
    }

    public V getValue()            { return value; }
    public void setValue(V value)  { this.value = value; }
}
