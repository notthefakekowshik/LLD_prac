# Task Scheduler - Design Document

## 1. Problem Statement

Design a task scheduling system that can:
- Execute tasks at specified times or intervals
- Support one-time and recurring execution
- Allow cancellation of scheduled tasks
- Provide observability through metrics
- Be thread-safe for concurrent scheduling and execution

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           TaskScheduler                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │   scheduleOnce   │  │ scheduleAtFixed│  │    scheduleAt        │  │
│  │   scheduleRecurring│  │scheduleWithDelay│  │      submit          │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────────┬───────────┘  │
│           │                       │                       │              │
│           └───────────────────────┼───────────────────────┘              │
│                                   │                                      │
│                           ┌───────▼───────┐                             │
│                           │ TaskRegistry  │                             │
│                           │ (Concurrent   │                             │
│                           │  HashMap)     │                             │
│                           └───────┬───────┘                             │
│                                   │                                      │
│                           ┌───────▼───────┐                             │
│                           │  DelayQueue   │                             │
│                           │ (Priority by  │                             │
│                           │  exec time)   │                             │
│                           └───────┬───────┘                             │
│                                   │                                      │
│                           ┌───────▼───────┐                             │
│                           │   Dispatcher  │                             │
│                           │    Thread     │                             │
│                           └───────┬───────┘                             │
│                                   │                                      │
│                           ┌───────▼───────┐                             │
│                           │  Worker Pool  │                             │
│                           │(Fixed Thread  │                             │
│                           │    Pool)      │                             │
│                           └───────────────┘                             │
└─────────────────────────────────────────────────────────────────────────┘
```

## 3. Component Design

### 3.1 Task Interface

The `Task` interface is the core abstraction representing a unit of work:

```java
public interface Task extends Comparable<Task> {
    String getId();                    // Unique identifier
    String getName();                  // Human-readable name
    void execute();                    // Task logic
    long getNextExecutionTime();       // When to run next
    boolean isRecurring();             // One-time vs recurring
    long getIntervalMillis();          // Delay between runs
    boolean onExecuted();              // Update state, return if reschedulable
}
```

**Design Decisions:**
- `Comparable<Task>` for natural ordering in priority queue
- `onExecuted()` handles both execution count tracking and rescheduling
- Separation of `execute()` (action) and `onExecuted()` (state management)

### 3.2 DelayQueueScheduler - Main Implementation

**Concurrency Model:**

```
┌──────────────────────────────────────────────────────────┐
│                   DelayQueueScheduler                     │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐│
│  │   Thread    │     │  DelayQueue │     │   Thread    ││
│  │  (Main)     │────▶│  (Tasks)    │────▶│(Dispatcher) ││
│  │ Schedules   │     │  Priority   │     │  Takes &    ││
│  │   Tasks     │     │  by time    │     │ Dispatches  ││
│  └─────────────┘     └─────────────┘     └──────┬──────┘│
│                                                  │       │
│                                                  ▼       │
│                                         ┌─────────────┐ │
│                                         │ Worker Pool │ │
│                                         │ (Executes   │ │
│                                         │   Tasks)    │ │
│                                         └─────────────┘ │
└──────────────────────────────────────────────────────────┘
```

**Key Mechanisms:**

1. **DelayQueue**: Thread-safe priority queue from `java.util.concurrent`
   - Orders tasks by `getDelay()` (time until execution)
   - `take()` blocks until a task's delay expires
   - `put()` is non-blocking and thread-safe

2. **Dispatcher Thread**: Single dedicated thread
   - Loops calling `taskQueue.take()` (blocks until ready)
   - Submits ready tasks to worker pool
   - Handles rescheduling for recurring tasks

3. **Worker Pool**: `ExecutorService` with fixed thread count
   - Executes actual task logic concurrently
   - Size configurable at construction
   - Isolates task execution from scheduling logic

4. **Task Registry**: `ConcurrentHashMap<String, Task>`
   - Tracks all scheduled tasks by ID
   - Enables cancellation lookups
   - Thread-safe concurrent access

### 3.3 DelayedTask - Adapter Pattern

`DelayQueue` requires elements to implement `Delayed`:

```java
private static class DelayedTask implements Delayed {
    private final Task task;
    
    @Override
    public long getDelay(TimeUnit unit) {
        return task.getNextExecutionTime() - System.currentTimeMillis();
    }
    
