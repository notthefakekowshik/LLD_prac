package com.lldprep.systems.lfucache.demo;

import com.lldprep.systems.lfucache.LFUCache;
import com.lldprep.systems.lfucache.policy.O1LFUEvictionPolicy;
import com.lldprep.systems.lfucache.store.InMemoryBackingStore;
import com.lldprep.systems.lfucache.writebehind.WriteBehindBuffer;

public class LFUCacheDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  LFU Cache — Apple Interview: TTL + Write-Behind ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        // ── Demo 1: Basic LFU eviction (O(1)) ─────────────────────────────
        System.out.println("\n── Demo 1: LFU Eviction (capacity = 3) ──");
        InMemoryBackingStore<String, String> store = new InMemoryBackingStore<>();
        WriteBehindBuffer<String, String> wbb = new WriteBehindBuffer<>(store, 10);
        LFUCache<String, String> cache = new LFUCache<>(3, new O1LFUEvictionPolicy<>(), wbb, 5000);

        cache.put("a", "Alice",   60_000);
        cache.put("b", "Bob",     60_000);
        cache.put("c", "Charlie", 60_000);
        // frequencies: a=1, b=1, c=1

        cache.get("a");  // a=2
        cache.get("a");  // a=3
        cache.get("b");  // b=2
        // frequencies: a=3, b=2, c=1  → "c" is LFU

        System.out.println("  Before eviction: size=" + cache.size());
        cache.put("d", "Dave", 60_000);  // evicts "c" (LFU, freq=1)
        System.out.println("  After adding 'd' (evicts 'c'):");
        System.out.println("    c=" + cache.get("c")); // null — evicted
        System.out.println("    d=" + cache.get("d")); // Dave
        System.out.println("    a=" + cache.get("a")); // Alice  (survived, freq=3)

        // ── Demo 2: LFU tie-break → LRU ───────────────────────────────────
        System.out.println("\n── Demo 2: Tie-break — LRU within same frequency ──");
        InMemoryBackingStore<String, Integer> store2 = new InMemoryBackingStore<>();
        WriteBehindBuffer<String, Integer> wbb2 = new WriteBehindBuffer<>(store2, 10);
        LFUCache<String, Integer> cache2 = new LFUCache<>(3, new O1LFUEvictionPolicy<>(), wbb2, 5000);

        cache2.put("x", 10, 60_000);
        cache2.put("y", 20, 60_000);
        cache2.put("z", 30, 60_000);
        // all freq=1; "x" is oldest (inserted first → LRU victim)

        cache2.put("w", 40, 60_000);  // evicts "x" (same freq=1 as y,z; LRU tiebreak)
        System.out.println("    x=" + cache2.get("x")); // null — evicted (oldest, freq=1)
        System.out.println("    y=" + cache2.get("y")); // 20   — survived
        System.out.println("    w=" + cache2.get("w")); // 40   — survived

        // ── Demo 3: Per-entry TTL ──────────────────────────────────────────
        // Fresh cache so capacity pressure doesn't evict the TTL test keys immediately
        System.out.println("\n── Demo 3: Per-Entry TTL ──");
        InMemoryBackingStore<String, String> store3 = new InMemoryBackingStore<>();
        WriteBehindBuffer<String, String> wbb3 = new WriteBehindBuffer<>(store3, 10);
        LFUCache<String, String> ttlCache = new LFUCache<>(5, new O1LFUEvictionPolicy<>(), wbb3, 5000);

        ttlCache.put("short-lived", "expires-soon", 200);  // 200ms TTL
        ttlCache.put("long-lived",  "stays",        60_000); // 60s TTL

        System.out.println("  short-lived (immediate)= " + ttlCache.get("short-lived")); // expires-soon
        Thread.sleep(300);
        System.out.println("  short-lived (after 300ms)= " + ttlCache.get("short-lived")); // null
        System.out.println("  long-lived  (after 300ms)= " + ttlCache.get("long-lived"));  // stays
        ttlCache.shutdown();

        // ── Demo 4: Update path — frequency increments, not reset ─────────
        System.out.println("\n── Demo 4: Update does not reset frequency ──");
        InMemoryBackingStore<String, String> store4 = new InMemoryBackingStore<>();
        WriteBehindBuffer<String, String> wbb4 = new WriteBehindBuffer<>(store4, 10);
        LFUCache<String, String> cache3 = new LFUCache<>(2, new O1LFUEvictionPolicy<>(), wbb4, 5000);

        cache3.put("p", "v1", 60_000);
        cache3.get("p");  // freq p=2
        cache3.get("p");  // freq p=3
        cache3.put("q", "v1", 60_000); // freq q=1
        cache3.put("p", "v2", 60_000); // UPDATE: freq p=4, not reset to 1
        // Adding "r": must evict → "q" (freq=1) not "p" (freq=4)
        cache3.put("r", "v1", 60_000);
        System.out.println("    p=" + cache3.get("p")); // v2   — survived (high freq)
        System.out.println("    q=" + cache3.get("q")); // null — evicted (freq=1)
        System.out.println("    r=" + cache3.get("r")); // v1   — survived

        // ── Demo 5: Write-behind async flush ──────────────────────────────
        System.out.println("\n── Demo 5: Write-Behind (async flush) ──");
        System.out.println("  Writes enqueued to cache — backing store may lag:");
        for (int i = 0; i < 5; i++) {
            cache.put("key" + i, "val" + i, 60_000);
        }
        System.out.println("  Cache put() returned immediately. BackingStore writes:");
        Thread.sleep(500); // let flusher drain
        System.out.println("  Total BackingStore writes (store1): " + store.getWriteCount());

        cache.shutdown();
        cache2.shutdown();
        cache3.shutdown();
    }
}
