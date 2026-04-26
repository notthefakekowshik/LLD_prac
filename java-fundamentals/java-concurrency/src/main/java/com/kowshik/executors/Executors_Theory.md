# Java Executors and Thread Pools Theory

The Java Concurrency framework provides the `Executor` and `ExecutorService` interfaces along with the `Executors` utility class to decouple task submission from the mechanics of how each task will be run, including details of thread use, scheduling, etc.

## 1. Why use Thread Pools?

Creating and destroying threads dynamically is expensive and resource-intensive. A thread pool manages a pool of worker threads. It contains a queue that keeps tasks waiting to be executed.

- **Resource Management:** Prevents the system from crashing due to an OutOfMemoryError when too many threads are created.
- **Performance:** Reuses existing threads instead of creating new ones, which eliminates the overhead of thread creation.
- **Task Management:** Provides mechanisms to queue tasks, limit concurrent execution, and gracefully shut down.

---

## 2. Out-of-the-Box (OOTB) Thread Pools

The `java.util.concurrent.Executors` class provides factory methods to create several standard thread pool configurations:

### 1. `newFixedThreadPool(int nThreads)`
- Fixed number of threads — never grows, never shrinks.
- If a thread dies unexpectedly, a replacement is created.
- Queue: **unbounded `LinkedBlockingQueue`** — tasks wait forever if all threads are busy.
- `maximumPoolSize` == `corePoolSize`, so extra threads are never created.
- **Production warning:** unbounded queue → OOM if producers outpace consumers. Always prefer `ThreadPoolExecutor` with a bounded `ArrayBlockingQueue` in production.
- **Best for:** known, bounded workloads — CPU-bound batch jobs, server request handlers with known max concurrency.

```java
ExecutorService pool = Executors.newFixedThreadPool(4);
```

---

### 2. `newCachedThreadPool()`
- Grows unboundedly — creates a new thread for every task that has no idle thread available.
- Idle threads are reaped after **60 seconds**.
- Queue: **`SynchronousQueue`** (zero capacity) — every submitted task either finds a waiting thread instantly or a new thread is created.
- `corePoolSize` = 0, `maximumPoolSize` = `Integer.MAX_VALUE`.
- **Production warning:** under high load, can create thousands of threads → native memory exhaustion (`OutOfMemoryError: unable to create native thread`).
- **Best for:** many short-lived, burst tasks where thread count naturally self-limits (e.g., lightweight I/O callbacks).

```java
ExecutorService pool = Executors.newCachedThreadPool();
```

---

### 3. `newSingleThreadExecutor()`
- Single worker thread — all tasks run sequentially in submission order.
- If the thread dies, a replacement is created automatically (unlike a `newFixedThreadPool(1)` which can be reconfigured via cast).
- Queue: **unbounded `LinkedBlockingQueue`**.
- The returned `ExecutorService` is wrapped in a finalization-safe proxy — you cannot cast it to `ThreadPoolExecutor` to change its pool size.
- **Best for:** serializing access to a shared resource (e.g., single-threaded log writer, ordered event processor).

```java
ExecutorService pool = Executors.newSingleThreadExecutor();
```

---

### 4. `newScheduledThreadPool(int corePoolSize)`
- Runs tasks after a delay or at a fixed rate/fixed delay.
- Queue: **`DelayedWorkQueue`** (a specialized min-heap by next execution time — same concept as `DelayQueue`).
- Key scheduling methods:

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// One-shot: run once after 500ms
scheduler.schedule(task, 500, TimeUnit.MILLISECONDS);

// Fixed rate: run every 1s regardless of task duration
// If task takes 3s, next run is LATE but not skipped
scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);

// Fixed delay: wait 1s AFTER task completes, then run again
// If task takes 3s, next run starts at 3s + 1s = 4s
scheduler.scheduleWithFixedDelay(task, 0, 1, TimeUnit.SECONDS);
```

**`scheduleAtFixedRate` vs `scheduleWithFixedDelay`:**

| | `scheduleAtFixedRate` | `scheduleWithFixedDelay` |
|---|---|---|
| Period measured from | Start of previous execution | End of previous execution |
| Slow task (takes > period) | Next run is delayed, but not skipped | Always waits full delay after completion |
| Use when | Tasks should run on a wall-clock schedule | Tasks should have a rest gap between runs |
| Example | Heartbeat every 10s | Retry polling (wait 5s after each attempt) |

**Note:** If a task throws an uncaught exception, subsequent executions are **silently suppressed**. Always wrap task body in try-catch.

---

### 5. `newWorkStealingPool(int parallelism)` *(Java 8)*
- Backed by a **`ForkJoinPool`** with the given parallelism level (defaults to `Runtime.getRuntime().availableProcessors()` if no arg).
- Each worker thread has its **own deque** of tasks. When a thread finishes its own work, it **steals tasks from the tail** of another thread's deque — minimizes contention on a shared queue.
- Threads are **daemon threads** — the JVM can exit without an explicit `shutdown()` call (unlike all other OOTB pools).
- Task execution order is **NOT guaranteed** — tasks may run in any order.
- Optimal for **divide-and-conquer** workloads (recursive tasks that fork into sub-tasks).
- **Not suitable** for tasks that need sequential execution or hold locks (stealing can cause priority inversion).

```java
// parallelism defaults to availableProcessors()
ExecutorService pool = Executors.newWorkStealingPool();

