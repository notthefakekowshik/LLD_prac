# AbstractQueuedSynchronizer (AQS) — What It Is, What’s Inside, Interview Scenarios

> **Goal:** understand *how* Java builds locks/semaphores/latches without `synchronized`, and why these usually work well with **virtual threads** (they park/unpark instead of holding a JVM monitor while blocking).

---

## 1. What is AQS (in one sentence)?
`AbstractQueuedSynchronizer` is a **framework for building synchronizers** (locks, semaphores, latches) by managing:

- a single **`state`** integer (the “permit / hold / latch” value), and
- a **FIFO wait queue** of threads that failed to acquire and must block.

Concrete synchronizers define only *acquire/release rules*; AQS handles the hard parts: queuing, parking, waking, timeouts, and interrupt handling.

---

## 2. What’s inside AQS?
Think of AQS as: **`state` + queue + CAS + park/unpark + (optional) conditions**.

### 2.1 The `state` field (the whole game)
AQS has an internal `volatile int state` that represents the synchronizer’s current “resource count”.

Typical meanings:

- **Mutex / ReentrantLock**: `state == 0` means unlocked, `state > 0` means locked; the value often equals reentrancy depth.
- **Semaphore**: `state` is the number of remaining permits.
- **CountDownLatch**: `state` is the remaining count until it opens (reaches 0).

Critical property:

- Updates to `state` are done via **CAS** (compare-and-swap), ensuring correctness under contention without `synchronized`.

### 2.2 Exclusive vs Shared modes
AQS supports two acquisition styles:

- **Exclusive**: at most one thread “owns” the synchronizer at a time
  Examples: `ReentrantLock`, `ReentrantReadWriteLock.WriteLock`

- **Shared**: multiple threads can “hold” simultaneously up to some limit
  Examples: `Semaphore`, `CountDownLatch`, `ReentrantReadWriteLock.ReadLock`

The difference matters because waking logic differs: shared acquisition may wake **multiple** waiters if capacity allows.

### 2.3 The wait queue (a CLH-style FIFO queue)
When a thread fails to acquire, AQS enqueues it into a **linked queue** of `Node`s (often described as “CLH-style”).

High-level idea:

- Each waiting thread is represented by a `Node`.
- Nodes are linked via `prev/next`.
- The queue has `head`/`tail`.
- Only threads at/near the head are eligible to retry acquisition (reduces stampede).

This queue is the reason AQS can provide “mostly FIFO” behavior (especially in fair variants).

### 2.4 Parking and unparking (blocking without monitors)
Instead of blocking on object monitors, AQS uses `LockSupport`:

```java
LockSupport.park(this);                        // block current thread (with blocker hint)
LockSupport.unpark(thread);                    // unblock a specific thread
LockSupport.parkNanos(this, nanosTimeout);     // block with a timeout
LockSupport.parkUntil(this, deadlineMillis);   // block until absolute deadline
```

Key properties of `LockSupport`:

- `park()` can **spuriously return** (wake up without `unpark()` being called) — callers must always re-check their condition in a loop.
- `unpark()` is safe to call **before** `park()` — the “permit” is stored and consumed on the next `park()`, so there is no lost-wakeup problem.
- Each thread has exactly **one permit** (boolean, not a counter): multiple `unpark()` calls still produce only one free pass.

Why that matters:

- parking is **explicit** and controlled by AQS
- it integrates with timeouts / interrupts in a predictable way
- for **virtual threads**, parking points are generally **unmount-friendly**, so carriers aren’t pinned like with “block while holding a monitor”

### 2.5 Ownership (exclusive mode only)
For exclusive synchronizers, AQS tracks the “owner thread” concept (e.g., `ReentrantLock` tracks which thread holds it).
This is how **reentrancy** is implemented: if owner re-acquires, it increments `state` instead of blocking.

### 2.6 `ConditionObject` (wait-sets without `Object.wait/notify`)
AQS provides an internal `ConditionObject` that implements `Condition`.

Key idea:

