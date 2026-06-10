# Concurrent Maps

High-ROI map choices for Java backend interviews and regular work.

---

## 1. What Problem Do Concurrent Maps Solve?

Normal `HashMap` is unsafe under concurrent writes. `Collections.synchronizedMap()` is safe, but uses one coarse lock around the whole map. `ConcurrentMap` implementations solve the same problem with better concurrency:

- multiple readers can proceed without blocking each other.
- unrelated keys can often be updated concurrently.
- common compound operations are exposed as atomic methods.

```text
Concurrent Maps
├── ConcurrentHashMap      → fast exact key lookup
└── ConcurrentSkipListMap  → sorted keys + range queries
```

---

## 2. Quick Choice

| Need | Use | Avoid When |
|---|---|---|
| exact key lookup, cache, counters, dedupe map | `ConcurrentHashMap` | need sorted/range queries |
| sorted keys, range lookup, floor/ceiling queries | `ConcurrentSkipListMap` | only exact lookup needed |

---

## 3. `ConcurrentHashMap<K, V>`

Default concurrent map.

Use cases:

- user session cache
- idempotency-key store
- per-user rate limiter buckets
- in-memory metrics counters
- visited URL dedupe in crawler

Example:

```java
ConcurrentHashMap<String, LongAdder> requestCounts = new ConcurrentHashMap<>();

void recordRequest(String userId) {
    requestCounts.computeIfAbsent(userId, id -> new LongAdder()).increment();
}
```

Why good:

- `get`, `put`, `compute`, `merge` are thread-safe.
- compound updates can be atomic through `compute*` / `merge`.
- iterators are weakly consistent, not fail-fast.

Internals that matter:

- Backed by a hash table of bins/buckets.
- Read path is mostly lock-free; `get()` uses volatile reads.
- Empty-bin insert uses CAS.
- Non-empty-bin update locks only that bin, not whole map.
- Colliding bin starts as linked list; can become red-black tree after many collisions.
- Counter is striped internally, so `size()` under writes is not a perfect snapshot.

### How `put()` Works Internally

Simplified flow:

```text
put(key, value)
├── compute hash
├── locate bucket index
├── if bucket empty
│   └── CAS new node into bucket
└── else
    ├── synchronized(bucket head)
    ├── update existing key OR append new node
    └── treeify bin if collision chain is large enough
```

Important consequence:

- two writes to different buckets can proceed concurrently.
- two writes to same bucket serialize.
- `get()` usually does not block on writers; it sees either old or new value safely.

### Atomic Compound Operations

These methods exist because check-then-act is unsafe:

```java
// ❌ race: two threads can both observe missing key
if (!map.containsKey(userId)) {
    map.put(userId, new Bucket());
}

// ✅ atomic per key/bin
Bucket bucket = map.computeIfAbsent(userId, id -> new Bucket());
```

High-ROI methods:

| Method | Use |
|---|---|
| `putIfAbsent(k, v)` | insert only if missing |
| `computeIfAbsent(k, fn)` | lazy creation/cache loading |
| `compute(k, fn)` | atomic read-modify-write |
| `merge(k, value, fn)` | counters/aggregation |
| `remove(k, v)` | remove only if mapping still matches |

### Why `null` Is Not Allowed

`map.get(k)` returns `null` when key is absent. If null values were allowed, this would be ambiguous:

```java
V value = map.get(k);
// Is key absent, or present with null value?
```

In concurrent code, checking `containsKey(k)` after `get(k)` is not reliable because another thread can mutate the map between calls. CHM avoids the ambiguity by banning null.

### Weakly Consistent Iteration

CHM iterators:

- do not throw `ConcurrentModificationException`.
- may reflect some updates that happen during iteration.
- are safe for monitoring/debug/reporting.
- are not a transactional snapshot.

Example:

```java
for (Map.Entry<String, LongAdder> entry : requestCounts.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue().sum());
}
```

Good for metrics output. Not good for “bill exactly these users once” unless you take your own snapshot/lock.

Watch out:

- no `null` keys or values.
- `size()` can be approximate under concurrent writes.
- `containsKey` then `put` is not atomic; use `putIfAbsent` or `computeIfAbsent`.

Interview line:

> `ConcurrentHashMap` is best for high-throughput exact key lookup. Java 8 uses bucket-level CAS / synchronized, not old segment locking.

Demo: [ConcurrentHashMapDemo.java](ConcurrentHashMapDemo.java)

---

## 4. `ConcurrentSkipListMap<K, V>`

Concurrent sorted `NavigableMap`.

Use cases:

- leaderboard by score
- order book price levels
- time-series events by timestamp
- range queries like `events between t1 and t2`

