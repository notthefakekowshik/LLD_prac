# Java Locks — Deep Dive

> **Companion to:** `Locks_Theory.md` (API reference) and `AbstractQueuedSynchronizer_Theory.md` (internals).
> **Goal:** understand the *why* — from first principles through every lock type, with the problems each one solves.

---

## 1. The Lock Taxonomy (Tree Diagram)

```
Java Locking Mechanisms
│
├── Intrinsic Locks  (built into every Java object)
│   │
│   └── synchronized
│       ├── synchronized method       → locks `this`
│       ├── synchronized static method → locks Class object
│       └── synchronized (obj) block  → locks obj's monitor
│
└── Extrinsic Locks  (explicit objects from java.util.concurrent.locks)
    │
    ├── ReentrantLock          (exclusive, reentrant, fair/non-fair)
    │
    ├── ReadWriteLock
    │   ├── ReentrantReadWriteLock.ReadLock   (shared)
    │   └── ReentrantReadWriteLock.WriteLock  (exclusive)
    │
    └── StampedLock            (optimistic read + read + write, NOT reentrant)
```

**Key distinction:**
- **Intrinsic** — JVM-managed, tied to object monitors, released automatically when block exits.
- **Extrinsic** — Developer-managed, must call `unlock()` explicitly, vastly more powerful.

---

## 2. Intrinsic Locking: `synchronized`

### How it works

Every Java object has a hidden **monitor** (a mutex + wait-set). `synchronized` acquires that monitor.

```java
// Form 1: synchronized instance method — locks 'this'
public synchronized void deposit(int amount) {
    balance += amount;
}

// Form 2: synchronized static method — locks Counter.class
public static synchronized int nextId() {
    return ++counter;
}

// Form 3: synchronized block — locks chosen object (finer granularity)
private final Object lock = new Object();

public void transfer(int amount) {
    synchronized (lock) {
        balance -= amount;
    }
}
```

### What `synchronized` gives you

- **Mutual exclusion**: only one thread in the block at a time.
- **Visibility**: changes made inside are visible to the next thread that acquires the same monitor (happens-before).
- **Reentrancy**: the same thread can re-enter (e.g., a synchronized method calling another synchronized method on `this`).

---

## 3. The Problem with `synchronized` + `wait` / `notify`

`synchronized` alone handles mutual exclusion, but real programs need threads to **wait for a condition** (e.g., "wait until the queue is non-empty"). Java provides `Object.wait()` / `Object.notify()` for this — but they have serious design flaws.

### Classic bounded-buffer with `synchronized`

```java
class BoundedBuffer {
    private final int[] buf;
    private int head, tail, count;

    BoundedBuffer(int capacity) { buf = new int[capacity]; }

    public synchronized void put(int val) throws InterruptedException {
        while (count == buf.length) wait();   // wait until not-full
        buf[tail++ % buf.length] = val;
        count++;
        notifyAll();                           // wake all waiters
    }

    public synchronized int take() throws InterruptedException {
        while (count == 0) wait();            // wait until not-empty
        int val = buf[head++ % buf.length];
        count--;
        notifyAll();
        return val;
    }
}
```

### Problems with this approach

**Problem 1: One wait-set per object — no separate conditions**

`Object` has a single wait-set. `notifyAll()` in `put()` wakes up BOTH producers and consumers, even though:
- A producer waking other producers is wasteful — they'll just re-enter `wait()`.
- You want `put()` to signal only *consumers*, and `take()` to signal only *producers*.
- With `synchronized`, you cannot do this — there is only one condition tied to the object monitor.

```
notify()  → wakes ONE random thread from the single wait-set.
            Could wake another producer who immediately goes back to sleep.
notifyAll() → wakes ALL threads, most of which do nothing useful.
              Under high contention this causes a "thundering herd".
```

**Problem 2: `notify()` can cause missed signals / deadlock**

```java
// Thread A (producer): adds item, calls notify()
// Thread B (consumer): not yet in wait() — it's between the while-check and wait()
// Thread A's notify() fires with nobody listening → signal lost → Thread B waits forever
```

`notifyAll()` avoids the lost-signal problem but is a blunt instrument.

**Problem 3: Spurious wakeups**

