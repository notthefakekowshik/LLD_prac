# Advanced Topics Theory

Modern Java concurrency, JVM internals, and production-grade system design.

---

## 1. Virtual Threads — Project Loom (Java 21)

### The Problem with Platform Threads

Platform threads map 1:1 to OS threads. Each platform thread has a stack of ~1MB. Starting tens of thousands of threads:
- Memory: 10,000 threads × 1MB = 10GB just for stacks
- Context switching: OS kernel involvement, ~1–10µs per switch

Web servers blocked on I/O (DB queries, HTTP calls) waste carrier threads. The thread is parked but consuming memory.

### Virtual Threads

Virtual threads are managed entirely by the JVM — not the OS.

```java
// Create a virtual thread:
Thread vt = Thread.ofVirtual().name("worker").start(() -> doWork());

// Virtual thread executor (one virtual thread per task):
try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        exec.submit(() -> handleRequest());
    }
} // auto-shutdown + join
```

**How it works:**
- Virtual threads are **mounted** on a **carrier thread** (platform thread from `ForkJoinPool.commonPool()`)
- When a virtual thread blocks on I/O, it **unmounts** from the carrier (the carrier is free to run another virtual thread)
- When I/O completes, the virtual thread is **remounted** on any available carrier

```
Virtual Thread V1 ──I/O call──> unmounts from Carrier C1
Carrier C1 ──────────────────> runs V2, V3, V4...
I/O completes ───────────────> V1 remounts on any free Carrier
```

Context switch: JVM-level mount/unmount ~100ns (no kernel involvement). vs platform thread ~1–10µs.

### Pinning — The Critical Gotcha

A virtual thread is **pinned** to its carrier when:
1. Inside a `synchronized` block or method
2. Inside a native method or foreign function

When pinned, the virtual thread CANNOT unmount — it blocks the carrier thread:
```java
// THIS PINS — blocks the carrier during the entire I/O wait:
synchronized (this) {
    result = httpClient.send(request); // I/O inside synchronized!
}

// FIX — use ReentrantLock instead:
lock.lock();
try {
    result = httpClient.send(request); // Virtual thread can unmount here
} finally { lock.unlock(); }
```

**Detect pinning:** JVM flag `-Djdk.tracePinnedThreads=full` — logs a stack trace when a virtual thread pins.