// explicit parallelism
ExecutorService pool = Executors.newWorkStealingPool(8);
```

Work-stealing algorithm:
```
Thread-1 deque: [T1, T2, T3]   ← T1 runs from head
Thread-2 deque: []              ← idle → steals T3 from TAIL of Thread-1
Thread-3 deque: [T4]            ← runs T4
```

---

### 6. `newVirtualThreadPerTaskExecutor()` *(Java 21)*
- Creates a new **virtual thread** for every submitted task — no pooling needed (virtual threads are cheap to create).
- `corePoolSize` and `maximumPoolSize` concepts don't apply — there is no pool.
- Backed by a `ForkJoinPool` carrier thread pool (managed by the JVM).
- Virtual threads block cheaply — no carrier thread is pinned when the virtual thread is parked (except inside `synchronized` blocks — use `ReentrantLock` instead).
- **Best for:** high-concurrency I/O-bound workloads (HTTP servers, DB queries, file reads) with thousands of concurrent tasks.
- **Not for:** CPU-bound tasks (virtual threads don't add parallelism beyond the number of CPU cores).

```java
// Java 21+
ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

// Every submit() creates a fresh virtual thread — no queueing
pool.submit(() -> {
    // blocking I/O here is fine — virtual thread parks, carrier is freed
    String result = httpClient.get("https://api.example.com/data");
    process(result);
});
```

**OOTB Pools — Quick Comparison:**

| Factory Method | Threads | Queue | Max Threads | Daemon? | Best For |
|---|---|---|---|---|---|
| `newFixedThreadPool(n)` | Fixed | Unbounded LBQ | = core | No | Known-size CPU/IO workloads |
| `newCachedThreadPool()` | Elastic | SynchronousQueue | ∞ | No | Bursty short-lived tasks |
| `newSingleThreadExecutor()` | 1 | Unbounded LBQ | 1 | No | Ordered sequential execution |
| `newScheduledThreadPool(n)` | Fixed core | DelayedWorkQueue | ∞ | No | Delayed/periodic tasks |
| `newWorkStealingPool(p)` | ≈ parallelism | Per-thread deques | dynamic | **Yes** | Fork-join, divide-and-conquer |
| `newVirtualThreadPerTaskExecutor()` | 1 per task | None | ∞ (virtual) | Yes | High-concurrency I/O (Java 21) |

---

## 3. Creating Custom Thread Pools (`ThreadPoolExecutor`)

If the OOTB pools do not meet your requirements, you can construct a `ThreadPoolExecutor` manually. This gives you fine-grained control over the pool's behavior.

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    corePoolSize,
    maximumPoolSize,
    keepAliveTime,
    TimeUnit,
    workQueue,
    threadFactory,
    rejectedExecutionHandler
);
```

### Parameters explained

1. **`corePoolSize`**: The number of threads to keep in the pool, even if they are idle (unless `allowCoreThreadTimeOut` is set).
2. **`maximumPoolSize`**: The maximum number of threads allowed in the pool.
3. **`keepAliveTime` + `TimeUnit`**: When the number of threads is greater than the core pool size, this is the maximum time that excess idle threads will wait for new tasks before terminating.
4. **`workQueue`**: The queue to use for holding tasks before they are executed.
5. **`threadFactory`**: The factory to use when the executor creates a new thread (useful for setting custom thread names, priority, or daemon status).
6. **`handler`**: The strategy to use when execution is blocked because the thread bounds and queue capacities are reached.

### ⚠️ Thread Creation Rules (Core vs Max)

It is a common misconception that an `Executor` will immediately spin up new threads up to `maximumPoolSize` if all core threads are currently busy. This is **incorrect**.

**Lazy Initialization:** The `ThreadPoolExecutor` uses a strategy of lazy initialization. It only spins up a thread when it actually needs to do work (i.e., when tasks are submitted).

A `ThreadPoolExecutor` follows a strict set of rules when a new task is submitted:

1. If fewer than `corePoolSize` threads are running, it creates a new thread to run the task.
2. If `corePoolSize` or more threads are running, it tries to queue the task into the `workQueue`.
3. **If the queue is full (and only if the queue is full),** it will attempt to create a new thread up to `maximumPoolSize`.
4. If `maximumPoolSize` has been reached and the queue is full, the task is rejected.

#### Example Scenario

Suppose we configure a thread pool with:

- `corePoolSize` = 5
- `maximumPoolSize` = 10
- `workQueue` = `new ArrayBlockingQueue<>(50)`

What happens as we submit 61 tasks simultaneously?

1. **Tasks 1-5:** The pool creates 5 new threads (Core pool is now full).
2. **Tasks 6-55:** The pool does **not** create thread #6. Instead, it places all these 50 tasks into the `ArrayBlockingQueue`. The queue is now full, and we still only have 5 threads running.
3. **Tasks 56-60:** Now the queue is full. The pool will spin up new threads (up to the max of 10) to handle these incoming tasks. So threads 6, 7, 8, 9, and 10 are created.
4. **Task 61:** The core pool is full, the queue is full, and the max pool size is reached. The rejection policy is triggered.

---

## 4. Types of Queues for Thread Pools

The `workQueue` parameter accepts implementations of `BlockingQueue<Runnable>`. The choice of queue heavily impacts the behavior of the pool:

1. **`LinkedBlockingQueue`**
   - Typically used as an **unbounded** queue (capacity `Integer.MAX_VALUE`).
   - Used by `newFixedThreadPool` and `newSingleThreadExecutor`.
   - **Behavior:** New tasks will queue up indefinitely if all core threads are busy. As a result, the pool will never grow past `corePoolSize` (so `maximumPoolSize` is ignored).

2. **`SynchronousQueue`**
   - A queue with a capacity of **zero**. It does not hold elements; it simply hands them off between threads.
   - Used by `newCachedThreadPool`.
   - **Behavior:** If a thread is not available to immediately take the task, it will try to create a new thread (up to `maximumPoolSize`). If the max is reached, the task is rejected.

3. **`ArrayBlockingQueue`**
   - A **bounded** queue backed by an array. You must provide a fixed capacity.
   - **Behavior:** Prevents resource exhaustion. When core threads are busy, tasks fill the queue. When the queue is full, the pool creates new threads until `maximumPoolSize` is reached. If the maximum is reached and the queue is full, the task is rejected.

4. **`PriorityBlockingQueue`**
   - An **unbounded** concurrent queue. It uses the same ordering rules as class `PriorityQueue` (elements must implement `Comparable` or a `Comparator` must be provided).

5. **`DelayQueue`**
   - Used by ScheduledThreadPools. Tasks are pulled from the queue only when their delay expires.

---

## 5. Rejection Policies (`RejectedExecutionHandler`)

When you submit a task to an `ExecutorService` but it cannot be accepted (e.g., because the pool is shut down, or the queue is full and threads are at maximum), a rejection policy kicks in. Java provides 4 standard policies:

1. **`ThreadPoolExecutor.AbortPolicy` (Default)**
   - Throws a runtime `RejectedExecutionException`.

2. **`ThreadPoolExecutor.CallerRunsPolicy`**
   - Executes the task directly in the calling thread (the thread that called `execute()` or `submit()`).
   - This provides a simple feedback control mechanism that slows down the rate at which new tasks are submitted.

3. **`ThreadPoolExecutor.DiscardPolicy`**
   - Silently drops the task. No exception is thrown.

4. **`ThreadPoolExecutor.DiscardOldestPolicy`**
   - Discards the oldest unhandled request in the queue and tries to submit the new task again.

---

## 6. Shutting down Thread Pools

Thread pools do not shut down automatically. You must shut them down gracefully to allow the JVM to exit.

- **`shutdown()`**: Orderly shutdown. The executor stops accepting new tasks but will finish executing tasks that are already in the queue or currently running.
- **`shutdownNow()`**: Hard shutdown. Attempts to stop all actively executing tasks and halts the processing of waiting tasks. Returns a list of tasks that were waiting to be executed.
- **`awaitTermination(long timeout, TimeUnit unit)`**: Blocks the current thread until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is interrupted.

### Best Practice Shutdown Sequence

```java
pool.shutdown(); // Disable new tasks from being submitted
try {
    // Wait a while for existing tasks to terminate
    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
            System.err.println("Pool did not terminate");
    }
} catch (InterruptedException ie) {
    // (Re-)Cancel if current thread also interrupted
    pool.shutdownNow();
    // Preserve interrupt status
    Thread.currentThread().interrupt();
}
```

---

## 7. Interview Q&A

