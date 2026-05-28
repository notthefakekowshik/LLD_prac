# DelayQueue Internals

A deep-dive into how `java.util.concurrent.DelayQueue` works under the hood — why it beats a hand-rolled `PriorityQueue + Thread.sleep()` scheduler, and what it costs.

---

## 1. What DelayQueue Is Built From

`DelayQueue<E extends Delayed>` is not a new data structure. It is a thin wrapper around three things:

```
DelayQueue
  ├── PriorityQueue<E>       — the actual heap (min-heap by remaining delay)
  ├── ReentrantLock lock     — single lock guards all queue mutations
  └── Condition available    — signaling channel between producer and consumer
```

The heap orders elements by `getDelay()` — the element with the **smallest remaining delay** sits at the root. When that delay reaches zero, the element is "ready" and `take()` returns it.

---

## 2. The `Delayed` Contract

Every element in a `DelayQueue` must implement `Delayed`:

```java
public interface Delayed extends Comparable<Delayed> {
    long getDelay(TimeUnit unit);  // remaining time until "ready"
}
```

`getDelay()` is called dynamically — not stored. Every time the queue peeks at an element, it calls `getDelay(NANOSECONDS)` fresh against `System.nanoTime()`. This means:

- No need to update the element when time passes — time passes on its own
- Ordering is based on **remaining delay at the moment of comparison**

In this codebase, `DelayedTask` wraps `Task` and implements this:

```java
@Override
public long getDelay(TimeUnit unit) {
    long delayMillis = task.getNextExecutionTime() - System.currentTimeMillis();
    return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
}
```

---

## 3. `put()` — How a Task Enters the Queue

```java
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        q.offer(e);                          // O(log n) — heap sift-up
        if (q.peek() == e) {                 // did this become the new head?
            leader = null;                   // invalidate any existing leader
            available.signal();              // wake one waiting thread
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

**The key line:** `if (q.peek() == e)` — if the just-inserted element is now the earliest (new head of heap), the current leader's wait time is stale. The leader is cleared and `available.signal()` wakes one waiting thread so it can recalculate the correct wait time.

**If the new element is NOT the head** — no signal is sent. The existing leader is already waiting for something earlier, so nothing changes.

---

## 4. `take()` — The Leader-Follower Pattern

This is the most important part. `take()` must block until the head element's delay reaches zero, **without burning CPU**.

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();              // O(1) — read heap root

            if (first == null) {
                available.await();           // queue empty — block indefinitely
            } else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0L) {
                    return q.poll();         // O(log n) — remove root, sift-down
                }

                first = null;               // don't hold reference while waiting

                if (leader != null) {
                    available.await();       // FOLLOWER — another thread is already
                                            // waiting for the head; just block
                } else {
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;     // LEADER — this thread claims the wait
                    try {
                        available.awaitNanos(delay);  // timed wait — releases lock
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();             // wake next thread if queue still has work
        lock.unlock();
    }
}
```

### What "leader" means

At any point, at most **one thread is the leader**. The leader is the only thread doing a timed wait — `awaitNanos(delay)`. All other threads (followers) call `await()` with no timeout and block indefinitely.

```
Thread A (leader)  →  awaitNanos(3000ms)   — wakes up when head task is ready
Thread B (follower) →  await()             — blocks until signaled
Thread C (follower) →  await()             — blocks until signaled
```

When the leader's wait expires, it re-acquires the lock, calls `q.poll()`, and before returning, calls `available.signal()` to promote one follower to the next leader.

### Why this is better than everyone timing out

Without the leader pattern, if 10 threads are all waiting, all 10 would call `awaitNanos(delay)`. When the delay expires, all 10 wake, compete for the lock, and 9 immediately go back to sleep after one wins. That's 9 unnecessary wakeups per task — **thundering herd**.

With the leader pattern: exactly one thread times out. The other 9 sleep forever until signaled.

---

## 5. Full Lifecycle of One Task

```
Producer thread                       Dispatcher thread (take())
───────────────                       ──────────────────────────
scheduleOnce("X", task, 5s)
  lock.lock()
  q.offer(task)         ──────────►  if new head → available.signal()
  lock.unlock()                         dispatcher wakes, re-reads delay
                                        delay = 4999ms (just under 5s)
                                        leader = dispatcherThread
                                        awaitNanos(4999ms)
                                        ... OS parks thread, zero CPU ...
                                        [4999ms later]
                                        lock re-acquired
                                        q.poll() → returns task   O(log n)
                                        signal() → wake next waiter
                                        task submitted to worker pool
```

---

## 6. Time Complexity Summary

| Operation | Complexity | Notes |
|---|---|---|
| `offer` / `put` | O(log n) | Heap sift-up |
| `take` (blocking) | O(log n) | Sift-down on removal; zero CPU while waiting |
| `peek` | O(1) | Read heap root, no removal |
| `poll` (non-blocking) | O(log n) if ready, O(1) if not ready | Returns null immediately if head not ready |
| `remove(Object)` | O(n) | Linear scan — no element index in heap |
| `size` | O(1) | Maintained as field |
| `clear` | O(n) | Nulls all references |

**The cancellation gap:** `remove(Object)` is O(n) because a heap has no element index — to remove by task ID you'd need an external map. This is why `DelayQueueScheduler` maintains a `ConcurrentHashMap<taskId, Task>` alongside the queue: cancel marks the task in the map O(1), and the dispatcher simply skips cancelled tasks when it dequeues them.

---

## 7. PriorityQueue vs DelayQueue — Side by Side

| Aspect | PriorityQueue + sleep | DelayQueue |
|---|---|---|
| Waiting | `Thread.sleep(POLL_INTERVAL)` — wakes on fixed cadence | `awaitNanos(exactDelay)` — wakes exactly when task is ready |
| CPU when idle | Burns cycles polling | Zero — thread parked by OS |
| Timing precision | ±POLL_INTERVAL drift | Nanosecond precision |
| New task with shorter delay | Dispatcher doesn't know until next poll | `offer()` calls `signal()` — dispatcher wakes immediately |
| Cancel by ID | O(n) scan | O(n) heap scan — but paired with external map gives O(1) |
| Thread-safety | Must manage own `ReentrantLock` | Internally synchronized |
| Multiple consumers | Complex coordination required | Leader-follower built in |

---

## 8. Limitations

**Single lock bottleneck.** All `offer`, `take`, `poll`, `peek` share one `ReentrantLock`. Under very high throughput (10K+ tasks/sec), this becomes a contention point. Alternatives: `ScheduledThreadPoolExecutor` (uses a custom lock-free heap variant) or a Hashed Wheel Timer.

**O(n) remove.** Despite the external HashMap trick for cancellation, the element remains physically in the heap until dequeued. A cancelled task wastes one O(log n) dequeue cycle when the dispatcher picks it up and discards it.

**Unbounded.** `DelayQueue` has no capacity limit. Scheduling 1M tasks fills 1M heap slots. Memory-sensitive systems need a cap at the application layer.

**No fairness.** `ReentrantLock` is used without the fairness flag (default: unfair). Under high contention, some threads may be starved.
