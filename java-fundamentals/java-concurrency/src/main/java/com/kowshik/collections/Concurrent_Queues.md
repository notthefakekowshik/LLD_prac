# Concurrent Queues

Queues are for handoff between producers and consumers. Main decision: non-blocking vs blocking/back-pressure.

---

## 1. What Problem Do Concurrent Queues Solve?

Queues decouple producers from consumers.

```text
Producer threads  →  Queue  →  Consumer threads
```

Two big families:

```text
Concurrent Queues
├── Non-blocking queues
│   ├── ConcurrentLinkedQueue
│   └── ConcurrentLinkedDeque
└── Blocking queues
    ├── ArrayBlockingQueue
    ├── LinkedBlockingQueue
    ├── PriorityBlockingQueue
    ├── DelayQueue
    ├── SynchronousQueue
    └── LinkedTransferQueue
```

Key distinction:

- **Non-blocking queue:** `offer` / `poll` return immediately. No back-pressure.
- **Blocking queue:** producer/consumer can wait. Useful for back-pressure and coordination.

---

## 2. Quick Choice

| Need | Use | Avoid When |
|---|---|---|
| non-blocking FIFO | `ConcurrentLinkedQueue` | need back-pressure |
| non-blocking double-ended queue | `ConcurrentLinkedDeque` | need blocking waits |
| bounded producer-consumer | `ArrayBlockingQueue` | need dynamic capacity |
| high-throughput producer-consumer | bounded `LinkedBlockingQueue` | you forget capacity |
| priority task queue | `PriorityBlockingQueue` | need FIFO fairness |
| delayed tasks / TTL expiry | `DelayQueue` | need bounded memory |
| direct handoff, no buffering | `SynchronousQueue` | producers must not block |
| handoff + optional buffering | `LinkedTransferQueue` | need hard capacity |

---

## 3. Core Queue Method Behavior

BlockingQueue gives four styles of methods:

| Action | Throws Exception | Special Value | Blocks | Times Out |
|---|---|---|---|---|
| Insert | `add(e)` | `offer(e)` | `put(e)` | `offer(e, time, unit)` |
| Remove | `remove()` | `poll()` | `take()` | `poll(time, unit)` |
| Examine | `element()` | `peek()` | - | - |

Practical rule:

- Use `put/take` for simple producer-consumer demos.
- Use `offer(timeout)` / `poll(timeout)` in services so shutdown can be checked.
- Use `drainTo(batch, max)` for batch consumers — fewer lock acquisitions than looping `poll()`. Pattern: block on `poll(timeout)` for the first item, then `drainTo(batch, max)` to grab everything currently available in one call.

See `executors/BlockingQueue_Theory.md` for full deep dive.

---

## 4. Non-Blocking Queues

### 4.1 `ConcurrentLinkedQueue<E>`

Unbounded, lock-free FIFO queue.

Use cases:

- async event buffer when blocking is not desired
- collecting completed task IDs
- lightweight handoff where consumers poll

Example:

```java
ConcurrentLinkedQueue<String> completedJobs = new ConcurrentLinkedQueue<>();

completedJobs.offer(jobId);
String next = completedJobs.poll(); // null if empty
```

Watch out:

- no back-pressure.
- consumers must poll; no blocking `take`.
- unbounded growth possible.

Internals that matter:

- Lock-free linked-node queue.
- Producers CAS the tail link.
- Consumers CAS the head forward.
- `offer` / `poll` do not block; under contention, threads retry CAS.
- Weakly consistent iteration.

### Flow

```text
offer(x)
├── create node
├── CAS current tail.next from null → node
└── swing tail forward eventually

poll()
├── read head.next
├── if null → queue empty
└── CAS head forward and return item
```

This is good when blocking is undesirable, but it means producers can outrun consumers forever.

---

### 4.2 `ConcurrentLinkedDeque<E>`

Non-blocking double-ended queue.

Use cases:

- local deque for work-stealing-like designs
- recent activity buffer where both ends matter
- concurrent stack/deque behavior

Example:

```java
ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>();

deque.addFirst("urgent");
deque.addLast("normal");

String next = deque.pollFirst();
```

Use when both head and tail operations matter. If only FIFO is needed, use `ConcurrentLinkedQueue`.

Internals that matter:

- Lock-free linked nodes with both previous and next links.
- Head/tail operations use CAS.
- Removed nodes can remain temporarily linked until traversal helps unlink them.
- Good for concurrent deque operations, not for blocking coordination.

---

## 5. Blocking Queues

### 5.1 `ArrayBlockingQueue<E>`

Bounded FIFO queue backed by fixed-size array.

Use cases:

- bounded worker queue
- back-pressure between producer and consumer
- production-safe `ThreadPoolExecutor` work queue

Example:

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);

queue.put(task);        // blocks when full
Task task = queue.take(); // blocks when empty
```

Watch out:

- single lock; producer/consumer contend more than `LinkedBlockingQueue`.
- capacity fixed.

Internals that matter:

- Backed by fixed-size array ring buffer.
- Uses one `ReentrantLock` for both put and take.
- Uses two conditions: not-full and not-empty.
- Optional fairness mode serves waiting threads roughly FIFO, usually with lower throughput.

### Flow

```text
put(e)
├── acquire single lock
├── while count == capacity → wait on notFull
├── insert at tail index
├── count++
└── signal notEmpty

take()
├── acquire same lock
├── while count == 0 → wait on notEmpty
├── remove from head index
├── count--
└── signal notFull
```

Trade-off:

- less GC than linked queue.
- lower concurrency because producer and consumer share one lock.

---

### 5.2 `LinkedBlockingQueue<E>`

FIFO queue backed by linked nodes. Optionally bounded.

Use cases:

- producer-consumer with high throughput
- task queue when capacity is known but can be larger

Example:

```java
BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(500);
```

Watch out:

- default constructor is effectively unbounded.
- unbounded queue can cause OOM if producers outrun consumers.

Internals that matter:

- Backed by linked nodes.
- Uses separate locks for put and take.
- Producers and consumers can often proceed concurrently.
- Uses atomic count to coordinate capacity and empty/full state.
- More allocation per element than `ArrayBlockingQueue`.

### Flow

```text
put(e)
├── acquire putLock
├── while count == capacity → wait on notFull
├── link new node at tail
├── increment atomic count
└── if queue was empty → signal notEmpty

take()
├── acquire takeLock
├── while count == 0 → wait on notEmpty
├── unlink head node
├── decrement atomic count
└── if queue was full → signal notFull
```

This is why producer and consumer can overlap more than `ArrayBlockingQueue`.

Interview line:

> Always pass capacity in production. Raw `new LinkedBlockingQueue<>()` is a common OOM footgun.

---

### 5.3 `PriorityBlockingQueue<E>`

Unbounded blocking queue ordered by priority.

Use cases:

- priority tasks
- retry jobs where smaller retry count or earlier time wins
- background jobs with SLA tiers

Example:

```java
record Job(int priority, String name) implements Comparable<Job> {
    public int compareTo(Job other) {
        return Integer.compare(this.priority, other.priority);
    }
}

BlockingQueue<Job> queue = new PriorityBlockingQueue<>();
queue.put(new Job(1, "critical"));
queue.put(new Job(10, "low"));
```

Watch out:

- unbounded.
- equal priority ordering is not guaranteed FIFO.
- low-priority tasks can starve.

Internals that matter:

- Backed by heap-style priority queue.
- Uses one lock for queue mutations.
- `take()` blocks only when empty; `put()` never blocks because queue is unbounded.
- Head is smallest according to natural order/comparator.

### Priority Gotcha

```java
record Job(int priority, long seq, String name) implements Comparable<Job> {
    public int compareTo(Job other) {
        int byPriority = Integer.compare(this.priority, other.priority);
        return byPriority != 0 ? byPriority : Long.compare(this.seq, other.seq);
    }
}
```

If FIFO among same priority matters, include sequence number yourself.

---

### 5.4 `DelayQueue<E extends Delayed>`

Unbounded blocking queue where item is available only after delay expires.

Use cases:

- cache TTL cleanup
- unpaid order timeout
- retry with backoff
- session expiry

Example:

```java
class RetryTask implements Delayed {
    private final long runAtMillis;

    RetryTask(long delayMillis) {
        this.runAtMillis = System.currentTimeMillis() + delayMillis;
    }

