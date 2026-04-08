# Synchronization Theory

The most critical and most mis-understood area in Java concurrency. Get this right before touching locks or atomics.

---

## 1. Java Memory Model (JMM) — The Core Problem

Modern CPUs have multiple layers of cache (L1/L2/L3). The JVM allows threads to cache variables in CPU registers/cache for performance. This creates the **visibility problem**: writes by Thread A may not be immediately visible to Thread B.

**The JMM defines a contract**: it specifies the minimum guarantees the JVM makes about when writes by one thread become visible to another. It does this via **happens-before** relationships.

### Happens-Before Rules (8 canonical rules)

A write to variable `x` *happens-before* a read of `x` if:
1. **Program order**: within a single thread, statements happen in program order
2. **Monitor lock**: `unlock()` of a monitor HB all subsequent `lock()` of the same monitor
3. **volatile**: a write to a `volatile` field HB all subsequent reads of that field
4. **Thread start**: `Thread.start()` HB any action in the started thread
5. **Thread join**: any action in thread T HB `T.join()` returning in another thread
6. **Thread interrupt**: `Thread.interrupt()` HB the interrupted thread detecting the interrupt
7. **Object construction**: object constructor completes HB `finalize()` begins
8. **Transitivity**: if A HB B and B HB C, then A HB C

If no happens-before relationship exists between a write and a read, the read may observe a stale value.

---

## 2. `volatile` — Visibility Without Atomicity

```java
volatile boolean running = true;

// Thread A:
running = false;   // Write flushes to main memory immediately

// Thread B:
while (running) {  // Read always fetches from main memory
    doWork();
}
```

**Guarantees:**
- Visibility: every write is immediately flushed to main memory; every read fetches from main memory
- Ordering: reads/writes to volatile variables cannot be reordered relative to each other

**Does NOT guarantee atomicity:**
```java
volatile int count = 0;
count++; // NOT atomic — this is LOAD + INCREMENT + STORE (3 operations)
         // Two threads can interleave these 3 steps and lose increments
```

**When volatile is sufficient:**
- Simple flags (`running`, `shutdown`, `initialized`)
- Publishing an immutable object reference (one-time write, many reads)
- Double-checked locking (the reference field must be volatile)

**When volatile is NOT sufficient:**
- Any compound action (read-modify-write: `count++`, `balance += amount`)
- Check-then-act: `if (x == null) x = new X()` — needs synchronized or Atomic

---

## 3. `synchronized` — Mutual Exclusion + Visibility

```java
class Counter {
    private int count = 0;

    // Synchronized method — lock is 'this'
    public synchronized void increment() { count++; }
    public synchronized int get() { return count; }

    // Synchronized block — can use a different, dedicated lock object
    private final Object lock = new Object();
    public void decrement() {
        synchronized (lock) { count--; }
    }

    // Static synchronized — lock is Counter.class
    public static synchronized void staticOp() { ... }
}
```

**What synchronized does:**
1. **Mutual exclusion**: only one thread holds the intrinsic lock at a time
2. **Visibility**: acquiring a lock sees all writes made before the last release of that lock
3. **Reentrant**: the same thread can re-enter a synchronized block it already holds

**Synchronized method vs block:**
- Method: locks the entire method — simple, but coarse-grained
- Block: locks only the critical section — prefer this for performance

**Wrong lock pitfalls:**
```java
// BUG: different lock objects for each call
synchronized (new Object()) { ... }  // Each thread gets its own lock!

// BUG: Integer autoboxing creates different objects for values > 127
synchronized (Integer.valueOf(id)) { ... }  // Cache only applies to -128..127

// BUG: String interning — unexpected sharing
synchronized ("LOCK") { ... }  // String literals are interned; unexpected sharing
```

---

## 4. Object Monitor Methods — `wait()` / `notify()` / `notifyAll()`

```java
synchronized (lock) {
    // ALWAYS use while, not if (spurious wakeups)
    while (!conditionMet()) {
        lock.wait(); // Releases lock, enters WAITING — another thread can now lock
    }
    // Condition is now true — proceed
    doWork();
    lock.notifyAll(); // Wake up all waiting threads
}
```

**Rules:**
- Must be called from within a `synchronized` block on the same object — otherwise `IllegalMonitorStateException`
- `wait()` atomically releases the lock and enters WAITING
- `notify()` wakes one arbitrary waiting thread
- `notifyAll()` wakes all waiting threads (prefer this — safer, avoids lost signals)

**Thundering herd:** `notifyAll()` wakes all waiters, but only one gets the lock. The rest go back to sleep. Use `Condition.signal()` (from `ReentrantLock`) to target a specific waiting set and avoid this.

