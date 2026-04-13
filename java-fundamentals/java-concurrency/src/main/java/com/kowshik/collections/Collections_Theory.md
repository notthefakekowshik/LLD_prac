# Concurrent Collections Theory

Thread-safe collections with fine-grained synchronization — far superior to wrapping with `Collections.synchronizedXxx()`.

---

## 1. ConcurrentHashMap

The workhorse of concurrent Java. Understand its internals — frequently asked in interviews.

### Java 7 vs Java 8 Implementation

**Java 7:** Segment-based locking
- 16 default segments, each is a `ReentrantLock`
- Threads on different segments never block each other
- Max concurrency = 16

**Java 8+:** Node-level CAS + synchronized
- No segments — each bucket is independent
- Empty bucket: CAS insertion (no lock at all)
- Non-empty bucket: `synchronized` on the head node of the bucket's linked list / tree
- Max concurrency = number of buckets (hundreds to thousands)
- Buckets convert from linked list to Red-Black Tree when > 8 entries (O(n) → O(log n))

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

map.put("key", 1);
map.get("key");

// Atomic compound operations (single lock held for the whole operation):
map.putIfAbsent("key", 1);          // insert only if absent
map.computeIfAbsent("key", k -> 0); // compute and insert if absent
map.compute("key", (k, v) -> v == null ? 1 : v + 1); // atomic read-modify-write
map.merge("key", 1, Integer::sum);  // merge with existing value atomically

// Iteration is weakly consistent (reflects some but not all updates during iteration):
map.forEach((k, v) -> process(k, v));
```

**No null keys or values.** Why? Because `null` is used as a sentinel "key not found" signal internally, and distinguishing "key maps to null" from "key not present" requires separate logic — CHM avoids this by simply banning null.

### `size()` is approximate

Under concurrent modification, `size()` is not guaranteed to be exact. It sums per-segment/cell counts. For an exact count, use a counter maintained separately.

### Fail-safe iterators

CHM iterators reflect the map's state at some point during iteration. They never throw `ConcurrentModificationException`. Modifications during iteration may or may not be reflected.

---

## 2. ConcurrentSkipListMap

A lock-free sorted `NavigableMap` — think concurrent `TreeMap`.

```java
ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();
map.put(3, "three");
map.put(1, "one");
map.put(2, "two");

map.firstKey();          // 1 — smallest key
map.lastKey();           // 3 — largest key
map.headMap(2);          // {1=one} — keys < 2
map.tailMap(2);          // {2=two, 3=three} — keys >= 2
map.floorKey(2);         // 2 — greatest key <= 2
map.ceilingKey(2);       // 2 — smallest key >= 2
```

**How it works:** Multi-level linked list. Each level is a probabilistic subset of the level below. Search starts at the top level (fewer nodes, faster skip), descends to exact match. Lock-free insertions/deletions via CAS.

**Time complexity:** O(log n) average for get, put, remove.

**Use cases:** Real-time leaderboards, time-series data (sorted by timestamp), order books, priority queues with natural ordering.

---

## 3. CopyOnWriteArrayList

Every write creates a **full copy** of the backing array. Reads are always lock-free.

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("a");           // Creates new array ["a"]
list.add("b");           // Creates new array ["a", "b"]

// Iteration is always on a SNAPSHOT — never throws ConcurrentModificationException
for (String s : list) {
    list.remove(s);      // Safe! Iterator holds reference to old array
}
```

**Performance:**
- Read: O(1), lock-free — reference read
- Write: O(n) — full array copy

**Use case:** Small lists with very infrequent writes and frequent iteration. Classic example: event listener lists in GUI frameworks.

**Do NOT use when:** writes are frequent (every write = full copy = O(n) memory allocation + GC pressure).

---

## 4. `Collections.synchronizedXxx()` vs Concurrent Collections

```java
// Synchronized wrapper — uses a single lock on the entire collection
Map<K, V> m = Collections.synchronizedMap(new HashMap<>());

// STILL needs external synchronization for compound actions:
synchronized (m) {
    if (!m.containsKey(k)) {
        m.put(k, v);       // Without the outer sync, containsKey + put is not atomic
    }
}

// Also needs external sync for iteration:
synchronized (m) {
    for (Map.Entry<K, V> e : m.entrySet()) { ... }
}
```

| | `synchronizedMap` | `ConcurrentHashMap` |
|---|---|---|
| Lock granularity | Single lock (whole map) | Per-bucket |
| Compound atomicity | External sync needed | `computeIfAbsent`, `merge` etc. |
| Iteration | Needs external sync | Weakly consistent, no sync needed |
| Null keys/values | Allowed | Not allowed |
| Performance under contention | Poor | Excellent |

