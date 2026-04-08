package com.lldprep.cache.model;

import java.time.Instant;

/**
 * Wraps a cached value with metadata needed for TTL checks and access tracking.
 * Immutable except for lastAccessedAt, which is updated on every read.
 */
public class CacheEntry<V> {

    private final V value;
    private final Instant createdAt;
    private Instant lastAccessedAt;

    public CacheEntry(V value) {
        this.value = value;
        this.createdAt = Instant.now();
        this.lastAccessedAt = this.createdAt;
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    public V getValue()                  { return value; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getLastAccessedAt()   { return lastAccessedAt; }
}
