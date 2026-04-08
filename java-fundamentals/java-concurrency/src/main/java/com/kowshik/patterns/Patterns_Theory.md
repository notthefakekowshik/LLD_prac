# Concurrency Patterns Theory

Production-grade patterns that appear in real systems and system design interviews.

---

## 1. Producer-Consumer

**Problem:** Decouple production rate from consumption rate. Producers should not block when consumers are slow (up to a limit). Consumers should not spin when producers are slow.

**Solution: BlockingQueue** (preferred)
```java
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100); // bounded

// Producer:
queue.put(task);    // Blocks if queue is full (back-pressure)
queue.offer(task, 100, MILLISECONDS); // With timeout

// Consumer:
Task task = queue.take();              // Blocks if queue is empty
Task task = queue.poll(100, MILLISECONDS); // With timeout

// Poison pill termination:
queue.put(POISON_PILL);  // Signal all consumers to stop
// Consumer checks: if (task == POISON_PILL) { queue.put(POISON_PILL); return; }
```

**Why bounded queue?** An unbounded queue allows producers to run arbitrarily ahead — memory exhaustion. A bounded queue applies back-pressure: producers slow down when consumers can't keep up.

**Low-level with wait/notify** (shown in `ProducerConsumerDemo.java`):
```java
synchronized (lock) {
    while (queue.size() == capacity) lock.wait();   // Producer waits when full
    queue.add(item);
    lock.notifyAll();
}
synchronized (lock) {
    while (queue.isEmpty()) lock.wait();             // Consumer waits when empty
    int item = queue.poll();
    lock.notifyAll();
}
```

**With ReentrantLock + Condition** (shown in `ConditionDemo.java`): use `notFull.signal()` and `notEmpty.signal()` instead of `notifyAll()` — surgical wakeups, no thundering herd.

---

## 2. Rate Limiting Algorithms

### Token Bucket (implemented in `RateLimiter.java`)
- Bucket holds up to `capacity` tokens
- Tokens are added at `rate` per second
- Each request consumes 1 token; blocked if 0 tokens available
- **Allows bursting**: a burst up to `capacity` requests can be processed instantly if tokens are available

```java
// Core CAS loop:
long currentTokens, newTokens;
do {
    currentTokens = tokens.get();
    long elapsed = (now - lastRefillTime.get()) / 1_000_000_000L;
    newTokens = Math.min(capacity, currentTokens + elapsed * rate);
} while (!tokens.compareAndSet(currentTokens, newTokens - 1));
```

### Leaky Bucket
- Requests enter a bucket; bucket leaks (processes) at a fixed rate
- Excess requests are queued or rejected
- **No bursting** — output rate is strictly constant

### Fixed Window Counter
- Time divided into fixed windows (e.g., 1 second)
- Count requests per window; reject if limit exceeded
- **Problem:** boundary spike — 100 reqs in last 500ms of window + 100 reqs in first 500ms of next = 200 reqs in 1 second

### Sliding Window Log
- Store timestamp of each request
- On each request, evict timestamps older than `windowSize`
- Count remaining — if > limit, reject
- **Accurate** but high memory (O(requests per window))

### Sliding Window Counter
- Hybrid: combine fixed window counts with a weighted approximation
- Lower memory than log, more accurate than fixed window

---

## 3. Concurrent Cache Patterns

### ReadWriteLock Cache (implemented in `ConcurrentCacheDemo.java`)
```java
ReadWriteLock lock = new ReentrantReadWriteLock();
Map<K, V> cache = new HashMap<>();

V get(K key) {
    lock.readLock().lock();
    try { return cache.get(key); }
    finally { lock.readLock().unlock(); }
}

void put(K key, V value) {
    lock.writeLock().lock();
    try { cache.put(key, value); }
    finally { lock.writeLock().unlock(); }
}
```

