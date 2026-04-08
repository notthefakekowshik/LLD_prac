# Custom Thread Pool Implementation

Production-grade thread pool executor built from scratch without using `java.util.concurrent.ExecutorService`.

## Features

### Core Functionality
- ✅ Fixed-size thread pool with configurable worker threads
- ✅ Bounded task queue with configurable capacity
- ✅ Runnable and Callable task support
- ✅ Future-based result retrieval
- ✅ Graceful and immediate shutdown
- ✅ Thread-safe operations

### Design Patterns
- **Strategy Pattern** - Pluggable rejection policies
- **Factory Pattern** - Customizable thread creation
- **Builder Pattern** - Fluent API for configuration
- **State Pattern** - Lifecycle management (RUNNING → SHUTDOWN → TERMINATED)
- **Observer Pattern** - Future-based task completion

### Rejection Policies
When the queue is full, different strategies handle task rejection:

| Policy | Behavior | Use Case |
|--------|----------|----------|
| `AbortPolicy` | Throws `RejectedExecutionException` | Fail-fast, caller handles rejection |
| `CallerRunsPolicy` | Runs task in caller's thread | Natural backpressure |
| `DiscardPolicy` | Silently drops task | Non-critical tasks |
| `DiscardOldestPolicy` | Drops oldest task, retries new one | Newer data is more valuable |

### Metrics & Monitoring
- Active thread count
- Completed task count
- Queue size
- Pool state (RUNNING, SHUTDOWN, TERMINATED)

## Quick Start

### Basic Usage

```java
CustomThreadPool pool = new FixedThreadPool.Builder()
    .poolSize(5)
    .queueCapacity(100)
    .build();

// Submit tasks
pool.submit(() -> {
    System.out.println("Task executing in " + Thread.currentThread().getName());
});

// Graceful shutdown
pool.shutdown();
pool.awaitTermination(10, TimeUnit.SECONDS);
```

### With Custom Configuration

```java
CustomThreadPool pool = new FixedThreadPool.Builder()
    .poolSize(10)
    .queueCapacity(50)
    .rejectionPolicy(new CallerRunsPolicy())
    .threadFactory(new DefaultThreadFactory("MyPool", true))
    .build();
```

### Callable with Future

```java
CustomFuture<Integer> future = pool.submit(() -> {
    return 42;
});

try {
    Integer result = future.get(5, TimeUnit.SECONDS);
    System.out.println("Result: " + result);
} catch (TimeoutException e) {
    future.cancel(true);
}
```

## Architecture

```
CustomThreadPool (interface)
    ↑
    |
FixedThreadPool (implementation)
    |
    ├── WorkerThread[] (composition)
    ├── BlockingQueue<Runnable> (composition)
    ├── RejectionPolicy (strategy)
    └── ThreadFactory (factory)
```

## Package Structure

```
com.lldprep.threadpool/
├── DESIGN.md                       # Detailed design document
├── README.md                       # This file
├── CustomThreadPool.java           # Main interface
├── FixedThreadPool.java            # Implementation with Builder
├── WorkerThread.java               # Worker thread logic
├── PoolState.java                  # Lifecycle states enum
├── ThreadPoolMetrics.java          # Metrics snapshot
├── policy/
│   ├── RejectionPolicy.java        # Strategy interface
│   ├── AbortPolicy.java
│   ├── CallerRunsPolicy.java
│   ├── DiscardPolicy.java
│   └── DiscardOldestPolicy.java
├── factory/
│   ├── ThreadFactory.java          # Factory interface
│   └── DefaultThreadFactory.java
├── future/
│   ├── CustomFuture.java           # Future interface
│   └── FutureTask.java             # Future implementation
├── exception/
│   └── RejectedExecutionException.java
└── demo/
    ├── BasicThreadPoolDemo.java
    ├── RejectionPolicyDemo.java
    └── FutureDemo.java
```

## Demo Classes

### 1. BasicThreadPoolDemo
Demonstrates:
- Task submission
- Metrics monitoring
- Graceful shutdown
- awaitTermination

```bash
java com.lldprep.threadpool.demo.BasicThreadPoolDemo
```

### 2. RejectionPolicyDemo
Demonstrates:
- AbortPolicy (throws exception)
- CallerRunsPolicy (backpressure)
- DiscardPolicy (silent drop)

```bash
java com.lldprep.threadpool.demo.RejectionPolicyDemo
```

### 3. FutureDemo
Demonstrates:
- Callable task submission
- Future.get() with timeout
- Exception handling
- Task cancellation

```bash
java com.lldprep.threadpool.demo.FutureDemo
```

## API Reference

### CustomThreadPool Interface