Example:

```java
ConcurrentSkipListMap<Long, String> eventsByTime = new ConcurrentSkipListMap<>();

eventsByTime.put(System.currentTimeMillis(), "ORDER_CREATED");

Map<Long, String> lastMinute = eventsByTime.tailMap(System.currentTimeMillis() - 60_000);
```

Why good:

- keeps keys sorted.
- supports `firstKey`, `lastKey`, `floorKey`, `ceilingKey`, `headMap`, `tailMap`.
- average O(log n).

Internals that matter:

- Backed by a concurrent skip list: layered sorted linked lists.
- Top layers skip over many nodes; bottom layer contains all keys.
- Search moves right while key is smaller, then drops down level by level.
- Updates use CAS on node links instead of locking whole structure.
- This gives sorted operations without one global `TreeMap` lock.

### Skip List Mental Model

Think of it as express lanes over a sorted linked list:

```text
Level 3:  1 -------------------- 20
Level 2:  1 -------- 9 --------- 20
Level 1:  1 --- 4 --- 9 --- 15 -- 20
Level 0:  1-2-3-4-5-6-9-10-15-18-20
```

Search for `15`:

```text
start at top-left
move right while next key <= target
drop down when next key would overshoot
repeat until bottom level
```

Why this matters:

- sorted operations are O(log n) average.
- no global tree lock like `Collections.synchronizedMap(new TreeMap<>())`.
- range views like `tailMap`, `headMap`, `subMap` are natural.

### Range Query Example

```java
ConcurrentSkipListMap<Long, String> events = new ConcurrentSkipListMap<>();

long now = System.currentTimeMillis();
events.put(now - 10_000, "CREATED");
events.put(now - 2_000, "PAID");
events.put(now + 5_000, "SHIPPED");

Map<Long, String> recent = events.subMap(now - 5_000, true, now, true);
```

This kind of query is exactly where `ConcurrentHashMap` is the wrong data structure.

Watch out:

- slower than `ConcurrentHashMap` for exact lookup.
- use only when sorted/range behavior matters.

Interview line:

> Use `ConcurrentSkipListMap` when concurrency + sorted keys/range queries matter. Otherwise, `ConcurrentHashMap` is simpler and faster.

Demo: [ConcurrentSkipListMapDemo.java](ConcurrentSkipListMapDemo.java)

---

## 5. `synchronizedMap` vs `ConcurrentHashMap`

```java
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
```

`synchronizedMap` is not “bad”. It is just coarse-grained.

| Feature | `Collections.synchronizedMap` | `ConcurrentHashMap` |
|---|---|---|
| Locking | one lock for whole map | bucket/bin level |
| Reads | take global lock | mostly lock-free |
| Compound actions | external `synchronized(map)` needed | `compute`, `merge`, `putIfAbsent` |
| Iteration | caller must synchronize | weakly consistent |
| Best for | tiny maps, low contention | normal backend concurrent access |

External synchronization example:

```java
synchronized (syncMap) {
    if (!syncMap.containsKey(key)) {
        syncMap.put(key, value);
    }
}
```

---

## 6. Common Traps

| Trap | Correct Thought |
|---|---|
| `ConcurrentHashMap` makes full workflows atomic | Only individual methods are atomic; use `compute`, `merge`, or external lock for compound workflows |
| `size()` is always exact | Under concurrent writes, treat it as approximate |
| `ConcurrentSkipListMap` is better because sorted | Sorting costs O(log n); use only when sorted behavior matters |
| `get()` + `put()` is atomic because map is concurrent | No. Use `computeIfAbsent`, `putIfAbsent`, or `compute` |
| Weakly consistent iterator is unsafe | It is safe, just not exact snapshot |

---

## 7. Interview Q&A

**Q: Why does `ConcurrentHashMap` not allow null?**
A: `get()` returning null means key absent. If null values were allowed, `get()` would be ambiguous, and `containsKey()` would not fix that atomically under concurrency.

**Q: Does `ConcurrentHashMap` lock the whole map?**
A: No. Java 8+ uses CAS for empty-bin insertion and `synchronized` on a bin head for contended updates. Different bins can update concurrently.

**Q: When choose `ConcurrentSkipListMap`?**
A: When sorted keys or range queries are part of requirement: leaderboard, order book, time-series windows. For normal exact lookup, choose `ConcurrentHashMap`.

**Q: Is `size()` exact?**
A: Not guaranteed under concurrent mutation. It is fine for monitoring, not for correctness-critical decisions.

---

## 8. Mental Model

Use `ConcurrentHashMap` for fast exact lookup; use `ConcurrentSkipListMap` only when order/range queries are part of the requirement.
