# LFU Cache with TTL + Write-Behind

**Apple Coding Interview** — O(1) LFU eviction, per-entry TTL, asynchronous write-behind to backing store.  
**Source:** Reddit · r/developersIndia (seen 2026-06-09)

## Quick Start

```bash
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.lfucache.demo.LFUCacheDemo" -pl core-lld
```

## Features

- **O(1) LFU eviction** — `minFreq` + `freqBuckets` (LinkedHashSet per freq) — not the O(n) `Collections.min()` approach
- **LRU tiebreak** — among equal-frequency keys, the oldest (insertion order) is evicted
- **Per-entry TTL** — each `put(key, value, ttlMillis)` sets its own expiry; not a global TTL
- **Write-behind** — `put()` returns immediately; flush to backing store is async (batched)
- **Thread-safe** — `synchronized` monitor on `LFUCache`; `BlockingQueue` for write-behind

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Strategy** | `EvictionPolicy` → `O1LFUEvictionPolicy` | Swap LFU ↔ LRU ↔ ARC without touching `LFUCache` |
| **Strategy** | `BackingStore` → `InMemoryBackingStore` | Backing store is injectable; swap for JDBC in prod |
| **Producer-Consumer** | `WriteBehindBuffer` / flusher thread | Decouples hot write path from slow DB I/O |

## Package Structure

```
com.lldprep.systems.lfucache/
├── model/
│   ├── CacheEntry.java          # value + expiresAt (per-entry TTL)
│   └── WriteBehindTask.java     # record: key, value, enqueuedAt
├── policy/
│   ├── EvictionPolicy.java      # interface: keyAccessed / evictKey / removeKey
│   └── O1LFUEvictionPolicy.java # O(1) via minFreq + freqBuckets
├── store/
│   ├── BackingStore.java        # interface: write / read
│   └── InMemoryBackingStore.java
├── writebehind/
│   └── WriteBehindBuffer.java   # BlockingQueue + flusher thread + batching
├── exception/
│   └── CacheException.java
├── demo/
│   └── LFUCacheDemo.java        # 5 demo scenarios
├── LFUCache.java                # orchestrator
├── DESIGN_DICE.md
└── README.md
```

## The O(1) LFU Algorithm

Three data structures + one integer invariant:

```
keyFreq:     HashMap<K, Integer>                   key → access count
freqBuckets: HashMap<Int, LinkedHashSet<K>>        freq → keys at that freq (LRU ordered)
minFreq:     int                                   lowest occupied frequency

get(key):
  freq = keyFreq[key]
  move key from freqBuckets[freq] to freqBuckets[freq+1]
  if freqBuckets[freq] empty AND minFreq == freq: minFreq++

put(new key):
  evict freqBuckets[minFreq].first() if at capacity
  keyFreq[key] = 1; freqBuckets[1].add(key); minFreq = 1   ← RESET invariant

put(existing key):
  update value in-place; call keyAccessed(key)    ← freq++ but NO minFreq reset
```

**Critical interview point:** updating an existing key must NOT reset `minFreq` to 1. Only inserting a NEW key resets it. This is the most common bug in LFU interviews.

## Why O(1) Matters

Naive LFU (`Collections.min()` over all keys): O(n) per eviction.  
At 1M cached keys and 10K TPS: **10 billion comparisons/second** just for eviction.  
The O(1) approach makes eviction cost independent of cache size.

## Write-Behind vs Write-Through

| | Write-Through | Write-Behind (this impl) |
|---|---|---|
| `put()` latency | Blocks on DB write | Returns in nanoseconds |
| Durability | Immediate | Eventual (loss window exists) |
| Best for | Financial records | Analytics, counters, feeds |

## Demo Scenarios

1. **LFU eviction** — 3-slot cache; `a(3)`, `b(2)`, `c(1)` → adding `d` evicts `c`
2. **LRU tiebreak** — three keys at freq=1; oldest (insertion order) evicted
3. **Per-entry TTL** — `short-lived` (200ms) expires; `long-lived` (60s) survives
4. **Update path** — updating `p` increments freq, not resets; `q` (low freq) is evicted instead
5. **Write-behind async** — all `put()` calls return immediately; `BackingStore` lags behind

## Documentation

- `DESIGN_DICE.md` — Full D.I.C.E. workflow, O(1) algorithm with worked example, write-behind design, per-entry TTL rationale, gap analysis vs existing cache