    @Override
    public int compareTo(Delayed other) {
        // Compare by remaining delay
    }
}
```

**Pattern**: Adapter - wraps `Task` to provide `Delayed` interface

### 3.4 Task States

```
                    ┌──────────────┐
                    │   SCHEDULED  │
                    └──────┬───────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
            ▼              ▼              ▼
     ┌──────────┐   ┌──────────┐   ┌──────────┐
     │ RUNNING  │   │ CANCELLED│   │   DONE   │
     └────┬─────┘   └──────────┘   └──────────┘
          │
          ▼
     ┌──────────┐
     │ COMPLETED│───▶ (Reschedule if recurring?)
     └──────────┘
```

State transitions:
- `SCHEDULED` → `RUNNING`: When dispatcher picks it up
- `SCHEDULED` → `CANCELLED`: When `cancel()` called before execution
- `RUNNING` → `COMPLETED`: After successful execution
- `COMPLETED` → `SCHEDULED`: If recurring (rescheduled)

**State Implementation:**
- `AtomicBoolean cancelled` - cancellation flag
- `AtomicBoolean running` - execution flag
- `AtomicLong executionCount` - track runs
- `AtomicLong nextExecutionTime` - when to run next

## 4. Thread Safety Analysis

### 4.1 Concurrent Operations

| Operation | Thread Safety | Mechanism |
|-----------|---------------|-----------|
| `schedule*()` | ✅ Safe | `ConcurrentHashMap.put()`, `DelayQueue.put()` |
| `cancel()` | ✅ Safe | `AtomicBoolean.set()`, `ConcurrentHashMap.remove()` |
| `execute()` | ✅ Safe | `compareAndSet` prevents double execution |
| `onExecuted()` | ✅ Safe | `AtomicLong.incrementAndGet()` |
| Dispatcher loop | ✅ Safe | `DelayQueue.take()` is thread-safe |

### 4.2 Potential Issues & Solutions

**Issue 1: Lost Rescheduling**
- **Problem**: Task completes and reschedules, but gets cancelled in between
- **Solution**: Check `cancelled` flag before re-adding to queue

**Issue 2: Race in Cancellation**
- **Problem**: Task cancelled just as dispatcher picks it up
- **Solution**: Check `isCancelled()` in both dispatcher and execution

**Issue 3: Memory Leaks**
- **Problem**: Completed tasks stay in registry
- **Solution**: Remove from registry after completion (non-recurring)

## 5. Design Patterns Used

### 5.1 Builder Pattern - TaskBuilder

```java
new TaskBuilder(scheduler, "heartbeat", runnable)
    .runEvery(30, TimeUnit.SECONDS)
    .startingIn(5, TimeUnit.SECONDS)
    .maxTimes(100)
    .schedule();
```

**Benefits:**
- Fluent, readable API
- Optional parameters without telescoping constructors
- Immutable task configuration

### 5.2 Strategy Pattern - Pluggable Scheduling

```java
public interface TaskScheduler {
    ScheduledTask scheduleOnce(...);
    ScheduledTask scheduleAtFixedRate(...);
    ScheduledTask scheduleWithFixedDelay(...);
}
```

Different scheduling strategies implemented in one class.
Future: Different `TaskScheduler` implementations (CronScheduler, DistributedScheduler).

### 5.3 Adapter Pattern - DelayedTask

Adapts `Task` interface to `Delayed` interface required by `DelayQueue`.

### 5.4 Producer-Consumer Pattern

- **Producers**: Threads calling `schedule*()` methods
- **Consumer**: Dispatcher thread calling `taskQueue.take()`
- **Buffer**: `DelayQueue` acts as the bounded buffer

### 5.5 Facade Pattern

`TaskScheduler` interface provides simple API hiding:
- DelayQueue complexity
- Thread pool management
- Concurrent state management

## 6. Performance Characteristics

### 6.1 Time Complexity

| Operation | Time | Notes |
|-----------|------|-------|
| Schedule | O(log n) | DelayQueue insert (heap operation) |
| Cancel | O(1) | HashMap lookup + flag set |
| Execute | O(1) | Direct method call |
| Reschedule | O(log n) | Remove old + insert new |
| Query metrics | O(1) | Atomic reads |

### 6.2 Space Complexity

- **Task storage**: O(n) where n = number of scheduled tasks
- **Worker threads**: O(p) where p = pool size
- **Total**: O(n + p)

### 6.3 Throughput Considerations

- **Bottleneck**: Single dispatcher thread
- **Mitigation**: Very lightweight (just takes and dispatches)
- **Worker capacity**: Pool size limits concurrent execution
- **Queue capacity**: Unbounded (DelayQueue grows as needed)

## 7. Error Handling & Fault Tolerance

### 7.1 Task Execution Failures

```java
private void executeTask(Task task) {
    try {
        task.execute();
        task.onExecuted();  // Update state
    } catch (Exception e) {
        // Log error
        // For recurring: still reschedule
        // For one-time: remove from registry
    }
}
```

**Policy:**
- One-time tasks: Removed on failure
- Recurring tasks: Rescheduled despite failure (fault tolerance)

### 7.2 Scheduler-Level Failures

- **Dispatcher crash**: Caught exceptions, continue loop
- **Worker pool exhaustion**: Tasks queue in thread pool
- **OOM on DelayQueue**: Unbounded growth risk (documented limitation)

## 8. Alternative Approaches Considered

### 8.1 PriorityBlockingQueue + Polling

```java
// Instead of DelayQueue
while (true) {
    Task task = queue.peek();
    if (task.getNextTime() <= now) {
        execute(queue.take());
    } else {
        Thread.sleep(pollingInterval);  // Wasteful!
    }
}
```

**Rejected:** Inefficient polling, imprecise timing.

### 8.2 ScheduledThreadPoolExecutor

Could wrap Java's built-in executor.

**Rejected for LLD:**
- Doesn't support absolute wall-clock time (our differentiator)
- Less control over internal mechanics
- Learning exercise: build from primitives

### 8.3 Timer/TimerTask

Java's legacy timer.

**Rejected:**
- Single thread (one slow task blocks all)
- No thread pool
- Exceptions kill the timer thread

## 9. Extensibility Points

### 9.1 Custom Scheduling Strategies

```java
public interface SchedulingStrategy {
    long calculateNextExecution(Task task, long lastExecution);
}