`wait()` can return even when no `notify()` was called (OS-level). This is why the condition check **must** be in a `while` loop, not an `if`. This is a footgun — beginners always write `if`.

**Problem 4: No timeout, no interruptibility on `lock()` itself**

`synchronized` blocks indefinitely. You cannot say "try to acquire for at most 500ms, then give up". This makes it impossible to build deadlock-safe resource acquisition patterns.

**Problem 5: No fairness control**

`synchronized` is non-fair and non-configurable. Long-waiting threads can be starved indefinitely.

**Problem 6: Pins virtual threads (Java 21+)**

A virtual thread blocking inside a `synchronized` block **pins its carrier thread**, preventing other virtual threads from running on that carrier. See `VirtualThreads_Theory.md` for full details.

---

## 4. The Fix: `ReentrantLock` + `Condition`

`ReentrantLock` replaces `synchronized`. `Condition` (from `lock.newCondition()`) replaces `Object.wait/notify`. Each `Condition` has its **own** wait-set, solving the single-wait-set problem directly.

### Same bounded-buffer, done right

```java
import java.util.concurrent.locks.*;

class BoundedBuffer {
    private final int[] buf;
    private int head, tail, count;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();  // producers wait here
    private final Condition notEmpty = lock.newCondition();  // consumers wait here

    BoundedBuffer(int capacity) { buf = new int[capacity]; }

    public void put(int val) throws InterruptedException {
        lock.lock();
        try {
            while (count == buf.length) notFull.await();   // only producers wait here
            buf[tail++ % buf.length] = val;
            count++;
            notEmpty.signal();    // wake exactly one consumer — precise, no thundering herd
        } finally {
            lock.unlock();
        }
    }

    public int take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) notEmpty.await();           // only consumers wait here
            int val = buf[head++ % buf.length];
            count--;
            notFull.signal();     // wake exactly one producer
            return val;
        } finally {
            lock.unlock();
        }
    }
}
```

### What `Condition` fixes vs `wait/notify`

| Problem with `wait/notify` | How `Condition` solves it |
|---|---|
| Single wait-set — can't wake only producers or only consumers | Multiple `Condition`s per lock — separate wait-sets |
| `notify()` wakes random thread (may be wrong type) | `signal()` on specific `Condition` wakes right waiter |
| `notifyAll()` thundering herd | `signal()` wakes exactly one; use `signalAll()` only when needed |
| Must use `synchronized` (pins virtual threads) | Used with `ReentrantLock` (park/unpark, virtual-thread safe) |
| No timed wait on lock acquisition | `lock.tryLock(timeout, unit)` |
| No interruptible lock acquisition | `lock.lockInterruptibly()` |
| No fairness control | `new ReentrantLock(true)` for fair mode |

### `Condition` method reference

```java
ReentrantLock lock = new ReentrantLock();
Condition cond = lock.newCondition();

lock.lock();
try {
    cond.await();                         // release lock + park; re-acquires on return
    cond.await(500, TimeUnit.MILLISECONDS); // timed wait; returns false on timeout
    cond.awaitNanos(nanosTimeout);
    cond.awaitUntil(deadline);            // absolute deadline (Date)
    cond.awaitUninterruptibly();          // ignores interrupts (rare)

    cond.signal();                        // wake one waiter on THIS condition
    cond.signalAll();                     // wake all waiters on THIS condition
} finally {
    lock.unlock();
}
```

**Rule:** `await()` / `signal()` must be called while holding the lock — same as `wait()` / `notify()`. Violation throws `IllegalMonitorStateException`.

---

## 5. `ReentrantLock` Full Feature Set

```java
ReentrantLock lock = new ReentrantLock();        // non-fair (default)
ReentrantLock fairLock = new ReentrantLock(true); // fair — FIFO acquisition

// --- Acquisition styles ---

lock.lock();                               // blocks indefinitely, ignores interrupt
lock.lockInterruptibly();                  // blocks, throws InterruptedException if interrupted
boolean got = lock.tryLock();             // non-blocking — returns immediately
boolean got = lock.tryLock(500, MILLISECONDS); // timed — gives up after timeout

// --- Release ---
lock.unlock();                             // ALWAYS in finally

// --- Diagnostics ---
lock.isLocked();                           // is anyone holding it?
lock.isHeldByCurrentThread();             // does THIS thread hold it?
lock.getHoldCount();                       // reentrancy depth for current thread
lock.getQueueLength();                     // approx waiting threads
lock.hasWaiters(condition);               // any threads awaiting on this Condition?
```