- `await()` moves the current thread from the **AQS sync queue** to a **condition queue** (a separate wait queue)
- `signal()` transfers one waiter from the **condition queue** back to the **sync queue**

This mirrors `wait/notify`, but it is **explicitly tied to a particular lock** and does not use the object monitor.

```java
ReentrantLock lock = new ReentrantLock();
Condition notEmpty = lock.newCondition();
Condition notFull  = lock.newCondition();

// Producer:
lock.lock();
try {
    while (queue.isFull()) notFull.await();   // releases lock, parks thread
    queue.add(item);
    notEmpty.signal();                         // moves one waiter back to sync queue
} finally { lock.unlock(); }

// Consumer:
lock.lock();
try {
    while (queue.isEmpty()) notEmpty.await();
    Item item = queue.poll();
    notFull.signal();
} finally { lock.unlock(); }
```

Practical rule:

- use `Condition` with `ReentrantLock`
- use `wait/notify` only with `synchronized`

---

## 3. What do you implement to build an AQS-based synchronizer?
You don’t reimplement queuing. You implement the *policy*:

- **Exclusive**
  - `tryAcquire(int arg)` — return `true` if acquired
  - `tryRelease(int arg)` — return `true` if fully released

- **Shared**
  - `tryAcquireShared(int arg)` — return value has 3 meanings:
    - **negative**: failed to acquire (enqueue the thread)
    - **zero**: acquired, but no capacity left for more shared holders
    - **positive**: acquired, and there may be capacity for more (AQS propagates and wakes next waiters)
  - `tryReleaseShared(int arg)` — return `true` if release may allow a waiter to acquire

AQS then exposes ready-made APIs such as:

- `acquire(...)`, `release(...)`
- `acquireInterruptibly(...)`
- `tryAcquireNanos(...)` (timeouts)
- `acquireShared(...)`, `releaseShared(...)`

Mental model: you write “is resource available?” + “update state safely”; AQS provides “how to wait correctly when it’s not”.

---

## 4. How core JUC classes map to AQS

| Class | Mode | `state` meaning |
|---|---|---|
| `ReentrantLock` | Exclusive | Hold count (0 = unlocked, N = reentrant depth) |
| `Semaphore` | Shared | Remaining permits |
| `CountDownLatch` | Shared | Remaining count; acquire succeeds only when 0 |
| `ReentrantReadWriteLock` | Both | Upper 16 bits = shared (read) hold count; lower 16 bits = exclusive (write) hold count |

### `ReentrantReadWriteLock` state encoding (deep-dive)
A single `int state` encodes **both** read and write counts by bit-packing:

```
state = [ 16 bits: read count | 16 bits: write count ]

Read count  = state >>> 16
Write count = state & 0xFFFF    (lower 16 bits)
```

- Write lock acquired: CAS lower 16 bits from 0 → 1 (exclusive).
- Read lock acquired: CAS add `0x00010000` to upper 16 bits (shared increment).
- Maximum reentrancy depth for each is 65,535.

---

## 5. Fair vs Non-fair: what changes internally?
Fairness is mostly about **who is allowed to barge**.

- **Non-fair**: a new contender may acquire immediately if `state` is available, even if there are queued threads (higher throughput, possible starvation).
- **Fair**: acquisition checks “are there queued predecessors?” before taking available state (better latency fairness, lower throughput under contention).

Interview trade-off line:

> Fair locks reduce starvation but usually reduce throughput due to more queue coordination and fewer lucky fast-path acquisitions.

---

## 6. Building a custom AQS synchronizer

### Example A: Non-reentrant exclusive mutex

```java
class SimpleMutex {
    private final Sync sync = new Sync();

    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            return compareAndSetState(0, 1);   // CAS 0 → 1; fails if already locked
        }

        @Override
        protected boolean tryRelease(int arg) {
            setState(0);                        // plain write is safe: only owner calls this
            return true;
        }
    }

    public void lock()   { sync.acquire(1); }
    public void unlock() { sync.release(1); }
}
```

