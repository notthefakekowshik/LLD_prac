package com.lldprep.cache.policy;

import java.util.LinkedHashSet;

public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {
    private final LinkedHashSet<K> keyOrder;

    public LRUEvictionPolicy() {
        this.keyOrder = new LinkedHashSet<>();
    }

    @Override
    public synchronized void keyAccessed(K key) {
        keyOrder.remove(key);
        keyOrder.add(key);
    }

    @Override
    public synchronized K evictKey() {
        if (keyOrder.isEmpty()) return null;
        K key = keyOrder.iterator().next();
        keyOrder.remove(key);
        return key;
    }

    @Override
    public synchronized void removeKey(K key) {
        keyOrder.remove(key);
    }
}