### Structured Concurrency (Java 21 Preview)

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> user    = scope.fork(() -> fetchUser(id));
    Future<String> orders  = scope.fork(() -> fetchOrders(id));
    scope.join();           // Wait for both
    scope.throwIfFailed();  // Propagate any exception
    return new Response(user.resultNow(), orders.resultNow());
}
// When scope closes: any still-running tasks are cancelled automatically
```

**vs CompletableFuture.allOf():** Structured concurrency enforces lifecycle — tasks cannot outlive their scope. This prevents the common `CompletableFuture` bug where a task runs after its result is no longer needed.

### When to Use Virtual Threads

| Workload | Use |
|---|---|
| I/O-bound (DB, HTTP, filesystem) | Virtual threads — huge win |
| CPU-bound computation | Platform threads — virtual threads offer no advantage |
| One-thread-per-request server | Virtual threads — scale to millions of concurrent requests |
| Short tasks submitted to a pool | Either — overhead is similar |

---

## 2. Arrays.parallelSort and ForkJoinPool Internals

```java
int[] arr = new int[10_000_000];
Arrays.parallelSort(arr); // Uses ForkJoinPool.commonPool()
```

**Algorithm:** Parallel merge-sort variant
1. Split array into chunks until chunk size < threshold (~8192 elements)
2. Sort each chunk sequentially (fast sequential sort for small arrays)
3. Merge sorted chunks in parallel (tree of merge tasks)

**When parallelSort is NOT faster:**
- Array size < ~8192 — sequential sort wins (no fork overhead)
- Memory-bandwidth bound — copying arrays for merge saturates memory bus regardless of cores
- Heavily loaded `commonPool` — tasks compete with other ForkJoin tasks

**Thread pool sizing formula:**
- CPU-bound: `N = numCores`
- I/O-bound: `N = numCores × (1 + wait_time / compute_time)` (Little's Law variant)

---

## 3. Distributed Task Scheduling Concepts

### Leader Election
- **Simple**: lowest-ID node becomes leader — not fault-tolerant
- **Raft/Paxos**: consensus-based — fault-tolerant but complex
- **ZooKeeper ephemeral nodes**: leader creates an ephemeral node; if leader dies, node disappears, others race to re-elect

### Work Stealing in Distributed Context
- Each node maintains a local queue
- Idle nodes poll the busiest node's queue and steal tasks
- Reduces coordination overhead vs a single global queue
- Challenge: need to know which node is "busiest" (gossip protocol or central broker)

### Task Idempotency
Essential for safe retry after node failure:
- **Idempotent operation**: executing N times = executing once
- Achieved via: idempotency keys in DB, at-least-once delivery + idempotent handlers
- Without idempotency: retried tasks cause duplicate side effects (double charges, duplicate emails)

### Health Monitoring
```java
// Heartbeat pattern:
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
monitor.scheduleAtFixedRate(() -> {
    for (Worker w : workers) {
        if (System.currentTimeMillis() - w.lastHeartbeat() > TIMEOUT_MS) {
            reassignTasks(w); // Worker is dead
        }
    }
}, 0, 5, SECONDS);
```

---

## 4. Parallel Web Crawling Patterns

See `ParallelWebCrawlerDemo.java` for full implementation.

**Key patterns:**
- `ConcurrentHashMap.newKeySet()` for thread-safe visited URL deduplication
- `CompletableFuture.allOf()` for fan-out with bounded depth
- `RateLimiter` for politeness (respect robots.txt rate limits)
- `CallerRunsPolicy` as natural back-pressure (submitter does the work when pool is saturated)
- Depth limiting: `Set<String> visited` + depth counter to prevent crawling the entire web

**Pitfall:** Unbounded recursion. Always cap depth and visited set size.

---

## 5. Context Switch Cost Reference

| Operation | Approximate Cost |
|---|---|
| Platform thread context switch (OS) | 1–10 µs |
| Virtual thread mount/unmount (JVM) | ~100 ns |
| `volatile` read/write | ~1–5 ns (memory fence) |
| CAS success (no contention) | ~5–10 ns |
| CAS failure (retry) | ~10–50 ns |
| Uncontended `synchronized` | ~10–30 ns (biased locking) |
| Contended `synchronized` | ~1–10 µs (OS mutex) |
| Thread creation (platform) | ~100 µs |
| Thread creation (virtual) | ~1 µs |

---

## 6. Interview Q&A

**Q: What is pinning in the context of virtual threads?**
A: A virtual thread is pinned when it is inside a `synchronized` block or native method. While pinned, it cannot unmount from its carrier platform thread — blocking the carrier for the duration. This negates virtual threads' main benefit for I/O. Fix: replace `synchronized` with `ReentrantLock`.

**Q: How does structured concurrency differ from `CompletableFuture.allOf()`?**
A: Structured concurrency (`StructuredTaskScope`) enforces a strict lifecycle: all forked tasks are guaranteed to complete (or be cancelled) before the scope closes. `CompletableFuture.allOf()` has no such guarantee — if you don't call `join()` and handle cancellation, tasks can leak and run after their results are discarded. Structured concurrency also propagates exceptions more cleanly.

**Q: When should you NOT use virtual threads?**
A: (1) CPU-bound tasks — no I/O means no unmounting benefit; virtual threads just add overhead. (2) Tasks that use `synchronized` heavily — pinning blocks carriers. (3) When you need thread-local state management with `ThreadLocal` across tasks — virtual threads have their own ThreadLocals, but if task-per-thread semantics matter, the model is fine. (4) When using libraries that do not support non-blocking I/O (still block a carrier thread).

**Q: How would you implement a distributed rate limiter?**
A: Use Redis with `INCR` and `EXPIRE`: atomically increment a key and set its TTL on first write. For sliding window: use a Redis sorted set with timestamps as scores; `ZREMRANGEBYSCORE` removes old entries, `ZCARD` counts current. For token bucket: use a Lua script (atomic read-modify-write in Redis) to implement the refill-and-consume logic. For extreme throughput: local token bucket with periodic sync to Redis.
