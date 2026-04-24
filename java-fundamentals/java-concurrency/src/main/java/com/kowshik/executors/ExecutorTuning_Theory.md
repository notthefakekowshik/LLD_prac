# Executor Tuning — How to Configure ThreadPoolExecutor Safely

> **Prerequisites:** Read `Executors_Theory.md` first for the basics of `ThreadPoolExecutor` parameters and queue types.

---

## 1. Why OOTB Executors Are Production-Unsafe

Every factory method in `Executors` hides a dangerous default. The problem is not the thread pool itself — it's the **unbounded resource** lurking inside.

| Factory Method | Internal Queue / Thread Creation | OOM Vector |
|---|---|---|
| `newFixedThreadPool(n)` | `LinkedBlockingQueue(Integer.MAX_VALUE)` | Queue grows to ~2B tasks → heap exhaustion |
| `newSingleThreadExecutor()` | `LinkedBlockingQueue(Integer.MAX_VALUE)` | Same as above |
| `newCachedThreadPool()` | `SynchronousQueue` + **unbounded thread creation** | Creates one OS thread per task → native memory exhaustion |
| `newScheduledThreadPool(n)` | `DelayedWorkQueue` (unbounded) | Same as `FixedThreadPool` but for scheduled tasks |

### The Failure Mode

```
Producer submits tasks at 10,000 req/s
Pool processes tasks at 5,000 req/s
↓
Queue grows at 5,000 tasks/s
↓
After ~10 minutes → queue has 3,000,000 tasks in heap
↓
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

The JVM does not give you a warning. The queue silently absorbs everything until it can't.

**Rule:** In production, **never use OOTB factory methods** unless you have explicitly verified the workload is bounded. Always construct `ThreadPoolExecutor` directly with a bounded `ArrayBlockingQueue`.

---

## 2. Characterize Your Task First

Before picking any number, you must classify your workload. Every tuning decision flows from this.

### CPU-Bound Tasks
- Spend most of their time doing computation (parsing, hashing, sorting, ML inference).
- More threads than CPU cores = context-switching overhead with no throughput gain.
- Example: image processing, JSON serialization, encryption.

### IO-Bound Tasks
- Spend most of their time **waiting** for an external resource: DB query, HTTP call, disk read.
- The thread is parked/sleeping during the wait — the CPU is free.
- More threads beyond cores is beneficial here because idle threads don't waste CPU.
- Example: REST calls, JDBC queries, file reads.

### Mixed Tasks
- Have both a compute phase and an IO phase.
- Use the formula below with a measured Wait/Compute ratio.

---

## 3. Sizing `corePoolSize`

### Formula (Brian Goetz, *Java Concurrency in Practice*)

```
N_threads = N_cpu × U_cpu × (1 + W/C)
```

| Variable | Meaning |
|---|---|
| `N_cpu` | `Runtime.getRuntime().availableProcessors()` |
| `U_cpu` | Target CPU utilization (0.0–1.0). Usually 0.7–0.9 in mixed systems |
| `W/C` | Wait time / Compute time ratio of a single task |

### Examples

**CPU-bound** (W/C ≈ 0, U_cpu = 1.0, 8 cores):
```
N = 8 × 1.0 × (1 + 0) = 8 threads
```
Add 1 extra as a buffer for GC pauses → **corePoolSize = 9**

**IO-bound** (DB query: W = 50ms wait, C = 5ms compute, W/C = 10, 8 cores, U_cpu = 0.8):
```
N = 8 × 0.8 × (1 + 10) = 70.4 → ~70 threads
```

**Practical shortcut if you can't measure W/C:**
- CPU-bound: `N_cpu + 1`
- IO-bound: `N_cpu * 2` to `N_cpu * 10` (start at 2x, load-test upward)

### What `corePoolSize` actually means at runtime

- Threads are **not pre-created** — they're created lazily as tasks arrive.
- Once created, core threads **never die** (unless `allowCoreThreadTimeOut(true)` is set).
- Core threads sit idle between tasks. This is the "warm standby" you pay for.
- Call `prestartAllCoreThreads()` if you want them ready before the first request.

---

## 4. Sizing `maximumPoolSize`

`maximumPoolSize` only matters when the queue is **full**. With an unbounded queue, `maximumPoolSize` is effectively ignored (tasks just keep queuing forever).

### When `maximumPoolSize > corePoolSize` is useful

Use a max pool larger than core when:
- You have burst traffic that is short-lived.
- Your tasks are IO-bound (threads are cheap when they're sleeping).
- You prefer spawning extra threads over dropping requests.

### Memory constraint — the hard ceiling

Each thread has a stack. Default stack size on JVM is **512 KB–1 MB** depending on the platform.

```
Max safe threads ≈ (Available Native Memory) / (Thread Stack Size)