---

## 5. BlockingQueue Implementations

Covered in depth in `executors/BlockingQueue_Theory.md`. Quick reference:

| Class | Bounded? | Ordering | Notes |
|---|---|---|---|
| `ArrayBlockingQueue` | Yes | FIFO | Backed by array, fair mode option |
| `LinkedBlockingQueue` | Optional | FIFO | Optionally bounded; separate head/tail locks |
| `PriorityBlockingQueue` | No | Priority | Unbounded; elements must be Comparable |
| `SynchronousQueue` | 0 capacity | N/A | Direct handoff — no buffering |
| `DelayQueue` | No | Delay expiry | Elements available only after delay expires |
| `LinkedTransferQueue` | No | FIFO | Like SynchronousQueue but with buffer |

---

## 6. DelayQueue — Deep Dive

A `DelayQueue` is an **unbounded blocking queue** where elements can only be taken once their delay has expired. Elements must implement the `Delayed` interface.

### How it works

- Backed by a **`PriorityQueue`** — elements are ordered by remaining delay (shortest first = head).
- Uses a single `ReentrantLock` + a **leader-follower pattern**: only one thread (the "leader") does a timed wait for the head element's delay. Other threads wait indefinitely until signaled. This avoids thundering-herd wakeups.
- `take()` blocks until the head element's delay expires.
- `poll()` returns `null` immediately if nothing has expired.
- `put()` never blocks (unbounded). If the new element has a shorter delay than the current head, the leader thread is woken up.

### Delayed interface

```java
public interface Delayed extends Comparable<Delayed> {
    long getDelay(TimeUnit unit); // remaining delay; ≤ 0 means expired
}
```

You must implement both `getDelay()` and `compareTo()`. Store the **absolute expiry time** and compute the remaining delay dynamically:

```java
class Task implements Delayed {
    private final long expiryTime;

    Task(long delayMillis) {
        this.expiryTime = System.currentTimeMillis() + delayMillis;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(expiryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }
}
```

### When to use

| Use Case | How |
|---|---|
| **Task scheduling** | `ScheduledThreadPoolExecutor` uses `DelayQueue` internally |
| **Cache expiry** | Entries enqueued with TTL; reaper thread calls `take()` to evict |
| **Session timeout** | Sessions enqueued on creation; expired sessions cleaned by consumer |
| **Order cancellation** | Unpaid orders enqueued with 30-min delay; consumer cancels + releases inventory |
| **Retry with backoff** | Failed items re-enqueued with increasing delay |

### Key gotchas

- **Unbounded** — no back-pressure. If producers are faster than delay expiry, memory grows indefinitely.
- **Not fair** — elements with the same delay have no FIFO guarantee.
- **No null elements.**
- **Single lock** — under very high contention, throughput degrades. For extreme scale, consider a time-wheel (e.g., Netty's `HashedWheelTimer`).

---

## 7. Interview Q&A

**Q: Why does `ConcurrentHashMap` not allow null keys?**
A: `null` is used as an internal sentinel. When `get()` returns `null`, it could mean "key not found" or "key maps to null". In a single-threaded map you can disambiguate with `containsKey`, but in concurrent code that's a non-atomic check-then-act. By banning null, CHM avoids this ambiguity entirely.

**Q: Is `ConcurrentHashMap` fully consistent or weakly consistent?**
A: Weakly consistent. Operations like `size()`, `isEmpty()`, `containsValue()` reflect a transient state and may not be accurate at any single point in time. Iterators reflect the map state at some point during iteration. Individual `get/put/computeIfAbsent` operations ARE atomic.

**Q: When would `CopyOnWriteArrayList` be a bad choice?**
A: When writes are frequent. Every `add()`, `remove()`, or `set()` allocates a new array of the same size and copies all elements — O(n) time and memory per write. Under write-heavy workloads this generates massive GC pressure. Use `Collections.synchronizedList()` or a `ConcurrentLinkedQueue` instead.

**Q: How does `ConcurrentSkipListMap` achieve O(log n) without locks?**
A: It uses a lock-free multi-level linked list where each level is a probabilistic subset of the level below. Insertions and deletions use CAS on node references. The key insight: logical deletion (mark a node as deleted via CAS) precedes physical removal, preventing interference between concurrent operations.

**Q: What's the difference between `LinkedBlockingQueue` and `ArrayBlockingQueue`?**
A: `ArrayBlockingQueue` uses a single lock for both producers and consumers — they contend. `LinkedBlockingQueue` uses separate locks for head (consumer) and tail (producer) — producers and consumers rarely block each other. `LinkedBlockingQueue` therefore has higher throughput in producer-consumer scenarios.
