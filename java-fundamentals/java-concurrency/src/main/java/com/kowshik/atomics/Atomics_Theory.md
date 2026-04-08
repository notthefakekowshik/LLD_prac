# Atomics Theory

Lock-free thread safety via hardware CAS instructions. Faster than locks under low-to-moderate contention.

---

## 1. CAS — Compare-And-Swap

CAS is a single, **atomic** CPU instruction (e.g., `CMPXCHG` on x86):

```
CAS(address, expectedValue, newValue):
  if *address == expectedValue:
    *address = newValue
    return true
  else:
    return false   // "try again"
```

The entire read-compare-write happens atomically at the hardware level. No OS lock needed.

**Java CAS loop pattern (how AtomicInteger.incrementAndGet() is implemented):**
```java
int current;
do {
    current = get();           // Read current value
} while (!compareAndSet(current, current + 1));  // Retry if someone else changed it
return current + 1;
```

This is **optimistic**: assumes no contention, retries on conflict. Under high contention, the retry loop spins, wasting CPU cycles — this is why CAS is NOT always faster than locks.

---

## 2. Atomic Classes Overview

### Scalars
```java
AtomicInteger  i = new AtomicInteger(0);
AtomicLong     l = new AtomicLong(0L);
AtomicBoolean  b = new AtomicBoolean(false);

i.get()                          // Plain read
i.set(5)                         // Plain write
i.getAndSet(5)                   // Atomic swap — returns old value
i.compareAndSet(expected, next)  // CAS — returns true if successful
i.incrementAndGet()              // Atomic ++i (most common)
i.getAndIncrement()              // Atomic i++ (returns old value)
i.addAndGet(delta)               // Atomic i += delta
i.updateAndGet(x -> x * 2)      // Atomic function application
i.accumulateAndGet(5, Integer::sum)  // Atomic binary op
```

### References
```java
AtomicReference<Node> head = new AtomicReference<>(null);
// CAS on object references:
head.compareAndSet(null, new Node(1));

// IMPORTANT: reference equality (==), not .equals()
```

### Arrays
```java
AtomicIntegerArray arr = new AtomicIntegerArray(10);
arr.incrementAndGet(3);     // Atomically increment element at index 3
arr.compareAndSet(3, 5, 6); // CAS on element at index 3
```

---

## 3. LongAdder vs AtomicLong

| | AtomicLong | LongAdder |
|---|---|---|
| Under low contention | Fast | Slightly slower (cell check overhead) |
| Under HIGH contention | Slow (CAS retries spin) | Fast (cell striping) |
| Can use for CAS logic? | Yes | No — only add/sum |
| Memory | 1 long | 1+ cells (dynamic) |

**How LongAdder works:** maintains a base value + an array of `Cell` objects. Threads that contend are hashed to different cells and increment independently. `sum()` adds base + all cells. Under low contention, falls back to just base (no cells allocated).

**Rule of thumb:** Use `LongAdder` for counters and sums. Use `AtomicLong` when you need `compareAndSet` or need to read-then-act on the value.

---

## 4. The ABA Problem

```
Thread 1: reads value A from address X
Thread 2: changes X from A → B
Thread 2: changes X from B → A  (back to A!)
Thread 1: CAS(X, A, newValue) SUCCEEDS — but the world has changed!
```

**Real example:** Node in a lock-free stack. Thread 1 reads `top = NodeA`. Thread 2 pops A, pops B, pushes A back. Thread 1's CAS succeeds but the stack state is wrong — B was lost.

### Fix: AtomicStampedReference

```java
AtomicStampedReference<Node> top = new AtomicStampedReference<>(head, 0);

// Read both value AND stamp together
int[] stampHolder = new int[1];
Node node = top.get(stampHolder);
int stamp = stampHolder[0];

// CAS requires BOTH value AND stamp to match
top.compareAndSet(node, newNode, stamp, stamp + 1);
// Even if node value cycles back to same reference, stamp monotonically increases
```

`AtomicMarkableReference` — same idea but stamp is just a boolean (has the reference been "logically deleted"?).

---

## 5. Non-Blocking Data Structures

### Treiber Stack (Lock-Free Stack)

```java
class LockFreeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>(null);

    public void push(T item) {
        Node<T> newHead = new Node<>(item);
        Node<T> oldHead;
        do {
            oldHead = top.get();
            newHead.next = oldHead;
        } while (!top.compareAndSet(oldHead, newHead)); // Retry if top changed
    }

    public T pop() {
        Node<T> oldHead;
        Node<T> newHead;
        do {
            oldHead = top.get();
            if (oldHead == null) return null;
            newHead = oldHead.next;
        } while (!top.compareAndSet(oldHead, newHead));
        return oldHead.item;
    }
}
```

The retry loop is the key pattern. It's correct because each retry re-reads the latest state.

---

## 6. When to Use Atomics vs Locks

| Scenario | Use |
|---|---|
| Single-variable increment/decrement | `AtomicInteger` |
| Simple counter under high write throughput | `LongAdder` |
| Publish an object reference once | `AtomicReference` / `volatile` |
| Multi-variable invariant (e.g., `balance > 0` before debit) | `synchronized` or `Lock` |
| Lock-free data structure (stack, queue) | `AtomicReference` with CAS loop |
| Read-heavy, rare writes to a value | `AtomicReference` + optimistic read |

---

## 7. Interview Q&A

**Q: When would you prefer `AtomicInteger` over `synchronized`?**
A: When the shared state is a single integer variable and all operations are single-variable (increment, decrement, CAS). AtomicInteger has lower overhead — no OS mutex, no context switch.

**Q: Explain the ABA problem with a concrete example.**
A: In a lock-free stack: Thread 1 reads top=A. Thread 2 pops A, pops B, pushes A. Thread 1 CAS(A → C) succeeds even though the stack state was modified — B is now lost. Fix: `AtomicStampedReference` adds a monotone version number so A at stamp 0 ≠ A at stamp 2.

**Q: What does `compareAndSet` guarantee?**
A: Atomicity: the read-compare-write is a single indivisible operation. If it returns true, the update happened and no other thread modified the value in between. It also provides the same memory ordering as `volatile` reads/writes.

**Q: Is `AtomicInteger.incrementAndGet()` always faster than `synchronized`?**
A: Under low contention, yes. Under very high contention (many threads spinning on the same AtomicInteger), CAS retries cause significant CPU waste (cache-line bouncing). In this case, `LongAdder` or a lock with queued waiting may be better.

**Q: What is a memory fence / memory barrier?**
A: A CPU instruction that prevents reordering of reads/writes across the barrier. `volatile` reads/writes insert barriers. `compareAndSet` also acts as a full memory fence (both read and write barriers).