**Non-negotiable unlock pattern:**

```java
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();   // executes even if exception thrown — no permanent leak
}
```

---

## 6. Pessimistic Locking

### What it is

**Pessimistic locking** assumes contention will happen and **acquires a lock before accessing the resource**. The resource is blocked for other threads until the lock is released.

```
Thread A wants to update balance:
  1. Acquire lock          ← blocks Thread B immediately
  2. Read balance
  3. Compute new balance
  4. Write new balance
  5. Release lock          ← Thread B unblocks here
```

### Java implementations

```java
// 1. synchronized (intrinsic pessimistic lock)
public synchronized void withdraw(int amount) {
    if (balance >= amount) balance -= amount;
}

// 2. ReentrantLock (extrinsic pessimistic lock)
private final ReentrantLock lock = new ReentrantLock();

public void withdraw(int amount) {
    lock.lock();
    try {
        if (balance >= amount) balance -= amount;
    } finally {
        lock.unlock();
    }
}

// 3. Write lock of ReadWriteLock (pessimistic for writes)
private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

public void withdraw(int amount) {
    rwLock.writeLock().lock();
    try {
        if (balance >= amount) balance -= amount;
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

### Pros and cons

| Pros | Cons |
|---|---|
| Simple mental model — resource is yours until you release | Throughput bottleneck under high contention |
| Correct for **write-heavy** workloads | Threads block and context-switch — expensive |
| Prevents data races completely | Risk of deadlock if lock ordering not enforced |
| Works well when critical section is short | Starvation possible (non-fair mode) |
| Predictable: no retry logic needed | Pinning with `synchronized` + virtual threads |

**When to use:** write-heavy shared state, operations that must not be retried (e.g., financial transactions, in-memory counters with complex invariants).

---

## 7. Optimistic Locking

### What it is

**Optimistic locking** assumes contention is rare. It **reads without locking**, does work, then at write time **validates** that nobody else modified the data in the meantime. If validation fails, it retries.

```
Thread A wants to update balance:
  1. Read current value + version/stamp     ← NO lock taken
  2. Compute new value
  3. Attempt to write:
       "set balance = newValue IF current is still oldValue"
  4a. CAS succeeded → done
  4b. CAS failed (someone else wrote) → go back to step 1 and retry
```

### Java implementations

**CAS-based (AtomicInteger / AtomicReference):**

```java
AtomicInteger balance = new AtomicInteger(1000);

// Atomic increment — optimistic, no lock
balance.incrementAndGet();

// Manual CAS loop — read, compute, try to write, retry on conflict
int oldVal, newVal;
do {
    oldVal = balance.get();
    newVal = oldVal - amount;
    if (newVal < 0) throw new InsufficientFundsException();
} while (!balance.compareAndSet(oldVal, newVal));
```

**StampedLock optimistic read (highest-performance reads):**

```java
StampedLock sl = new StampedLock();
double x, y;

// Optimistic read — acquires NO lock, just reads a version stamp
long stamp = sl.tryOptimisticRead();
x = point.x;
y = point.y;
if (!sl.validate(stamp)) {
    // A write happened between our read and validation — fall back to real read lock
    stamp = sl.readLock();
    try {
        x = point.x;
        y = point.y;
    } finally {
        sl.unlockRead(stamp);
    }
}
double distance = Math.sqrt(x * x + y * y);
```

**Database-style (version field on entity — common in JPA/Hibernate):**

```java
@Entity
public class Account {
    @Version                    // Hibernate manages this
    private int version;        // incremented on every update

    private int balance;
}