---

## 5. Condition — Multiple Waiting Sets

Covered in depth in `ConditionDemo.java`.

Key advantage: a single `ReentrantLock` can have multiple `Condition` objects — a separate waiting room for each predicate.

```java
// Classic BoundedBuffer:
Lock lock = new ReentrantLock();
Condition notFull  = lock.newCondition();  // producers wait here
Condition notEmpty = lock.newCondition();  // consumers wait here

// When producing:
while (queue.size() == capacity) notFull.await();
queue.add(item);
notEmpty.signal();  // ONLY wake consumers — producers stay asleep

// When consuming:
while (queue.isEmpty()) notEmpty.await();
queue.remove();
notFull.signal();   // ONLY wake producers
```

With `synchronized` + `notifyAll()`, you'd wake both producers and consumers every time — wasted context switches. `Condition.signal()` is surgical.

---

## 6. Deadlock

**The four necessary conditions (all must hold):**
1. **Mutual exclusion**: at least one resource is non-shareable
2. **Hold-and-wait**: a thread holds a resource while waiting for another
3. **No preemption**: resources cannot be forcibly taken from a thread
4. **Circular wait**: T1 waits for T2's resource, T2 waits for T1's resource

**Classic deadlock:**
```java
// Thread 1:
synchronized (lockA) { synchronized (lockB) { ... } }

// Thread 2:
synchronized (lockB) { synchronized (lockA) { ... } }
// → Deadlock if T1 holds lockA and T2 holds lockB simultaneously
```

**Avoidance strategies:**

1. **Lock ordering** (most common): always acquire locks in the same order across all threads
2. **tryLock with timeout**: give up if you can't get the second lock
   ```java
   if (lockA.tryLock(100, MILLISECONDS)) {
       try {
           if (lockB.tryLock(100, MILLISECONDS)) {
               try { doWork(); } finally { lockB.unlock(); }
           }
       } finally { lockA.unlock(); }
   }
   ```
3. **Lock hierarchy**: assign a numeric level to each lock; always acquire lower-numbered locks first
4. **Single global lock**: only for low-concurrency scenarios

**Detection in production:** `jstack <pid>` or `VisualVM` — look for "Found one Java-level deadlock" in the thread dump.

---

## 7. Other Concurrency Hazards

| Hazard | Description | Fix |
|---|---|---|
| Race condition | Outcome depends on thread scheduling | Proper synchronization |
| Starvation | A thread is perpetually denied a resource | Fair locks (`new ReentrantLock(true)`) |
| Livelock | Threads run but make no progress (keep yielding to each other) | Add randomness to retry intervals |
| Double-checked locking (broken) | Partially constructed object visible | Add `volatile` to the reference |

---

## 8. Double-Checked Locking — The Right Way

```java
class Singleton {
    private static volatile Singleton instance; // volatile is NON-NEGOTIABLE

    public static Singleton getInstance() {
        if (instance == null) {                  // First check — no lock (fast path)
            synchronized (Singleton.class) {
                if (instance == null) {          // Second check — with lock
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

Without `volatile`: the JVM can reorder `instance = new Singleton()` — another thread may see a non-null but partially constructed object.

**Better for singletons:** use the initialization-on-demand holder idiom (no volatile/synchronized needed):
```java
class Singleton {
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    public static Singleton getInstance() { return Holder.INSTANCE; }
}
```

---

## 9. Interview Q&A

**Q: Difference between `synchronized` and `ReentrantLock`?**
A: Both provide mutual exclusion. ReentrantLock adds: interruptible acquisition, timed acquisition, fairness policy, multiple Conditions per lock. `synchronized` is simpler and JVM-optimized (biased locking, lock elision). Prefer `synchronized` unless you need the extras.

**Q: Can `volatile` replace `synchronized`?**
A: Only for simple reads/writes of a single variable where no compound action is needed. `volatile` gives visibility + ordering but NOT atomicity.

**Q: How to detect deadlock in production?**
A: Thread dump (`jstack <pid>`) — the JVM explicitly identifies deadlocked threads. Also: JMX (`ThreadMXBean.findDeadlockedThreads()`).

**Q: What is a spurious wakeup, and how do you handle it?**
A: The OS/JVM can wake a waiting thread without `notify()` being called — this is in the Java spec. Handle by always re-checking the condition in a `while` loop after `wait()`.

**Q: Is `count++` thread-safe if `count` is `volatile`?**
A: No. `count++` is not atomic — it's read + increment + write. Two threads can both read `0`, both increment to `1`, and both write `1`. The result is `1` instead of `2`. Use `AtomicInteger.incrementAndGet()` instead.
