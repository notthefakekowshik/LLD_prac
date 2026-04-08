package com.lldprep.cache;

import com.lldprep.cache.policy.LRUEvictionPolicy;
import com.lldprep.cache.storage.HashMapStorage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {

    @Test
    public void testCacheOperations() {
        Cache<Integer, String> cache = new Cache<>(new HashMapStorage<>(2), new LRUEvictionPolicy<>(), 10000);

        cache.put(1, "A");
        cache.put(2, "B");
        assertEquals("A", cache.get(1));

        cache.put(3, "C"); // This should evict key 2 (LRU was 2)
        assertNull(cache.get(2));
        assertEquals("A", cache.get(1));
        assertEquals("C", cache.get(3));

        cache.shutdown();
    }

    @Test
    public void testTTLExpiration() throws InterruptedException {
        // Cache with 500ms TTL
        Cache<Integer, String> cache = new Cache<>(new HashMapStorage<>(2), new LRUEvictionPolicy<>(), 500);

        cache.put(1, "A");
        assertEquals("A", cache.get(1));

        Thread.sleep(600); // Wait for expiration

        assertNull(cache.get(1)); // Should be null due to expiration during access
        cache.shutdown();
    }
}