**Q: Why is `newFixedThreadPool` dangerous in production?**
A: It uses an unbounded `LinkedBlockingQueue`. If tasks are submitted faster than they are processed, the queue grows without limit until `OutOfMemoryError`. Always use a custom `ThreadPoolExecutor` with a bounded `ArrayBlockingQueue` and a `RejectedExecutionHandler` in production.

**Q: What is the difference between `newFixedThreadPool(1)` and `newSingleThreadExecutor()`?**
A: Both use one thread and an unbounded queue. The difference: `newSingleThreadExecutor()` wraps the pool in a proxy that prevents callers from casting it to `ThreadPoolExecutor` and reconfiguring it (e.g., calling `setCorePoolSize(4)`). The single-thread guarantee is enforced. With `newFixedThreadPool(1)` you can cast and accidentally resize the pool.

**Q: Why can `newCachedThreadPool` cause OOM?**
A: Its `maximumPoolSize` is `Integer.MAX_VALUE` and it uses `SynchronousQueue` (zero buffer). Every submitted task that finds no idle thread immediately spawns a new OS thread. Under a spike of thousands of concurrent submissions, thousands of threads are created — each consuming native stack memory (~512KB default) — leading to `OutOfMemoryError: unable to create native thread`.

**Q: What is the difference between `scheduleAtFixedRate` and `scheduleWithFixedDelay`?**
A: `scheduleAtFixedRate` measures the period from the *start* of the previous execution — the next run is scheduled at `start + period`. `scheduleWithFixedDelay` measures the delay from the *end* of the previous execution — the next run is scheduled at `end + delay`. If the task takes longer than the period, `scheduleAtFixedRate` will run the next iteration immediately after completion (it catches up), while `scheduleWithFixedDelay` always respects the gap after completion.

**Q: What happens if a task throws an exception in `scheduleAtFixedRate`?**
A: The recurring execution is **silently cancelled** — no future runs happen and no exception is propagated. The `ScheduledFuture` returned will rethrow the exception when `get()` is called. Always wrap the task body in try-catch to prevent silent suppression.

**Q: Why are `newWorkStealingPool` threads daemon threads?**
A: Work-stealing pools are designed for parallel computation that runs as part of the program's main work. Since they're backed by `ForkJoinPool`, the JVM treats them as background computational helpers — the application shouldn't have to track their lifecycle. Non-daemon threads would prevent JVM exit even if the main program is done.

**Q: When would you choose `newVirtualThreadPerTaskExecutor` over `newFixedThreadPool`?**
A: For I/O-bound workloads with many concurrent tasks. Virtual threads are extremely cheap (stack starts at ~few hundred bytes, not 512KB). A `newFixedThreadPool(200)` limits concurrency to 200; `newVirtualThreadPerTaskExecutor()` can handle 100,000+ concurrent tasks without exhausting memory, because each virtual thread parks cheaply while waiting for I/O instead of tying up an OS thread.

**Q: Can you use `newVirtualThreadPerTaskExecutor` for CPU-intensive tasks?**
A: Not beneficially. Virtual threads run on top of a `ForkJoinPool` of platform threads sized to the number of CPU cores. CPU-bound tasks keep the carrier threads busy the entire time — you get no concurrency advantage over a fixed thread pool of `N_CPU` threads. Worse, if you create millions of virtual threads all doing CPU work, the scheduler overhead degrades throughput.

---

## 8. Demo Files Reference

| File | Key scenarios demonstrated |
|---|---|
| `FixedThreadPoolDemo.java` | Queue backlog proof; thread reuse across batches; bounded production-safe variant with rejection handler |
| `CachedThreadPoolDemo.java` | Elasticity (grow on burst, idle after); thread reuse between waves; OOM risk illustration; bounded safe alternative; live thread-count drain with monitor |
| `SingleThreadExecutorDemo.java` | Sequential ordering guarantee; thread resurrection after worker crash; proxy cast protection vs `newFixedThreadPool(1)`; serialized log writer without synchronization |
| `ScheduledThreadPoolDemo.java` | One-shot delayed task; `scheduleAtFixedRate`; `scheduleWithFixedDelay`; slow-task comparison (rate vs delay) side-by-side; silent exception suppression + try-catch fix |
| `WorkStealingPoolDemo.java` | Throughput vs fixed pool on CPU-bound tasks; divide-and-conquer parallel array sum; daemon thread proof; uneven task distribution with work stealing |
| `VirtualThreadExecutorDemo.java` | 10,000 tasks in ~100ms; I/O throughput vs fixed pool; `isVirtual`/`isDaemon` properties; carrier pinning with `synchronized` vs `ReentrantLock` |