// Implementations:
// - CronStrategy (cron expressions)
// - ExponentialBackoffStrategy (retry with backoff)
// - BusinessHoursStrategy (only 9-5)
```

### 9.2 Task Persistence

For crash recovery:

```java
public interface TaskStore {
    void save(Task task);
    void delete(String taskId);
    List<Task> loadAll();
}

// Implementations:
// - InMemoryTaskStore (current)
// - FileTaskStore (JSON/CSV)
// - DatabaseTaskStore (SQL)
// - RedisTaskStore (distributed)
```

### 9.3 Distributed Scheduling

Multiple scheduler instances:

```java
public interface TaskLock {
    boolean acquire(String taskId);
    void release(String taskId);
}

// Redis-based distributed lock ensures only one instance executes
```

## 10. Testing Strategy

### 10.1 Unit Tests

```java
@Test
void testOneTimeTaskExecutes() {
    TaskScheduler scheduler = new DelayQueueScheduler(1);
    AtomicBoolean executed = new AtomicBoolean(false);
    
    scheduler.scheduleOnce("test", () -> executed.set(true), 100, MILLISECONDS);
    
    Thread.sleep(200);
    assertTrue(executed.get());
}

@Test
void testCancellation() {
    TaskScheduler scheduler = new DelayQueueScheduler(1);
    AtomicBoolean executed = new AtomicBoolean(false);
    
    ScheduledTask task = scheduler.scheduleOnce("test", () -> executed.set(true), 1, HOURS);
    task.cancel();
    
    assertTrue(task.isCancelled());
    assertFalse(executed.get());  // Never ran
}
```

### 10.2 Load Tests

- Schedule 10,000 tasks
- Measure throughput
- Verify no memory leaks
- Test with varying pool sizes

### 10.3 Concurrency Tests

- Multiple threads scheduling concurrently
- Cancel while executing
- Stress test dispatcher thread

## 11. Production Considerations

### 11.1 Monitoring

Metrics to track:
- `scheduler_tasks_pending` (gauge)
- `scheduler_tasks_executed_total` (counter)
- `scheduler_task_execution_duration` (histogram)
- `scheduler_worker_pool_utilization` (gauge)

### 11.2 Alerting

- Pending tasks > threshold (backup)
- Success rate < 95% (task failures)
- Worker pool saturated

### 11.3 Operational Commands

```java
// Runtime configuration
scheduler.setPoolSize(newSize);
scheduler.pause();   // Stop dispatching new tasks
scheduler.resume();  // Resume dispatching
```

## 12. Summary

This Task Scheduler demonstrates:
- **Concurrency mastery**: DelayQueue, thread pools, atomic operations
- **Design patterns**: Builder, Strategy, Adapter, Facade
- **Clean interfaces**: Separation of concerns
- **Production readiness**: Metrics, error handling, graceful shutdown

The architecture balances simplicity with extensibility, making it suitable for:
- Interview demonstrations
- Educational purposes
- Production use (with persistence layer added)
