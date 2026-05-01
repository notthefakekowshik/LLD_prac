# Striped Executor Pattern

**Intent:** Partition work by a key (stripe) such that all operations for the same key execute serially on a dedicated thread, while operations for different keys execute in parallel.

---

## Problem

You have high-throughput, stateful operations where:
- Operations on the **same key must be serialized** (to maintain consistency)
- Operations on **different keys can run in parallel** (to maximize throughput)
- Using locks (synchronized, ReentrantLock) creates contention and hurts performance

### Classic Examples
| Domain | Key (Stripe) | Why Serialize |
|--------|--------------|---------------|
| **Order Book** | Symbol (AAPL, TSLA) | Price-time priority matching requires FIFO per symbol |
| **Cache** | Key hash bucket | Eviction policy state per bucket |
| **Rate Limiter** | User ID / IP | Token bucket state per user |
| **Session Store** | Session ID | Session state updates must be atomic |
| **Game Server** | Room ID / Match ID | Game state per room |
| **Banking** | Account ID | Account balance updates must be serial |

---

## Solution

### Core Idea: Thread Confinement

Instead of locking, assign each key to a dedicated thread. All operations for that key run on that thread alone.

```
┌────────────────────────────────────────────────────────────────┐
│                     STRIPED EXECUTOR                            │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Key A ──┐                                                    │
│   Key A ──┤──► [Executor for Key A] ──► [Handler A]            │
│   Key A ──┘         (1 thread)          (stateful)            │
│                                                                 │
│   Key B ──┐                                                    │
│   Key B ──┤──► [Executor for Key B] ──► [Handler B]            │
│   Key B ──┘         (1 thread)          (stateful)            │
│                                                                 │
│   Key C ──┐                                                    │
│   Key C ──┼──► [Executor for Key C] ──► [Handler C]            │
│   Key C ──┘         (1 thread)          (stateful)            │
│                                                                 │
└────────────────────────────────────────────────────────────────┘

Result: No locks needed inside Handler A/B/C. Each is single-threaded.
```

### Structure

```java
/**
 * Generic Striped Executor Pattern.
 * 
 * @param <K> Key type (stripe identifier)
 * @param <T> Task type
 */
public class StripedExecutor<K, T> {
    
    // Map: Key → Dedicated Executor
    private final ConcurrentHashMap<K, ExecutorService> executors;
    
    // Map: Key → State Handler (optional)
    private final ConcurrentHashMap<K, Handler<T>> handlers;
    
    /**
     * Submit task for a specific key.
     * Task runs on the thread assigned to that key.
     */
    public Future<?> submit(K key, T task) {
        ExecutorService executor = executors.computeIfAbsent(
            key, 
            k -> createDedicatedExecutor(k)
        );
        return executor.submit(() -> process(task, handlers.get(key)));
    }
}
```

---

## Implementation Variants

### Variant 1: Unbounded (One Thread Per Key)

```java
private ExecutorService createDedicatedExecutor(K key) {
    return Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "stripe-" + key);
        t.setDaemon(true);
        return t;
    });
}
```

| Pros | Cons |
|------|------|
| Perfect isolation (no contention) | O(keys) threads — doesn't scale |
| Simple implementation | Memory overhead per thread (~1MB) |
| Ideal for small key count | Thread creation overhead |

**Use when:** Number of keys is bounded and small (< 1000).

---

### Variant 2: Bounded (Fixed Thread Pool with Hash Striping)

```java
public class BoundedStripedExecutor<K, T> {
    private final int STRIPE_COUNT = 32;  // Fixed
    private final ExecutorService[] executors;
    
    public BoundedStripedExecutor() {
        executors = new ExecutorService[STRIPE_COUNT];
        for (int i = 0; i < STRIPE_COUNT; i++) {
            executors[i] = Executors.newSingleThreadExecutor();
        }
    }
    
    public Future<?> submit(K key, T task) {
        int stripe = Math.abs(key.hashCode()) % STRIPE_COUNT;
        return executors[stripe].submit(() -> process(task, key));
    }
}
```