### Example B: Binary latch (open-once gate)

```java
class BinaryLatch {
    private final Sync sync = new Sync();

    private static class Sync extends AbstractQueuedSynchronizer {
        Sync() { setState(1); }   // 1 = closed; 0 = open

        @Override
        protected int tryAcquireShared(int arg) {
            return getState() == 0 ? 1 : -1;  // positive = acquired; negative = enqueue
        }

        @Override
        protected boolean tryReleaseShared(int arg) {
            setState(0);   // open the gate permanently
            return true;   // true → AQS propagates and wakes all waiters
        }
    }

    public void await() { sync.acquireShared(1); }   // blocks if closed
    public void open()  { sync.releaseShared(1); }   // opens for everyone
}
```

### Example C: Reentrant mutex with owner tracking

```java
class ReentrantMutex {
    private final Sync sync = new Sync();

    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, 1)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            } else if (current == getExclusiveOwnerThread()) {
                setState(c + 1);   // reentrant: increment depth
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            int c = getState() - 1;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            if (c == 0) setExclusiveOwnerThread(null);
            setState(c);
            return c == 0;   // fully released only when depth reaches 0
        }
    }

    public void lock()   { sync.acquire(1); }
    public void unlock() { sync.release(1); }
}
```

---

## 7. Interview scenarios (high-signal)
Use these to practice explaining AQS, not memorizing method names.

### Scenario A: “Implement a mutex lock”
**Prompt:** build a lock with `lock()` / `unlock()` and optional reentrancy.

What interviewer wants:

- `state` transitions 0 → 1 on acquire, 1 → 0 on release
- owner tracking for reentrancy: same thread increments state
- `unlock()` validates owner (otherwise throws)
- use AQS acquire/release path to handle contention

### Scenario B: “Implement a semaphore”
**Prompt:** allow up to N concurrent entries.

Talking points:

- `state` initialized to N
- `tryAcquireShared` decrements state if > 0 (returns positive), else returns negative (enqueue)
- `tryReleaseShared` increments state and returns true to wake next waiter

### Scenario C: “Implement a latch / gate”
**Prompt:** “many threads wait; one action opens the gate”.

Talking points:

- `state` as remaining count
- `tryAcquireShared` returns positive only when state is 0
- countdown decrements state; when it hits 0, `tryReleaseShared` returns true, waking all waiters

### Scenario D: “Explain `Condition` vs `wait/notify`”
**Prompt:** why not call `wait()` when using `ReentrantLock`?

Answer:

- `wait/notify` are **monitor** operations tied to `synchronized`
- `Condition` is the lock’s structured wait-set: `await/signal`
- `await()` releases the lock atomically and parks; on signal it re-contends and re-acquires before returning

### Scenario E: “Interruptible acquisition”
**Prompt:** difference between `lock()` and `lockInterruptibly()`.

Answer:

- `lock()` ignores interrupts while waiting (interrupt status set but it keeps waiting)
- `lockInterruptibly()` responds by throwing `InterruptedException`

Why it matters:

- cancellation / time-bounded systems (request handling, thread pools)

### Scenario F: “Timeouts”
**Prompt:** design a lock acquisition with a timeout.

Answer:

- AQS already supports timed acquisition (`tryAcquireNanos`)
- internally: enqueue + `parkNanos` with deadline + retry/abort logic

### Scenario G: “Starvation and fairness”
**Prompt:** you see occasional requests stuck for seconds under load.

Answer:

- non-fair locks can starve; consider fair lock if tail latency matters
- but fair locks reduce throughput; benchmark
- sometimes fix is **reducing critical section time**, not changing fairness

### Scenario H: “Why virtual threads behave better with AQS than monitors”
**Prompt:** why does `synchronized` pin but `ReentrantLock` doesn’t (usually)?

Answer:

- AQS blocks via `park/unpark`, which is virtual-thread-friendly (unmount)
- monitors + blocking in a synchronized region can pin carriers

---

## 8. Common pitfalls interviewers probe

