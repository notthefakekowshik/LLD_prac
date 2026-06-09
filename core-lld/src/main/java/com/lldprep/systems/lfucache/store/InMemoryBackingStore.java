package com.lldprep.systems.lfucache.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Simulates a DB for demo purposes. Tracks write count to prove write-behind is async. */
public class InMemoryBackingStore<K, V> implements BackingStore<K, V> {

    private final Map<K, V> store = new ConcurrentHashMap<>();
    private final AtomicInteger writeCount = new AtomicInteger(0);

    @Override
    public void write(K key, V value) {
        store.put(key, value);
        System.out.printf("  [BackingStore] persisted key=%-10s  total_writes=%d%n",
                key, writeCount.incrementAndGet());
    }

    @Override
    public V read(K key) {
        return store.get(key);
    }

    public int getWriteCount() { return writeCount.get(); }
}
