# Concurrent Lists

Java has one common high-ROI concurrent list: `CopyOnWriteArrayList`.

---

## 1. What Problem Do Concurrent Lists Solve?

Most concurrent workloads do **not** want a list. They want:

- a queue for producer-consumer.
- a set for unique membership.
- a map for lookup.

The high-ROI concurrent list case is listener-style data: many reads/iterations, very few writes.

```text
Concurrent Lists
└── CopyOnWriteArrayList  → read-heavy snapshot iteration
```

---

## 2. Quick Choice

| Need | Use | Avoid When |
|---|---|---|
| listener/callback/interceptor list | `CopyOnWriteArrayList` | writes are frequent |
| request logs / task queues / frequent append-remove | queue or externally synchronized list | need cheap writes |

---

## 3. `CopyOnWriteArrayList<E>`

Best for small, read-heavy, write-rare lists.

Use cases:

- event listeners
- interceptors
- callbacks
- configuration observers

Example:

```java
CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

void publish(Event event) {
    for (Listener listener : listeners) {
        listener.onEvent(event);
    }
}
```

Why good:

- iteration is snapshot-based.
- no `ConcurrentModificationException`.
- readers do not block writers.
- safe when listener removes itself during callback.

Internals that matter:

- Backed by an array reference.
- Reads access current array without locking.
- Writes take a lock, copy entire array, modify copy, then publish new array reference.
- Iterators keep reference to array that existed when iterator was created.
- This is why reads are fast and writes are expensive.

### Write Path Mental Model

```text
Initial array: [A, B]

add(C)
├── acquire write lock
├── copy [A, B] to new array [A, B, C]
├── publish new array reference
└── release lock

Old iterators still see [A, B].
New readers see [A, B, C].
```

### Why It Is Good For Listeners

```java
CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

void publish(Event event) {
    for (Listener listener : listeners) {
        listener.onEvent(event);
    }
}
```

During publish:

- no lock is held while calling listener code.
- listener can add/remove listeners without `ConcurrentModificationException`.
- current publish sees old snapshot; next publish sees updated list.

Watch out:

- every mutation copies full array.
- bad for write-heavy data.
- not for queues or request history.

Interview line:

> `CopyOnWriteArrayList` trades expensive writes for lock-free snapshot reads. Great for listeners, terrible for write-heavy lists.

---

## 4. `CopyOnWriteArrayList` vs `synchronizedList`

| Feature | `CopyOnWriteArrayList` | `Collections.synchronizedList` |
|---|---|---|
| Read iteration | no lock, snapshot | external synchronization needed |
| Write cost | O(n) copy | O(1) append with lock |
| Reader/writer blocking | readers do not block | global list lock |
| Best for | listeners/callbacks | small low-contention mutable lists |

External synchronization needed for `synchronizedList` iteration:

```java
List<String> list = Collections.synchronizedList(new ArrayList<>());

synchronized (list) {
    for (String item : list) {
        process(item);
    }
}
```

---

## 5. When Not To Use List

| Scenario | Better Choice |
|---|---|
| producer-consumer tasks | `BlockingQueue` |
| async completed job IDs | `ConcurrentLinkedQueue` |
| unique membership | `ConcurrentHashMap.newKeySet()` |
| frequent append + remove | queue/deque, not list |

---

## 6. Common Traps

| Trap | Correct Thought |
|---|---|
| Thread-safe list means good for all list workloads | `CopyOnWriteArrayList` is specialized for read-heavy workloads |
| Snapshot iterator sees latest writes | Iterator sees array snapshot from iterator creation |
| It avoids memory issues | Frequent writes create many copied arrays and GC pressure |
| It is good for append-heavy logs | No. Use queue or bounded buffer |
| It gives latest data during iteration | No. Iteration is snapshot-based |

---

## 7. Interview Q&A

**Q: Why does iterator not throw `ConcurrentModificationException`?**
A: Iterator holds old array snapshot. Writes publish new array; old iterator is unaffected.

**Q: When is it a bad choice?**
A: Write-heavy workloads. Every write copies entire array.

**Q: Why is it common for listener lists?**
A: Listener notification iterates much more often than registration changes, and snapshot semantics avoid lock/callback hazards.

---

## 8. Mental Model

Use concurrent lists for listener-style read-heavy data; otherwise, you probably need a queue, set, or map.