```java
// Task submission
void submit(Runnable task)
<T> CustomFuture<T> submit(Callable<T> task)

// Lifecycle management
void shutdown()                                    // Graceful shutdown
List<Runnable> shutdownNow()                      // Immediate shutdown
boolean awaitTermination(long timeout, TimeUnit unit)

// State queries
boolean isShutdown()
boolean isTerminated()

// Metrics
int getActiveCount()
long getCompletedTaskCount()
ThreadPoolMetrics getMetrics()
```

### Builder API

```java
FixedThreadPool pool = new FixedThreadPool.Builder()
    .poolSize(10)                                  // Required
    .queueCapacity(100)                            // Optional (default: unbounded)
    .rejectionPolicy(new AbortPolicy())            // Optional (default: AbortPolicy)
    .threadFactory(new DefaultThreadFactory())     // Optional (default: DefaultThreadFactory)
    .build();
```

## SOLID Principles

### Single Responsibility Principle (SRP)
- `FixedThreadPool` - Manages lifecycle and task distribution
- `WorkerThread` - Executes tasks
- `RejectionPolicy` - Handles task rejection
- `ThreadFactory` - Creates threads

### Open/Closed Principle (OCP)
- Add new rejection policies without modifying pool code
- Add new thread factories without modifying pool code

### Liskov Substitution Principle (LSP)
- Any `RejectionPolicy` implementation is interchangeable
- Any `ThreadFactory` implementation is interchangeable

### Interface Segregation Principle (ISP)
- `RejectionPolicy` has single method: `reject()`
- `ThreadFactory` has single method: `newThread()`

### Dependency Inversion Principle (DIP)
- `FixedThreadPool` depends on abstractions (`RejectionPolicy`, `ThreadFactory`)
- Not on concrete implementations

## Comparison with Java's ThreadPoolExecutor

| Feature | Custom Implementation | Java's ThreadPoolExecutor |
|---------|----------------------|---------------------------|
| Task submission | ✅ Runnable/Callable | ✅ Runnable/Callable |
| Rejection policies | ✅ 4 policies | ✅ 4 policies |
| Bounded queue | ✅ Configurable | ✅ Configurable |
| Future support | ✅ Custom implementation | ✅ FutureTask |
| awaitTermination | ✅ CountDownLatch | ✅ |
| Metrics | ✅ Basic | ✅ Extensive |
| Dynamic sizing | ❌ Fixed only | ✅ Core/Max pools |
| Scheduled execution | ❌ | ✅ ScheduledThreadPoolExecutor |

## Thread Safety

### Lock-Free Structures
- `AtomicReference<PoolState>` for state transitions
- `AtomicInteger` for active count
- `AtomicLong` for completed task count

### Blocking Structures
- `BlockingQueue` for thread-safe task queue
- `CountDownLatch` for termination signaling

### Synchronization Points
- Task submission: Queue offer (non-blocking)
- Task retrieval: Queue poll with timeout
- Shutdown: Interrupt all workers

## Exception Handling

### Task Exceptions
- Caught in `WorkerThread.executeTask()`
- Logged to stderr
- Worker thread continues (no thread leak)
- Exception stored in `FutureTask` for retrieval

### Thread Exceptions
- `UncaughtExceptionHandler` set via `ThreadFactory`
- Fatal errors logged but don't crash pool

### Rejection Exceptions
- `AbortPolicy` throws `RejectedExecutionException`
- Other policies handle gracefully

## Performance Characteristics

### Time Complexity
- `submit()`: O(1) amortized
- `shutdown()`: O(k) where k = pool size
- `awaitTermination()`: O(1) wait on latch
- `get()` on Future: O(1) wait on latch

### Space Complexity
- O(k + n) where k = pool size, n = queue capacity
- Bounded queue prevents OutOfMemoryError

## Interview Talking Points

### Design Decisions
1. **Bounded queue by default** - Prevents OOM, forces explicit capacity planning
2. **Strategy pattern for rejection** - Different apps need different backpressure strategies
3. **Builder pattern** - Many optional parameters, validation before construction
4. **CountDownLatch for termination** - Simple, efficient wait mechanism

### Trade-offs
1. **Fixed pool size** - Simpler implementation, predictable resource usage
   - Alternative: Dynamic pool with core/max sizes
2. **BlockingQueue** - Simplicity and correctness over lock-free performance
   - Alternative: ConcurrentLinkedQueue + LockSupport.park()
3. **No priority queue** - FIFO is sufficient for most use cases
   - Alternative: PriorityBlockingQueue for priority-based scheduling

### Extensibility
- **Scheduled execution** - Add `DelayQueue` and `ScheduledFutureTask`
- **Work stealing** - Per-thread deques, workers steal from others
- **Dynamic resizing** - Add/remove workers based on load

## License

Educational implementation for LLD interview preparation.
