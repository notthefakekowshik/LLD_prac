package com.lldprep.systems.lfucache.model;

/**
 * Represents a single pending write to the backing store.
 * Produced by LFUCache.put() and consumed by WriteBehindBuffer's flusher thread.
 */
public record WriteBehindTask<K, V>(K key, V value, long enqueuedAt) {

    public WriteBehindTask(K key, V value) {
        this(key, value, System.currentTimeMillis());
    }
}