| Pros | Cons |
|------|------|
| O(1) threads regardless of keys | Key collision → potential contention |
| Memory efficient | Same stripe for different keys may queue up |
| Predictable resource usage | Slightly complex routing |

**Use when:** Large/unbounded key space, but can tolerate some collision.

---

### Variant 3: Hybrid (Fast Lane + Slow Lane)

Best of both worlds: Dedicated threads for hot keys, shared pool for cold keys.

```java
public class HybridStripedExecutor<K, T> {
    
    // Hot keys get dedicated threads
    private final Map<K, ExecutorService> fastLane;
    
    // Cold keys share bounded pool
    private final ExecutorService[] slowLane;
    private final int SLOW_LANE_SIZE = 32;
    
    public Future<?> submit(K key, T task) {
        ExecutorService executor = fastLane.get(key);
        if (executor != null) {
            return executor.submit(() -> process(task, key));
        }
        
        int stripe = Math.abs(key.hashCode()) % SLOW_LANE_SIZE;
        return slowLane[stripe].submit(() -> process(task, key));
    }
}
```

| Pros | Cons |
|------|------|
| Hot keys: Perfect isolation | More complex management |
| Cold keys: Memory efficient | Need to classify hot vs cold |
| Scales to many keys | Dynamic promotion adds complexity |

**Use when:** Workload follows power law (80/20 rule). Most systems fall here.

---

## The Hot Stripe Problem

### What is it?

When 80% of traffic hits 20% of keys, the "hot stripe" becomes the bottleneck.

```
Normal Distribution:          Hot Stripe (80/20):

AAPL: ████████ (10%)       AAPL: ████████████████████ (80%)
TSLA: ████████ (10%)       TSLA: ██ (5%)
MSFT: ████████ (10%)       MSFT: ██ (5%)
...  : ████████ (10%)       ...  : ████ (10%)

Problem: AAPL's single thread is saturated while others are idle.
```