Example: 2 GB native heap headroom / 512 KB per thread ≈ 4,000 threads
```

Set `-Xss256k` (JVM flag) to reduce stack size if you need a very high thread count.

**Rule of thumb:**
- `maximumPoolSize` should never be more than **2–5×** `corePoolSize` unless you have a specific burst pattern with measured data.
- For CPU-bound work: `maximumPoolSize == corePoolSize` (no benefit in extra threads).
- For IO-bound work: `maximumPoolSize` can be up to the memory ceiling.

### The queue-size / max-pool-size tradeoff

```
Small queue + Large max pool  →  more threads, lower latency under burst, higher memory
Large queue + Small max pool  →  fewer threads, lower memory, more queuing latency
```

---

## 5. Sizing the `workQueue` (Bounded Queue Capacity)

### Use `ArrayBlockingQueue` in production. Never `LinkedBlockingQueue` without a capacity.

The queue capacity determines how many tasks can **wait** before either more threads are spun up or the rejection policy fires.

### Sizing with Little's Law

```
L = λ × W
```

| Variable | Meaning |
|---|---|
| `L` | Queue capacity you need (tasks in system at one time) |
| `λ` | Task arrival rate (tasks/second) |
| `W` | Acceptable waiting time in the queue (seconds) |

**Example:** 500 req/s arrival rate, max tolerable queue wait = 200ms:
```
L = 500 × 0.2 = 100 tasks
```
→ `new ArrayBlockingQueue<>(100)` is a reasonable bound.

### Why not just set it to a large number?

A queue of 100,000 with slow consumers means tasks wait for minutes. A 200ms SLA becomes a 10-minute nightmare. **The queue size implicitly defines your worst-case latency.**

---

## 6. `keepAliveTime` — When Non-Core Threads Die

Non-core threads (those above `corePoolSize`) are cleaned up after being idle for `keepAliveTime`.

### Guidance

| Workload Pattern | `keepAliveTime` |
|---|---|
| Steady, predictable traffic | Short (30–60 seconds). Non-core threads are rare and should be reclaimed fast. |
| Bursty traffic (e.g., batch jobs at midnight) | Longer (5–10 minutes). Avoid thrashing thread creation/destruction during bursts. |
| Pure CPU-bound with `max == core` | Irrelevant — there are no non-core threads. |

### Applying keepAlive to core threads too

By default, core threads live forever. You can change this:

```java
executor.allowCoreThreadTimeOut(true);
executor.setKeepAliveTime(60, TimeUnit.SECONDS);
```

Useful if your service handles intermittent traffic and you want to reduce idle resource usage between bursts (e.g., a batch job that runs hourly).

---

## 7. Choosing a `RejectedExecutionHandler`

Rejection fires when: **queue is full AND active threads == maximumPoolSize**.

This is a **system-health signal**, not just a task-failure event. Choose your handler based on what rejection means for your application.

### Decision Matrix

| Policy | Behavior | Use When |
|---|---|---|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` | Caller can handle it (retry, circuit-break, return error). Surfaces overload explicitly. |
| `CallerRunsPolicy` | Caller thread runs the task itself | You want **backpressure** — slows the producer naturally. Good for internal pipelines. |
| `DiscardPolicy` | Task is silently dropped | Task is truly ephemeral (e.g., sampled metrics, low-priority heartbeats). Never for business-critical work. |
| `DiscardOldestPolicy` | Drops the oldest queued task, retries current | Tasks are time-sensitive; newer tasks are more valuable (e.g., market data ticks). |
| **Custom** | Full control | Log + alert + send to dead-letter queue + emit metric. Always preferred in production. |

