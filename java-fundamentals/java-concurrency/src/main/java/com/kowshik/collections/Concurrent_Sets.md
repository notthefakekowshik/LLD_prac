# Concurrent Sets

Thread-safe unique-value collections. Most backend use cases need membership/dedupe, not ordering.

---

## 1. What Problem Do Concurrent Sets Solve?

A set answers: “Have I seen this value before?” In backend systems this usually means dedupe, active membership, or listener registration.

```text
Concurrent Sets
├── ConcurrentHashMap.newKeySet()  → default membership/dedupe set
├── CopyOnWriteArraySet            → rare-write listener/subscriber set
└── ConcurrentSkipListSet          → sorted/range set
```

Normal `HashSet` is unsafe under concurrent writes. `Collections.synchronizedSet()` is safe but coarse-grained and needs manual locking during iteration. Concurrent sets optimize for common concurrent access patterns.

---

## 2. Quick Choice

| Need | Use | Avoid When |
|---|---|---|
| general thread-safe membership / dedupe | `ConcurrentHashMap.newKeySet()` | need sorted order |
| rare-write unique listener registry | `CopyOnWriteArraySet` | writes are frequent |
| sorted concurrent set | `ConcurrentSkipListSet` | plain membership is enough |

---

## 3. `ConcurrentHashMap.newKeySet()`

Best general-purpose concurrent set.

Use cases:

- visited URL set
- active user IDs
- processed event IDs
- unique lock keys

Example:

```java
Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

boolean firstTime = processedEventIds.add(eventId);
if (firstTime) {
    process(event);
}
```

Why good:

- same scalability characteristics as `ConcurrentHashMap`.
- `add` is atomic.
- better than `Collections.synchronizedSet(new HashSet<>())` under contention.

Internals that matter:

- It is a `ConcurrentHashMap` where keys are set elements.
- Values are dummy constants hidden by the set view.
- `add(x)` maps to atomic map insertion.
- Same weakly consistent iteration as `ConcurrentHashMap`.

### Why It Is Better Than `synchronizedSet`

```java
Set<String> syncSet = Collections.synchronizedSet(new HashSet<>());

// Iteration must be externally synchronized.
synchronized (syncSet) {
    for (String id : syncSet) {
        inspect(id);
    }
}
```

With `ConcurrentHashMap.newKeySet()`, iteration is weakly consistent and safe without locking whole set:

```java
for (String id : processedEventIds) {
    inspect(id); // safe, but not exact snapshot
}
```

### Dedupe Pattern

```java
boolean firstSeen = processedEventIds.add(eventId);
if (!firstSeen) {
    return; // duplicate event
}
handle(event);
```

`add` is atomic. This is the core reason it is useful.

Watch out:

- no ordering.
- weakly consistent iteration.

---

## 4. `CopyOnWriteArraySet<E>`

Set version of copy-on-write list.

Use cases:

- unique event listeners
- feature flag subscribers
- small plugin registry

Example:

```java
Set<Listener> listeners = new CopyOnWriteArraySet<>();

void notifyAllListeners(Event event) {
    for (Listener listener : listeners) {
        listener.onEvent(event);
    }
}
```

Why good:

- iteration needs no lock.
- duplicate listeners are avoided.
- safe when listeners are rarely added/removed but notified often.

Internals that matter:

- Backed by a `CopyOnWriteArrayList`.
- `add` first checks if element exists; if absent, creates a new copied array.
- Iterators hold reference to old array snapshot.
- Reads are cheap; writes are O(n) copy.

### Listener Registry Example

```java
class EventBus {
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    void register(Listener listener) {
        listeners.add(listener);
    }

    void publish(Event event) {
        for (Listener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
```

Why this works well:

- publishing happens frequently.
- registering listeners happens rarely.
- listener can unregister during callback without breaking iteration.

Watch out:

- every write copies array → O(n).
- poor for large/write-heavy sets.

---

## 5. `ConcurrentSkipListSet<E>`

Sorted concurrent set.

Use cases:

- sorted active IDs
- online users sorted by login time
- scheduled timestamps needing range queries

Example:

```java
ConcurrentSkipListSet<Long> scheduledTimes = new ConcurrentSkipListSet<>();
scheduledTimes.add(System.currentTimeMillis() + 5_000);

Long next = scheduledTimes.ceiling(System.currentTimeMillis());
```

Internals that matter:

- Backed by `ConcurrentSkipListMap` internally.
- Element is stored as map key; value is dummy marker.
- Sorted/range behavior comes from skip-list layers.
- Average operations are O(log n).

### Range Example

```java
ConcurrentSkipListSet<Long> loginTimes = new ConcurrentSkipListSet<>();

long now = System.currentTimeMillis();
long lastFiveMinutes = now - 5 * 60_000;

SortedSet<Long> recentLogins = loginTimes.tailSet(lastFiveMinutes);
```

If you only need `contains(id)`, this is overkill. If you need “all values after X”, this is exactly right.

Watch out:

- O(log n), so slower than hash-based membership.
- only worth it when sorted operations matter.

---

## 6. Comparison

| Feature | `newKeySet()` | `CopyOnWriteArraySet` | `ConcurrentSkipListSet` |
|---|---|---|---|
| Ordering | none | insertion-array snapshot order-ish, not sorted | sorted |
| Membership cost | O(1) average | O(n) scan | O(log n) |
| Write cost | scalable | O(n) copy | O(log n) |
| Best for | dedupe/membership | listeners | sorted/range membership |
| Iterator | weakly consistent | snapshot | weakly consistent sorted |

---

## 7. Common Traps

| Trap | Correct Thought |
|---|---|
| Use `CopyOnWriteArraySet` for all thread-safe sets | Only for small, read-heavy, write-rare sets |
| Need a set, so use `Collections.synchronizedSet` | Prefer `ConcurrentHashMap.newKeySet()` for concurrent membership |
| Sorted set is always nicer | Sorted means extra cost; use only if requirement needs order |
| `CopyOnWriteArraySet` avoids duplicates cheaply | Duplicate check is O(n), fine only for small sets |
| Weakly consistent iteration is broken | It is safe, but not exact snapshot |

---

## 8. Interview Q&A

**Q: What is the default concurrent set in Java?**
A: `ConcurrentHashMap.newKeySet()` for general membership/dedupe.

**Q: Why use `CopyOnWriteArraySet`?**
A: Listener-style workloads: many iterations, rare mutations, duplicate prevention.

**Q: When use `ConcurrentSkipListSet`?**
A: Need sorted set operations like `ceiling`, `floor`, `tailSet`, or range queries.

---

## 9. Mental Model

Use `ConcurrentHashMap.newKeySet()` by default; use copy-on-write for listener-style sets; use skip-list only for sorted/range behavior.
