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

1. **`newFixedThreadPool(int nThreads)`**
   - Creates a thread pool with a fixed number of threads.
   - If a thread dies, a new one is created.
   - Uses an **unbounded queue** (`LinkedBlockingQueue`). If all threads are busy, tasks wait in the queue.

2. **`newCachedThreadPool()`**
   - Creates a pool that creates new threads as needed but will reuse previously constructed threads when they are available.
   - Threads that have been idle for 60 seconds are terminated and removed from the cache.
   - Good for short-lived, asynchronous tasks.
   - Uses a **`SynchronousQueue`** (size 0), which directly hands off tasks to threads.

3. **`newSingleThreadExecutor()`**
   - Creates an Executor that uses a single worker thread.
   - Guarantees that tasks are executed sequentially.
   - Uses an **unbounded queue**.

4. **`newScheduledThreadPool(int corePoolSize)`**
   - Creates a pool that can schedule commands to run after a given delay or to execute periodically.
   - Uses a **`DelayedWorkQueue`**.

5. **`newWorkStealingPool(int parallelism)`** *(Introduced in Java 8)*
   - Creates a thread pool that maintains enough threads to support the given parallelism level.
   - Uses multiple queues to reduce contention.
   - Backed by `ForkJoinPool`.

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