- **Holding locks across I/O**: prevents concurrency regardless of pinning
- **Calling `signal()` without holding the lock**: illegal and racy
- **Using `notify()` with `ReentrantLock`**: wrong primitive — use `Condition.signal()`
- **Assuming fairness fixes deadlocks**: fairness doesn’t prevent deadlock; it impacts ordering/starvation
- **Ignoring spurious wakeups**: always re-check the predicate in a `while` loop, never `if`
- **`tryAcquireShared` wrong return sign**: returning 0 when capacity remains prevents AQS from propagating wakeups to other waiters

---

## 9. Decision matrix

| Need | Best choice |
|---|---|
| Simple mutual exclusion, no extras | `synchronized` |
| Timed / interruptible / tryLock | `ReentrantLock` |
| Limit N concurrent accesses | `Semaphore` |
| One-shot “all threads go” signal | `CountDownLatch` |
| Read-heavy shared data | `ReentrantReadWriteLock` |
| Extremely read-heavy (optimistic read) | `StampedLock` |
| Custom synchronizer with shared/exclusive policy | Extend `AbstractQueuedSynchronizer` |
| Multiple conditions on one lock | `ReentrantLock` + `lock.newCondition()` |

---

## 10. Interview Q&A

**Q: What is AQS and why does it exist?**
A: `AbstractQueuedSynchronizer` is the backbone of most `java.util.concurrent` synchronizers. It provides a reusable FIFO wait queue, park/unpark-based blocking, CAS-based state management, and timeout/interrupt support. Without it, every lock and semaphore would have to re-implement this complex machinery.

**Q: What do you override to build a custom AQS synchronizer?**
A: For exclusive mode: `tryAcquire` and `tryRelease`. For shared mode: `tryAcquireShared` and `tryReleaseShared`. You only implement the policy (“is the resource available?”); AQS handles queuing and parking.

**Q: What are the three return value meanings of `tryAcquireShared`?**
A: Negative = failed, enqueue the thread. Zero = acquired, but no remaining capacity for more. Positive = acquired, and AQS should propagate and try to wake the next queued waiter as well.

**Q: Why can `LockSupport.park()` return without `unpark()` being called?**
A: Spurious wakeups. The JVM spec allows this on some platforms. This is why every `park()` call in AQS is inside a loop that re-checks the acquire condition before re-parking.

**Q: How does `ReentrantReadWriteLock` fit two counts into one AQS `state` int?**
A: Bit-packing: upper 16 bits = shared (read) hold count, lower 16 bits = exclusive (write) hold count. Read count: `state >>> 16`. Write count: `state & 0xFFFF`. Maximum reentrancy depth for each is 65,535.

**Q: Fair lock vs non-fair — which has higher throughput and why?**
A: Non-fair. When a lock is released, a newly arriving thread can immediately barge in via CAS without waking the parked queue head. This avoids the overhead of `unpark()` and context switching. Fair locks must wake the queued thread first, which is slower. Use fair when starvation prevention outweighs throughput.

**Q: What happens to `await()` if the thread is interrupted?**
A: `Condition.await()` throws `InterruptedException` if the thread is interrupted while waiting. The lock is re-acquired before the exception propagates, maintaining the invariant that you always hold the lock when `await()` returns.

**Q: How does `unpark()` before `park()` avoid a lost wakeup?**
A: `LockSupport` gives each thread a single permit. Calling `unpark()` deposits the permit even if the thread isn’t parked yet. When `park()` is called later, it sees the permit and returns immediately without blocking, eliminating the race between “check condition” and “park”.

---

## 11. One-liners to memorize (but understand)

- **AQS**: “CAS a `state`, and if you can’t, join a queue and park.”
- **Condition**: “a second queue for threads waiting on a predicate, not on the lock itself.”
- **Fairness**: “trades throughput for more predictable wait time.”
- **`tryAcquireShared`**: “negative = fail; zero = got it, no room left; positive = got it, wake the next one too.”
- **`LockSupport.park`**: “may spuriously return — always check your predicate in a loop.”