### Production-grade custom handler

```java
public class ObservableRejectionHandler implements RejectedExecutionHandler {
    private static final Logger log = LoggerFactory.getLogger(ObservableRejectionHandler.class);

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 1. Emit a metric (Prometheus counter, Micrometer, etc.)
        Metrics.counter("executor.rejection").increment();

        // 2. Log with context
        log.error("Task rejected — poolSize={}, activeThreads={}, queueSize={}",
            executor.getPoolSize(),
            executor.getActiveCount(),
            executor.getQueue().size());

        // 3. Business decision: CallerRuns as fallback, or push to a dead-letter queue
        if (!executor.isShutdown()) {
            r.run(); // CallerRunsPolicy fallback
        }
    }
}
```

---

## 8. The Full Safe Configuration Template

```java
int cpus = Runtime.getRuntime().availableProcessors();

// IO-bound example: W/C ratio ≈ 5, target 80% CPU utilization
int coreThreads  = (int) (cpus * 0.8 * (1 + 5));   // ~39 for 8 cores
int maxThreads   = coreThreads * 2;                  // burst headroom
int queueCap     = 500;                              // tune with Little's Law
long keepAlive   = 60L;

ThreadPoolExecutor executor = new ThreadPoolExecutor(
    coreThreads,
    maxThreads,
    keepAlive,
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(queueCap),       // BOUNDED — never LinkedBlockingQueue raw
    new ThreadFactory() {                     // named threads for easier debugging
        private final AtomicInteger n = new AtomicInteger(1);
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "svc-worker-" + n.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    },
    new ObservableRejectionHandler()          // custom, with metrics
);
```

---

## 9. Monitoring — How to Know If Your Tuning Is Working

`ThreadPoolExecutor` exposes live stats. Poll these periodically (every 30s) via JMX, Micrometer, or a scheduled logger.

```java
// Key signals
executor.getActiveCount()      // threads currently executing a task
executor.getPoolSize()         // current total threads (core + non-core)
executor.getQueue().size()     // tasks waiting in queue
executor.getCompletedTaskCount()
executor.getLargestPoolSize()  // historical peak thread count
```

### What the numbers tell you

| Signal | Healthy | Problem |
|---|---|---|
| `queue.size()` | Near 0 most of the time | Consistently > 50% of capacity → under-provisioned |
| `activeCount` | Fluctuates below `corePoolSize` | Pinned at `maximumPoolSize` → saturated, expect rejections |
| `largestPoolSize` | Near `corePoolSize` | Near `maximumPoolSize` → bursts are hitting the ceiling |
| Rejection rate | 0 | > 0 → queue+threads are exhausted; scale up or shed load |

### Tuning feedback loop

```
1. Deploy with conservative settings (small core, bounded queue, AbortPolicy)
2. Observe queue.size() and rejection rate under real load
3. If queue fills → increase corePoolSize or maxPoolSize
4. If memory grows → reduce maxPoolSize or increase -Xss conservatively
5. Repeat
```

---

## 10. Common Mistakes Summary

| Mistake | Why It's Dangerous | Fix |
|---|---|---|
| Using `Executors.newFixedThreadPool()` in production | Unbounded `LinkedBlockingQueue` → OOM | Use `ThreadPoolExecutor` with `ArrayBlockingQueue` |
| Using `Executors.newCachedThreadPool()` under load | Unbounded thread creation → native OOM | Set a finite `maximumPoolSize` with `SynchronousQueue` |
| Setting `maxPoolSize` very high for CPU-bound tasks | Context switching kills throughput | `max == core == N_cpu + 1` for CPU-bound |
| Ignoring rejection | `AbortPolicy` silently crashes callers | Always add a custom handler with alerting |
| Setting huge queue to avoid rejection | Unbounded queue wait = broken SLAs | Size queue with Little's Law |
| No named threads | Impossible to debug thread dumps | Always use a custom `ThreadFactory` with meaningful names |
| Never calling `shutdown()` | JVM won't exit; threads leak | Always shutdown in a `try-finally` or via a lifecycle hook |
