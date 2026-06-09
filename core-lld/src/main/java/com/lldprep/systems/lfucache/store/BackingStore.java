package com.lldprep.systems.lfucache.store;

/**
 * Abstraction over the durable backing store (DB, Redis, disk).
 * Write-behind flushes to this interface asynchronously; cache reads never hit it.
 */
public interface BackingStore<K, V> {
    void write(K key, V value);
    V read(K key);
}
