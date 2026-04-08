# Locks Theory

The `java.util.concurrent.locks` package: more powerful than `synchronized`, more explicit control.

---

## 1. `synchronized` vs `ReentrantLock` — When to Choose

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Syntax | Implicit (compiler manages) | Explicit (`lock()` / `unlock()`) |
| Interruptible acquisition | No | Yes — `lockInterruptibly()` |
| Timed acquisition | No | Yes — `tryLock(timeout, unit)` |
| Non-blocking try | No | Yes — `tryLock()` |
| Fairness | No (non-fair barge) | Yes — `new ReentrantLock(true)` |
| Multiple Conditions | No (one per monitor) | Yes — `lock.newCondition()` |
| Performance | Similar (JVM-optimized) | Similar; no scope enforcement |

**Rule**: Prefer `synchronized` for simplicity. Use `ReentrantLock` when you need timed/interruptible acquisition, fairness, or multiple Conditions.

**Non-negotiable pattern with ReentrantLock:**
```java
lock.lock();
try {
    // critical section
} finally {
    lock.unlock(); // ALWAYS in finally — even if exception is thrown
}
```
Forgetting `finally` causes permanent lock leak — the lock is never released.

---

## 2. AQS — AbstractQueuedSynchronizer

`ReentrantLock`, `Semaphore`, `CountDownLatch`, `CyclicBarrier` — all built on AQS.

**Core mechanism:**
- An `int state` field (atomic) — represents lock count for ReentrantLock, permit count for Semaphore, etc.
- A CLH (Craig, Landin, Hagersten) queue of parked threads waiting for the lock
- CAS on `state` to acquire; `LockSupport.park()` to wait; `LockSupport.unpark()` to wake

```
Thread trying to acquire:
1. CAS(state, 0, 1) → success? → acquired
2. Failed? → enqueue to CLH, LockSupport.park()
3. When unlocked: head thread dequeued, LockSupport.unpark()
```

**Fair vs Non-Fair mode:**
- **Non-fair (default)**: A releasing thread does NOT check the queue — any thread (including a brand-new one) can barge in and steal the lock. Higher throughput, possible starvation.
- **Fair**: A releasing thread unparks the head of the queue first. Strict FIFO. Lower throughput (no barging), prevents starvation.

---

## 3. ReentrantLock Features

```java
ReentrantLock lock = new ReentrantLock(false); // non-fair by default

// Basic use:
lock.lock();
try { ... } finally { lock.unlock(); }

// Non-blocking try:
if (lock.tryLock()) {
    try { ... } finally { lock.unlock(); }
} else {
    // Do something else
}

// Timed try (avoids deadlock — give up after timeout):
if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    try { ... } finally { lock.unlock(); }
}

// Interruptible (allows another thread to interrupt the waiting thread):
lock.lockInterruptibly(); // throws InterruptedException if interrupted while waiting

// Diagnostics:
lock.isLocked();
lock.isHeldByCurrentThread();
lock.getQueueLength(); // approximate number of waiting threads
```

---

## 4. ReadWriteLock

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock  = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

// Multiple readers can hold the read lock simultaneously
readLock.lock();
try { return data; } finally { readLock.unlock(); }

// Only one writer; blocks all readers while held
writeLock.lock();
try { data = newData; } finally { writeLock.unlock(); }
```

**Rules:**
- Multiple threads can hold `readLock` at the same time (shared)
- `writeLock` is exclusive — blocks all readers and writers
- **Lock downgrade** (write → read): ALLOWED and safe
  ```java
  writeLock.lock();
  try {
      update();
      readLock.lock(); // acquire read BEFORE releasing write
  } finally { writeLock.unlock(); }
  // Now holding readLock only
  try { read(); } finally { readLock.unlock(); }
  ```
- **Lock upgrade** (read → write): NOT supported — attempting it causes deadlock
  (two threads each holding readLock would both block waiting for writeLock forever)

**Write starvation (non-fair mode):** If readers continuously arrive, the writer may wait indefinitely. Solutions: use fair mode (`new ReentrantReadWriteLock(true)`), or switch to `StampedLock`.

---

## 5. StampedLock (Java 8)

Three modes, designed for read-heavy scenarios:

```java
StampedLock sl = new StampedLock();