    public long getDelay(TimeUnit unit) {
        return unit.convert(runAtMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public int compareTo(Delayed other) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
}
```

Watch out:

- unbounded.
- elements must implement `Delayed`.
- good for timing, not general priority.

Demo: [DelayQueueDemo.java](DelayQueueDemo.java)

Internals that matter:

- Backed by priority queue ordered by remaining delay.
- Uses one lock and a leader-follower waiting pattern.
- Only one waiting thread times the head delay; others wait normally.
- `take()` returns only when head delay is expired.
- `put()` does not block because queue is unbounded.

### Flow

```text
take()
├── if queue empty → wait
├── look at head delay
├── if delay <= 0 → remove and return
├── one leader waits exactly remaining delay
└── other consumers wait normally
```

This avoids waking every consumer on every delay check.

---

### 5.5 `SynchronousQueue<E>`

Zero-capacity queue. Producer and consumer meet directly.

Use cases:

- direct handoff
- `newCachedThreadPool` internals
- custom executor where idle worker must take task immediately

Example:

```java
BlockingQueue<Task> handoff = new SynchronousQueue<>();

handoff.put(task); // blocks until consumer takes it
```

Watch out:

- no buffering.
- if no consumer is ready, producer blocks.
- dangerous with fixed-size pools if all workers are busy.

Internals that matter:

- Stores no elements.
- A producer must match directly with a consumer.
- Non-fair mode generally uses stack-like matching for throughput.
- Fair mode uses queue-like matching for FIFO fairness.
- Used by cached thread pools to avoid task buffering.

### Flow

```text
put(task)
├── if consumer already waiting → hand task directly
└── else producer waits

take()
├── if producer already waiting → receive task directly
└── else consumer waits
```

That is why `size()` is always zero.

Interview line:

> `SynchronousQueue` is not a queue for storage; it is a rendezvous point.

---

### 5.6 `LinkedTransferQueue<E>`

Unbounded queue supporting both buffering and direct transfer.

Use cases:

- low-latency producer-consumer where direct handoff is preferred
- message pipeline where producer can wait for consumer if needed

Example:

```java
LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();

queue.transfer("event"); // waits until consumer receives
queue.offer("event");    // queues without waiting
```

Watch out:

- unbounded.
- extra API surface; use only if `transfer` semantics matter.

Internals that matter:

- Based on dual-queue idea: data nodes and request nodes can match.
- `transfer(e)` waits until consumer receives element.
- `tryTransfer(e)` succeeds only if consumer is already waiting.
- `offer(e)` can buffer like normal queue.
- Useful when direct handoff is preferred but buffering is allowed.

### Method Meaning

| Method | Meaning |
|---|---|
| `offer(e)` | enqueue and return |
| `transfer(e)` | wait until consumer receives |
| `tryTransfer(e)` | transfer only if consumer already waiting |
| `tryTransfer(e, timeout, unit)` | wait up to timeout for consumer |

---

## 6. Full Comparison

| Queue | Bounded | Blocks Producer | Blocks Consumer | Internal Shape | Best Use |
|---|---|---|---|---|---|
| `ConcurrentLinkedQueue` | no | no | no | lock-free linked nodes | non-blocking FIFO |
| `ConcurrentLinkedDeque` | no | no | no | lock-free doubly linked nodes | both-end operations |
| `ArrayBlockingQueue` | yes | when full | when empty | array + one lock | bounded back-pressure |
| `LinkedBlockingQueue` | optional | when full | when empty | linked nodes + two locks | high-throughput producer-consumer |
| `PriorityBlockingQueue` | no | never | when empty | heap + one lock | priority jobs |
| `DelayQueue` | no | never | until delay expires | priority queue by delay | TTL/retry/scheduling |
| `SynchronousQueue` | zero | until consumer | until producer | direct rendezvous | no buffering handoff |
| `LinkedTransferQueue` | no | only `transfer` | when empty | dual queue | handoff + buffering |

---

## 7. Common Traps

| Trap | Correct Thought |
|---|---|
| `ConcurrentLinkedQueue` gives back-pressure | It does not block; it can grow without bound |
| `LinkedBlockingQueue` is safe by default | Default capacity is huge; pass capacity |
| `PriorityBlockingQueue` preserves FIFO for same priority | It does not guarantee FIFO among equal priority items |
| `SynchronousQueue` stores tasks | It has zero capacity; it only hands off |
| `take()` is always best | It can block forever; services often need `poll(timeout)` for shutdown |
| Unbounded queue is convenient | It can turn slow consumers into memory leaks |

---

## 8. Interview Q&A

**Q: `ArrayBlockingQueue` vs `LinkedBlockingQueue`?**
A: Array uses one lock and fixed array, less GC, stricter bound. Linked uses separate put/take locks, often higher throughput, but allocates nodes and must be bounded manually.

**Q: Why does `newCachedThreadPool()` use `SynchronousQueue`?**
A: It wants no buffering. If no idle worker accepts task immediately, pool creates another thread.

**Q: What queue should I use for production `ThreadPoolExecutor`?**
A: Usually bounded `ArrayBlockingQueue` or bounded `LinkedBlockingQueue`, depending on memory/throughput trade-off. Avoid unbounded queues by default.

**Q: How do I consume batches efficiently?**
A: Use `poll(timeout)` for first item, then `drainTo(batch, max)` to drain available items with fewer lock acquisitions.

---

## 9. Mental Model

Use non-blocking queues when polling is acceptable; use blocking queues when producer-consumer coordination or back-pressure matters.