### Cache Stampede (Thundering Herd on Cache Miss)
When a hot key expires, N threads simultaneously discover a cache miss and all compute the value:
```java
// Problem:
V value = cache.get(key);
if (value == null) {
    value = expensiveCompute(key); // N threads all do this simultaneously!
    cache.put(key, value);
}

// Fix: use ConcurrentHashMap.computeIfAbsent
cache.computeIfAbsent(key, k -> expensiveCompute(k)); // Only one thread computes
```

### TTL Expiration
```java
// Store entry with expiry timestamp:
record Entry<V>(V value, long expiresAt) {}

// Background cleanup (ScheduledExecutorService):
scheduler.scheduleAtFixedRate(() -> {
    long now = System.currentTimeMillis();
    cache.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
}, 1, 1, MINUTES);
```

### Eviction Policies
- **LRU (Least Recently Used):** `LinkedHashMap(capacity, 0.75f, true)` — access-ordered; remove eldest when full
- **LFU (Least Frequently Used):** track frequency per key; evict lowest frequency
- **Size-based:** evict when byte size exceeds threshold (Caffeine does this)

---

## 4. Pipeline Pattern with Barriers

Multi-stage parallel computation where all workers must complete stage N before any starts stage N+1.

```java
CyclicBarrier barrier = new CyclicBarrier(workerCount, () -> mergeStageResults());

// Each worker:
for (int stage = 0; stage < STAGES; stage++) {
    processMyChunk(stage);           // Do this stage's work
    barrier.await();                 // Wait for all workers to finish this stage
    // Barrier action runs — results merged
    // Now all workers start stage+1 together
}
```

**Use case:** ETL pipelines (extract all → transform all → load all), MapReduce phases, scientific simulations with time steps.

---

## 5. Work Stealing

`ForkJoinPool` uses a per-thread **deque** (double-ended queue):
- Thread pushes its own tasks to the FRONT (LIFO — cache-friendly, reduces synchronization)
- Idle threads steal from the BACK of the busiest thread's deque (FIFO — least recently touched)

**Why LIFO for own work, FIFO for stealing?**
- Own work: LIFO keeps the most recent (likely still cached) tasks at the front
- Stealing FIFO: the oldest tasks are the largest (they haven't been subdivided yet) — steal less frequently but get bigger chunks

---

## 6. Interview Q&A

**Q: When would you use a bounded vs unbounded BlockingQueue?**
A: Almost always bounded. Unbounded queues can grow to exhaust heap memory if producers outpace consumers. Bounded queues enforce back-pressure — producers slow down automatically. The only time to use unbounded: when you can prove producers will never run faster than consumers over any meaningful window.

**Q: Implement a thread-safe bounded cache with TTL expiration.**
A: Use `ConcurrentHashMap<K, Entry<V>>` where `Entry` holds value + expiry timestamp. Reads: check expiry in `get()`; return null if expired. Writes: `put()` with `now + TTL`. Background cleanup: `ScheduledExecutorService` calling `removeIf()` on the entry set. For eviction: wrap in a `LinkedHashMap` with `removeEldestEntry()` for LRU behavior, guarded by a write lock.

**Q: Design a rate limiter supporting 1000 RPS across 10 threads.**
A: Token bucket with `AtomicLong` for token count and last refill time. On each request: CAS loop to atomically refill tokens based on elapsed time and consume 1. 1000 tokens/sec capacity. The CAS handles concurrent access without locks. For distributed rate limiting: Redis with `INCR` + `EXPIRE`, or a sliding window in Redis using sorted sets.

**Q: Difference between token bucket and leaky bucket?**
A: Token bucket allows bursting — up to `capacity` requests can be processed instantly if tokens have accumulated. Leaky bucket enforces a constant output rate regardless of bursts — excess requests are queued or dropped. Token bucket is better for user-facing APIs where short bursts are acceptable. Leaky bucket is better for protecting downstream systems with strict rate requirements.