// 1. Write lock (exclusive)
long stamp = sl.writeLock();
try { modify(); } finally { sl.unlockWrite(stamp); }

// 2. Read lock (shared, like ReadWriteLock.readLock)
long stamp = sl.readLock();
try { read(); } finally { sl.unlockRead(stamp); }

// 3. Optimistic read — NO lock acquired
long stamp = sl.tryOptimisticRead();
int x = readX();
int y = readY();
if (!sl.validate(stamp)) {
    // A write happened — fall back to real read lock
    stamp = sl.readLock();
    try { x = readX(); y = readY(); } finally { sl.unlockRead(stamp); }
}
```

**Optimistic read:** checks a version counter, not an actual lock. Near-zero cost if no write happened. Re-validates after reading — if a writer interrupted, retry with a real lock.

**Pitfalls:**
- `StampedLock` is **NOT reentrant** — if a thread holding a write lock tries to acquire any lock again, it deadlocks
- Stamps must be passed correctly (don't mix stamps between lock modes)
- Unlocking with the wrong stamp throws `IllegalMonitorStateException`

**Use case:** Data structures with very high read-to-write ratios (>95% reads). Configuration, reference data, caches.

---

## 6. Lock Best Practices

1. **Always unlock in `finally`** — no exceptions
2. **Minimize critical section size** — hold lock only while accessing shared state
3. **Prefer `tryLock` with timeout** over indefinite `lock()` in production to avoid deadlock
4. **One lock per protected resource** — don't use the same lock for unrelated resources
5. **Document thread-safety guarantees** — which lock protects which field
6. **Don't call alien methods while holding a lock** — calling an unknown method while locked risks deadlock or lock inversion

---

## 7. Interview Q&A

**Q: When would you use `StampedLock` over `ReadWriteLock`?**
A: When read traffic is extremely high (99%+ reads) and write latency under contention matters. `StampedLock`'s optimistic read has near-zero overhead — no lock acquisition at all. `ReadWriteLock` blocks writers when readers are active. But: `StampedLock` is not reentrant and has a more complex API — only use it when profiling shows `ReadWriteLock` is a bottleneck.

**Q: Can a read lock be upgraded to a write lock in `ReentrantReadWriteLock`?**
A: No. It deadlocks: both threads hold readLock, both try to acquire writeLock, neither can proceed. Either redesign to acquire writeLock from the start, or use `StampedLock` (which has a `tryConvertToWriteLock` method).

**Q: What is AQS and how does `ReentrantLock` use it?**
A: `AbstractQueuedSynchronizer` is the backbone of most `java.util.concurrent` locks. It maintains an atomic `state` int and a CLH queue of parked threads. `ReentrantLock` uses `state` as the lock hold count — 0 = unlocked, N = held N times by the same thread (reentrant). Acquiring: CAS(0 → 1). Releasing: decrement state; if 0, unpark the queue head.

**Q: What happens if you forget `unlock()` in a `ReentrantLock`?**
A: The lock is permanently held by that thread. All other threads attempting to `lock()` will block forever — a permanent deadlock. Unlike `synchronized`, there is no automatic release. This is why `finally` is non-negotiable.

**Q: Fair lock vs non-fair — which has higher throughput and why?**
A: Non-fair has higher throughput. When a lock is released, a newly arriving thread can immediately barge in via CAS without waking up the parked head of the queue. This avoids the overhead of `unpark()` and context switching. Fair locks must wake the queued thread, which is slower. Use fair when starvation prevention is more important than throughput.