// On update, Hibernate generates:
// UPDATE account SET balance=?, version=version+1
// WHERE id=? AND version=?   ← optimistic check
// If 0 rows updated → OptimisticLockException → retry or fail
```

### Pros and cons

| Pros | Cons |
|---|---|
| No blocking — threads never wait for each other | Retry loop adds complexity |
| High throughput under **low-to-moderate contention** | Under high contention, retries spin and waste CPU |
| No deadlock possible (no locks to deadlock on) | ABA problem with naive CAS (see `Atomics_Theory.md`) |
| Natural fit for read-heavy workloads | Not suitable for multi-step operations that can't be retried |
| No thread context switches | `StampedLock` is not reentrant — easy to deadlock yourself |

**When to use:** read-heavy data (counters, caches, config), single-variable updates, database entities with low write contention.

---

## 8. Pessimistic vs Optimistic — Decision Guide

```
Is the operation a single atomic variable update?
  └── YES → Use AtomicInteger / AtomicLong / AtomicReference (optimistic CAS)

Is the workload read-heavy (>80% reads)?
  └── YES → Use ReentrantReadWriteLock (pessimistic reads) or StampedLock (optimistic reads)

Is write contention expected to be LOW (rare conflicts)?
  └── YES → Optimistic (CAS loop or StampedLock optimistic read)

Is write contention expected to be HIGH (frequent conflicts)?
  └── YES → Pessimistic (ReentrantLock) — retrying under high contention wastes more CPU than blocking

