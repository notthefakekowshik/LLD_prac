# Cache Eviction Algorithms — Theory

---

## LRU (Least Recently Used)

### How it works
Evicts the item that was accessed least recently. Every access moves the item to the "most recently used" end. When the cache is full, the item at the "least recently used" end is evicted.

### Implementation
Typically a `HashMap` + doubly linked list. The list maintains access order; the map gives O(1) lookup. On every access, the node is unlinked and moved to the head.

### Where it performs well
- Workloads with **temporal locality** — recently accessed items are likely to be accessed again soon
- General-purpose caching (web pages, database query results)

### Where it breaks down

**1. Scan resistance problem**
A one-time sequential scan (e.g. a full table scan, batch job) floods the cache with items that will never be accessed again, evicting hot items that were being used regularly.

```
Cache size: 3
Hot items: A, B, C  (accessed repeatedly)
Scan arrives: X, Y, Z, W  (one-time reads)

After scan: cache = [Z, Y, X] — A, B, C are gone
Next access to A: MISS — even though A was "hot"
```

**2. Recency bias over frequency**
An item accessed 1000 times yesterday but not in the last minute gets evicted before an item accessed once a second ago. LRU only sees "when", not "how often".

**3. Cold start vulnerability**
A brand new item immediately gets the "most recent" spot, potentially evicting a frequently-used item just because the new item arrived later.

---

## LFU (Least Frequently Used)

### How it works
Evicts the item with the lowest access count. Tracks a frequency counter per item; on eviction, the item with `min(frequency)` is removed.

### Implementation
`HashMap<key, value>` + `HashMap<key, frequency>` + `HashMap<frequency, LinkedHashSet<keys>>`. Maintains a `minFrequency` pointer for O(1) eviction.

### Where it performs well
- Workloads with **frequency locality** — genuinely popular items stay; rarely used items go
- Content delivery (videos, articles) where popularity is stable over time

### Where it breaks down

**1. Cache pollution from historical frequency**
An item that was hot 6 hours ago but is now irrelevant has a high frequency count. It sits in the cache indefinitely, blocking newer, currently-relevant items.

```
Item "news-jan-1" accessed 500 times on Jan 1
Item "news-jan-7" accessed 10 times on Jan 7

On Jan 7: "news-jan-1" still has freq=500, never evicted
          "news-jan-7" gets evicted despite being current
```

**2. Frequency counts never decay**
Standard LFU has no notion of time. A burst of accesses permanently inflates an item's count. There is no way for an item to "cool down".

**3. New item bias**
Every new item starts at frequency=1 and is immediately the eviction candidate, even if it is about to become very popular. LFU is hostile to items that need a "warm-up" period.

**4. Tie-breaking is arbitrary**
Many items often share `minFrequency`. Which one gets evicted among equals is undefined (or FIFO within a frequency bucket), which can still evict the wrong item.

---

## Why Neither is Enough

| Problem | LRU | LFU |
|---|---|---|
| Scan resistance | Fails — scans evict hot items | OK |
| Frequency awareness | Fails — no frequency tracking | OK |
| Recency awareness | OK | Fails — old counts never decay |
| New item friendliness | OK — new items survive initially | Fails — freq=1 is first to go |
| Implementation complexity | Medium | High |

Real workloads mix both patterns — some items are hot because they are *recent*, others because they are *frequent*. Neither pure algorithm handles the mix well.

---

## TinyLFU — The Hybrid Answer

### Core idea
Combine a **frequency sketch** (approximate frequency counting) with a **recency window** (small LRU) and a **main protected cache** (larger LRU). Items must "prove" their frequency before entering the protected zone.

### Structure (W-TinyLFU — used by Caffeine)

```
Incoming requests
      |
      v
┌─────────────┐     admission     ┌──────────────────────────┐
│  Window LRU │  ─── filter ───>  │  Main Cache (SLRU)       │
│  (1% size)  │                   │  ┌──────────┬──────────┐ │
└─────────────┘                   │  │ Protected│ Probation│ │
                                  │  │ (80%)    │ (20%)    │ │
                                  │  └──────────┴──────────┘ │
                                  └──────────────────────────┘
```

- **Window LRU** — new items land here first (no frequency requirement). Handles cold-start problem.
- **Admission filter** — when an item is promoted from Window to Main, its frequency is compared against the item it would evict. It only enters if it has higher frequency. Scan resistance achieved here.
- **Protected segment** — frequently accessed items live here, safe from eviction.
- **Probation segment** — items here are candidates for eviction; they move to Protected if accessed again.
- **Count-Min Sketch** — space-efficient probabilistic structure that approximates frequency counts using a small fixed-size array. Counts decay periodically (frequency aging), solving LFU's historical pollution problem.

### How it solves each problem

| Problem | TinyLFU solution |
|---|---|
| Scan resistance | Admission filter rejects low-frequency items from entering Main |
| Recency awareness | Window LRU gives new items a chance without frequency requirement |
| Historical pollution | Count-Min Sketch periodically halves all counts (aging/decay) |
| New item fairness | Items enter Window first; frequency only required for Main promotion |
| Memory efficiency | Count-Min Sketch uses O(1) fixed space regardless of item count |

### Why it matters in practice
Caffeine (the Java caching library) implements W-TinyLFU and consistently outperforms both Guava (LRU) and pure LFU across production workloads. It achieves hit rates 10-40% higher than LRU on mixed access patterns.

---

## Summary

| Algorithm | Best for | Weakness |
|---|---|---|
| LRU | Temporal locality, simple workloads | Scans, no frequency awareness |
| LFU | Stable popularity workloads | New item starvation, no recency decay |
| TinyLFU | Mixed real-world workloads | More complex, slight approximation error |

For production systems with unpredictable access patterns, TinyLFU (via Caffeine) is the default choice.