### Symptoms
- One executor queue grows while others stay empty
- P99 latency spikes for hot key only
- Throughput capped at single-thread limit for that key
- Cannot scale horizontally (more cores don't help)

---

## Solutions to Hot Stripes

### Solution 1: Detection & Monitoring

```java
public class MonitoredStripedExecutor<K, T> {
    private final Map<K, AtomicInteger> queueDepth = new ConcurrentHashMap<>();
    private final Map<K, LongAdder> eventRate = new ConcurrentHashMap<>();
    
    public Future<?> submit(K key, T task) {
        queueDepth.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        eventRate.computeIfAbsent(key, k -> new LongAdder()).increment();
        
        return executorFor(key).submit(() -> {
            try {
                process(task, key);
            } finally {
                queueDepth.get(key).decrementAndGet();
            }
        });
    }
    
    public boolean isHotStripe(K key) {
        return queueDepth.get(key).get() > HOT_THRESHOLD;
    }
    
    public StripeStats getStats(K key) {
        return new StripeStats(
            key,
            queueDepth.get(key).get(),
            eventRate.get(key).sum(),
            isHotStripe(key)
        );
    }
}
```

**Why:** You can't fix what you can't measure.

---

### Solution 2: Backpressure

When a stripe is overloaded, apply backpressure:

```java
public enum BackpressureMode {
    REJECT,  // Throw exception (client retries)
    BLOCK,   // Wait until queue drains
    SHED     // Drop task (preserve availability)
}

public Future<?> submitWithBackpressure(K key, T task, BackpressureMode mode) {
    int depth = queueDepth.getOrDefault(key, new AtomicInteger(0)).get();
    
    if (depth > MAX_QUEUE_DEPTH) {
        switch (mode) {
            case REJECT: throw new RejectedExecutionException("Hot stripe: " + key);
            case BLOCK:  // Continue to submit (will wait)
            case SHED:   return CompletableFuture.completedFuture(null);
        }
    }
    return submit(key, task);
}
```

| Mode | Use Case |
|------|----------|
| REJECT | Client can retry with exponential backoff |
| BLOCK | Latency-sensitive, can't lose operations |
| SHED | Preserve core functionality, drop analytics/logging |

---

### Solution 3: Dedicated Fast Lane

Move hot keys to dedicated threads on isolated cores:

```java
public class HybridStripedExecutor<K, T> {
    private final Set<K> hotKeys;  // Configured or detected
    private final Map<K, ExecutorService> dedicatedExecutors;
    private final ExecutorService[] sharedPool;
    
    public Future<?> submit(K key, T task) {
        if (hotKeys.contains(key)) {
            // Fast lane: Dedicated thread
            return dedicatedExecutors
                .computeIfAbsent(key, k -> createDedicatedExecutor(k))
                .submit(() -> process(task, key));
        } else {
            // Slow lane: Shared pool
            int stripe = Math.abs(key.hashCode()) % sharedPool.length;
            return sharedPool[stripe].submit(() -> process(task, key));
        }
    }
}
```

**Why:** Isolates hot keys so they don't contend with cold keys.

---

### Solution 4: Dynamic Promotion/Demotion

Auto-detect and promote viral keys:

```java
public void maybePromote(K key) {
    long eps = eventRate.get(key).sum();
    int depth = queueDepth.get(key).get();
    
    if (eps > PROMOTION_THRESHOLD || depth > HOT_THRESHOLD) {
        promoteToFastLane(key);
    }
}

public void promoteToFastLane(K key) {
    if (fastLane.containsKey(key)) return;
    
    ExecutorService newExec = createDedicatedExecutor(key);
    fastLane.put(key, newExec);
    hotKeys.add(key);
    
    // Optional: Drain pending tasks from old executor
    migratePendingTasks(key, newExec);
}

public void demoteToSlowLane(K key) {
    ExecutorService removed = fastLane.remove(key);
    if (removed != null) {
        removed.shutdown();
        hotKeys.remove(key);
    }
}
```

**Why:** Adapts to changing workloads (e.g., meme stock going viral).

---

### Solution 5: Optimize Single-Threaded Path

When you can't shard further, optimize the hot path:

| Technique | Benefit |
|-----------|---------|
| Pre-allocate collections | Reduce GC pauses |
| Use primitive collections | Avoid boxing overhead |
| Batch operations | Amortize task submission overhead |
| Lock-free queues between stages | Decouple I/O from processing |
| Pin thread to core | Reduce context switches |

```java
// Batch processing example
public void processBatch(List<T> tasks) {
    List<Result> results = new ArrayList<>(tasks.size());
    for (T task : tasks) {
        results.add(processSingle(task));
    }
    // Single notification for batch
    listener.onBatchComplete(results);
}
```

---

## Decision Tree: Which Variant to Use?

```
Number of keys?
├── Bounded (< 1000)
│   └── Use Variant 1: Unbounded (one thread per key)
│       └── Simpler, perfect isolation
│
└── Unbounded or large
    └── Workload distribution?
        ├── Uniform (all keys equal)
        │   └── Use Variant 2: Bounded (hash striping)
        │       └── Good enough, predictable
        │
        └── Skewed (80/20, power law)
            └── Use Variant 3: Hybrid (fast + slow lanes)
                └── Best of both worlds
                    └── Add detection + backpressure
                    └── Add dynamic promotion for viral keys
```

---

## Case Study: Order Book Engine

### Problem
- Match orders for multiple stock symbols
- Each symbol: price-time priority (must be FIFO)
- Different symbols: can match in parallel
- 80% volume on top 20 NASDAQ stocks

### Solution Applied

**Phase 1: Basic Striped Executor**
```java
// One SingleThreadExecutor per symbol
symbol "AAPL" → Executor-1 → OrderBook(AAPL)
symbol "TSLA" → Executor-2 → OrderBook(TSLA)
```

**Phase 2: Hot Stripe Detected**
- AAPL queue depth = 5000
- Other symbols = 10
- AAPL thread saturated, P99 latency > 100ms

**Phase 3: Hybrid Solution**
```
Tier 1 (AAPL, MSFT, NVDA, TSLA, AMZN)
    └── Dedicated threads at startup

Tier 2 (GOOGL, META, NFLX, AMD, CRM)
    └── Dedicated threads at startup

Tier 3 (All others)
    └── Hash-routed to 32 shared stripes
```

**Phase 4: Dynamic Handling**
```java
// GME goes viral - promote on the fly
if (executor.isHotStripe("GME")) {
    executor.promoteToFastLane("GME");
}

// Apply backpressure during peak
executor.submitWithBackpressure("GME", order, BackpressureMode.REJECT);
```

### Results

| Metric | Before | After |
|--------|--------|-------|
| AAPL Throughput | ~10K ops/sec | ~200K ops/sec |
| AAPL P99 Latency | 150ms | 0.5ms |
| Cold Symbol Memory | 1 thread each | Shared 32 threads |
| Total Threads (10K symbols) | 10,000 | ~82 |

---

## Other Real-World Applications

### Cache with Per-Bucket Eviction
```java
// Stripe by key hash bucket
int bucket = key.hashCode() % BUCKET_COUNT;
stripedExecutor.submit(bucket, () -> {
    // Eviction logic for this bucket only
    cache.evictFromBucket(bucket);
});
```

### Rate Limiter (Token Bucket per User)
```java
// Stripe by user ID
stripedExecutor.submit(userId, () -> {
    // Token bucket is single-threaded, no locks
    if (bucket.consume(1)) {
        allowRequest();
    } else {
        rejectRequest();
    }
});
```

### Game Server (Per-Room State)
```java
// Stripe by room/match ID
stripedExecutor.submit(roomId, () -> {
    // Update game state - single threaded
    room.processMove(playerId, move);
    room.broadcastState();
});
```

### Banking (Account Transfers)
```java
// Stripe by account ID
stripedExecutor.submit(accountId, () -> {
    // Balance update - no locks needed
    account.debit(amount);
});

// For transfers (A → B): Use account ordering to prevent deadlock
K stripeKey = accountA.compareTo(accountB) < 0 ? accountA : accountB;
stripedExecutor.submit(stripeKey, () -> {
    // Both accounts on same stripe = serialized
    transfer(accountA, accountB, amount);
});
```

---

## Anti-Patterns

❌ **Don't use striped executor when:**
- Operations need strict global ordering across all keys
- Single operation touches multiple keys (hard to pick stripe)
- Work is CPU-bound and embarrassingly parallel (just use ForkJoinPool)
- Keys are truly uniform (overhead not worth it)

❌ **Don't create too many stripes:**
- 10K threads = memory pressure + scheduler overhead
- Prefer bounded hybrid approach

❌ **Don't ignore hot stripes:**
- Single hot key can kill your P99 latency
- Always add monitoring

---

## Checklist: Implementing Striped Executor

- [ ] Identify the stripe key (what defines "same" operations?)
- [ ] Determine if workload is uniform or skewed
- [ ] Choose variant: Unbounded / Bounded / Hybrid
- [ ] Implement key → executor routing
- [ ] Add monitoring (queue depth, event rate per stripe)
- [ ] Add backpressure (REJECT / BLOCK / SHED)
- [ ] Add hot stripe detection
- [ ] (Optional) Add dynamic promotion for viral keys
- [ ] Benchmark with realistic load distribution

---

## References

- **Order Book Implementation:** `systems/orderbook/service/HybridStripedExecutor.java`
- **Order Book Demo:** `systems/orderbook/demo/HybridStripedExecutorDemo.java`
- **Hot Stripe Pattern:** `systems/orderbook/HOT_STRIPE_PATTERN.md`
- **Design Document:** `systems/orderbook/DESIGN_DICE.md` (Thread Confinement section)