Does the operation span multiple variables / invariants?
  └── YES → Pessimistic (you can't retry a multi-step operation atomically without a lock)

Are you on Java 21+ with virtual threads?
  └── Use ReentrantLock instead of synchronized to avoid carrier pinning
```

| Scenario | Best lock choice |
|---|---|
| Single counter, high throughput | `LongAdder` (striped optimistic) |
| Single variable with CAS logic | `AtomicInteger` / `AtomicReference` |
| Complex invariant, write-heavy | `ReentrantLock` (pessimistic) |
| Complex invariant, mostly reads | `ReentrantReadWriteLock` |
| Near-zero-cost reads, rare writes | `StampedLock` optimistic read |
| Custom synchronizer | Extend `AbstractQueuedSynchronizer` |
| Multiple wait conditions on one lock | `ReentrantLock` + multiple `Condition`s |
| Virtual thread safety | `ReentrantLock` (not `synchronized`) |

---

## 9. ReadWriteLock — Pessimistic for Reads Too

When reads dominate, a plain `ReentrantLock` is wasteful — readers block each other even though they're safe to run concurrently.

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock  = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

// Many readers can proceed in parallel
public Data read() {
    readLock.lock();
    try { return data; } finally { readLock.unlock(); }
}

// Writer gets exclusive access — all readers and other writers block
public void update(Data d) {
    writeLock.lock();
    try { data = d; } finally { writeLock.unlock(); }
}
```

**Lock downgrade (write → read): allowed and safe**

```java
writeLock.lock();
try {
    data = newData;
    readLock.lock();       // acquire read BEFORE releasing write — no window for a writer to sneak in
} finally {
    writeLock.unlock();    // now holding only readLock
}
try {
    return data;
} finally {
    readLock.unlock();
}
```

**Lock upgrade (read → write): NOT supported — deadlocks**

```
Thread A holds readLock, tries to acquire writeLock → blocks (waiting for all readers to release)
Thread B holds readLock, tries to acquire writeLock → blocks
Neither can proceed → deadlock
```

**Write starvation:** in non-fair mode, a constant stream of readers can starve a writer indefinitely. Fix: `new ReentrantReadWriteLock(true)` (fair mode) or switch to `StampedLock`.

---

## 10. StampedLock — Optimistic Reads at Maximum Speed

`StampedLock` (Java 8) adds a third mode on top of read/write: **optimistic read** — a near-zero-cost "snapshot" that requires no lock at all.

```java
StampedLock sl = new StampedLock();

// Mode 1: Write (exclusive, like writeLock)
long stamp = sl.writeLock();
try { modify(); } finally { sl.unlockWrite(stamp); }

// Mode 2: Read (shared, like readLock)
long stamp = sl.readLock();
try { read(); } finally { sl.unlockRead(stamp); }

// Mode 3: Optimistic read (NO lock — just reads a version number)
long stamp = sl.tryOptimisticRead();
int x = data.x;
int y = data.y;
if (!sl.validate(stamp)) {               // check: did a write happen while we read?
    stamp = sl.readLock();               // fall back to real read lock
    try { x = data.x; y = data.y; } finally { sl.unlockRead(stamp); }
}
```

**Critical pitfalls:**

- `StampedLock` is **NOT reentrant** — a thread holding any StampedLock mode that tries to acquire again will deadlock itself.
- Must pass the correct `stamp` to the matching `unlock*` method. Wrong stamp throws `IllegalMonitorStateException`.
- No `Condition` support — use `ReentrantLock` if you need wait/signal.
- **Do not use with virtual threads as the primary lock** if reentrant behavior is needed anywhere in the call chain.

---

## 11. Common Pitfalls

- **`if` instead of `while` for `await()`**: spurious wakeups exist — always loop.
- **Forgetting `unlock()` in `finally`**: permanent lock leak, all other threads block forever.
- **Lock upgrade (read → write) on `ReentrantReadWriteLock`**: guaranteed deadlock.
- **`notify()` with `ReentrantLock`**: `Object.notify()` requires holding the object's monitor, not the `ReentrantLock` — use `Condition.signal()` instead.
- **Holding locks across long I/O**: serializes all threads waiting for that lock regardless of lock type.
- **CAS loop under high contention**: spinning wastes CPU — switch to `ReentrantLock` if contention is consistently high.
- **`StampedLock` reentrancy**: unlike `ReentrantLock`, attempting to re-acquire in same thread deadlocks.
- **`synchronized` with virtual threads**: pins carrier; use `ReentrantLock` instead.

---

## 12. Interview Q&A

**Q: What is the difference between intrinsic and extrinsic locks?**
A: Intrinsic locks are the built-in monitors on every Java object, used via `synchronized`. They are implicit — the JVM acquires and releases them automatically at block entry/exit. Extrinsic locks (`ReentrantLock`, `StampedLock`, etc.) are explicit objects you create, lock, and unlock manually. Extrinsic locks add features: timed acquisition, interruptibility, multiple `Condition`s, fairness, and virtual-thread-safe parking.

**Q: Why is `notifyAll()` preferred over `notify()` with `synchronized`?**
A: `notify()` wakes one random thread from the wait-set. If that thread's condition is still false, it immediately re-waits, and the intended waiter is never woken — a missed signal. `notifyAll()` wakes everyone, each re-checks their condition, and the right one proceeds. The downside is thundering herd under contention, which is why `Condition` with precise `signal()` is preferred.

**Q: Why can't you upgrade a read lock to a write lock in `ReentrantReadWriteLock`?**
A: Both Thread A and Thread B hold read locks. Both try to acquire the write lock. The write lock requires all readers to release first — neither can release because they're both waiting for the write lock. Result: deadlock. `StampedLock.tryConvertToWriteLock()` is the only Java lock that supports safe upgrade.

**Q: What is the difference between pessimistic and optimistic locking?**
A: Pessimistic locking acquires a lock before accessing shared data, blocking other threads. It's correct regardless of contention level but hurts throughput when contention is rare. Optimistic locking reads without a lock, then validates at write time (via CAS or version check). It has near-zero overhead when contention is rare but degrades under high contention due to retry spinning.

**Q: When would you choose `StampedLock` over `ReentrantReadWriteLock`?**
A: When reads are extremely frequent (95%+) and the overhead of even a shared read lock is measurable. `StampedLock`'s optimistic read requires only a version check — no memory barrier, no queue entry. Trade-off: no reentrancy, more complex API, no `Condition` support.

**Q: What goes wrong if you call `Condition.signal()` outside of `lock.lock()`?**
A: `IllegalMonitorStateException`. `Condition` requires the caller to hold the associated lock — same invariant as `Object.notify()` requiring the caller to hold the object's monitor. The lock ensures the signal and the state change it protects are atomic.

**Q: Why does `synchronized` pin virtual threads but `ReentrantLock` doesn't?**
A: `synchronized` uses JVM object monitors, which have ownership tied to the executing thread at the OS level. The JVM cannot currently transfer monitor ownership across a carrier-thread migration. `ReentrantLock` uses `LockSupport.park()` for waiting, which is a virtual-thread-aware parking point — the JVM can unmount the virtual thread from its carrier and resume it later on a different one.
