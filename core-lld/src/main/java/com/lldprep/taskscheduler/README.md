# Task Scheduler - Implementation Guide

## Overview

A production-grade, thread-safe task scheduling system implementing multiple scheduling strategies:

- **One-time delayed execution** - Run once after a delay
- **Absolute time scheduling** - Run at specific date/time (wall-clock or epoch)
- **Recurring (fixed rate)** - Run every N milliseconds regardless of execution time
- **Recurring (fixed delay)** - Run N milliseconds after previous completion
- **Cancellable tasks** - Cancel scheduled tasks before execution

## Quick Start

```java
// Create scheduler with 4 worker threads
TaskScheduler scheduler = new DelayQueueScheduler(4);

// One-time delayed task
ScheduledTask task = scheduler.scheduleOnce(
    "email-job",
    () -> sendEmail(),
    5, TimeUnit.MINUTES
);

// Recurring task (every 30 seconds)
ScheduledTask recurring = scheduler.scheduleAtFixedRate(
    "heartbeat",
    () -> pingServer(),
    0, 30, TimeUnit.SECONDS
);

// Schedule at specific time
scheduler.scheduleAt(
    "daily-report",
    () -> generateReport(),
    LocalTime.of(9, 0)  // 9:00 AM today or tomorrow
);

// Cancel a task
task.cancel();

// Shutdown gracefully
scheduler.shutdown(10, TimeUnit.SECONDS);
```

## Scheduling Patterns

### 1. One-Time Delayed Execution

Run a task once after a specified delay:

```java
// Run after 5 seconds
scheduler.scheduleOnce("quick-task", runnable, 5, TimeUnit.SECONDS);

// Run after 100ms
scheduler.scheduleOnce("fast-task", runnable, 100, TimeUnit.MILLISECONDS);

// Using Duration
scheduler.scheduleOnce("duration-task", runnable, 
    Duration.ofMinutes(10).toMillis(), TimeUnit.MILLISECONDS);
```

### 2. Absolute Time Scheduling

Run at a specific wall-clock time or date-time:

```java
// At 2:30 PM today (or tomorrow if already passed)
scheduler.scheduleAt("lunch-reminder", runnable, LocalTime.of(14, 30));

// At specific date and time
LocalDateTime meetingTime = LocalDateTime.of(2025, 4, 15, 10, 0);
scheduler.scheduleAt("meeting-alert", runnable, meetingTime);
```

### 3. Recurring Tasks (Fixed Rate)

Execute at fixed intervals, regardless of execution time:

```java
// Every 5 seconds, starting immediately
scheduler.scheduleAtFixedRate("poller", runnable, 0, 5, TimeUnit.SECONDS);

// Every hour, starting in 10 minutes
scheduler.scheduleAtFixedRate("hourly-job", runnable, 
    10, 60, TimeUnit.MINUTES);

// Using Duration
scheduler.scheduleRecurring("interval-task", runnable, Duration.ofMinutes(5));
```

### 4. Recurring Tasks (Fixed Delay)

Wait N time units after each execution completes:

```java
// Start immediately, wait 2 seconds after each completion
scheduler.scheduleWithFixedDelay("processor", runnable, 0, 2, TimeUnit.SECONDS);
```

### 5. Builder Pattern (Fluent API)

For complex task configuration:

```java
ScheduledTask task = new TaskBuilder(scheduler, "complex-job", () -> {
    // task logic
})
    .runEvery(30, TimeUnit.SECONDS)   // Recurring
    .startingIn(5, TimeUnit.SECONDS)  // Initial delay
    .maxTimes(100)                     // Limit executions
    .schedule();

// One-time with delay
ScheduledTask once = new TaskBuilder(scheduler, "delayed", runnable)
    .runOnce()
    .withDelay(10, TimeUnit.MINUTES)
    .schedule();

// At specific time
ScheduledTask atTime = new TaskBuilder(scheduler, "daily", runnable)
    .scheduleAt(LocalTime.of(9, 0));
```

## Task Management

### Cancellation

```java
ScheduledTask task = scheduler.scheduleOnce("temp", runnable, 1, TimeUnit.HOURS);

// Cancel before it runs
boolean cancelled = task.cancel();
System.out.println("Cancelled: " + cancelled);  // true

// Check status
System.out.println("Is cancelled: " + task.isCancelled());  // true
System.out.println("Is done: " + task.isDone());  // true
```

### Querying Tasks

```java
// Get all scheduled task IDs
List<String> taskIds = scheduler.getScheduledTaskIds();

// Count pending tasks
int count = scheduler.getScheduledTaskCount();

// Get task info
System.out.println("Task: " + task.getName());
System.out.println("Executions: " + task.getExecutionCount());
System.out.println("Next run: " + task.getNextExecutionTime());
System.out.println("Delay: " + task.getDelay(TimeUnit.SECONDS) + " seconds");
```

### Bulk Operations

```java
// Cancel all tasks
scheduler.cancelAll();

// Check if shutdown
boolean isShutdown = scheduler.isShutdown();
```

## Monitoring & Metrics

```java
SchedulerMetrics metrics = scheduler.getMetrics();

System.out.println("Submitted:  " + metrics.tasksSubmitted());
System.out.println("Executed:   " + metrics.tasksExecuted());
System.out.println("Cancelled:  " + metrics.tasksCancelled());
System.out.println("Pending:    " + metrics.pendingTasks());
System.out.println("Pool Size:  " + metrics.workerPoolSize());
System.out.println("In Flight:  " + metrics.tasksInFlight());
System.out.println("Success:    " + (metrics.successRate() * 100) + "%");
```

## Thread Safety

All operations are thread-safe:
- Multiple threads can schedule tasks concurrently
- Tasks can be cancelled from any thread
- Worker threads execute tasks concurrently (configurable pool size)
- Metrics are updated atomically

## Shutdown

Graceful shutdown with timeout:

```java
// Try graceful shutdown, wait up to 10 seconds
boolean graceful = scheduler.shutdown(10, TimeUnit.SECONDS);

if (!graceful) {
    // Some tasks didn't finish in time
    System.out.println("Warning: Forced shutdown");
}
```

## Architecture

### Components

1. **DelayQueue**: Thread-safe priority queue ordered by execution time
2. **Dispatcher Thread**: Single thread that waits for ready tasks
3. **Worker Pool**: Fixed thread pool for task execution
4. **Task Registry**: Concurrent map tracking all scheduled tasks

### Design Patterns Used

- **Strategy**: Pluggable scheduling algorithms
- **Builder**: Fluent task construction
- **Adapter**: DelayedTask wraps Task for DelayQueue
- **Facade**: Simple interface over complex concurrency
- **Producer-Consumer**: Tasks are produced, dispatcher consumes
- **Thread Pool**: Reuses worker threads

### Performance

- **O(log n)** for task scheduling (DelayQueue insert)
- **O(1)** for cancellation (registry lookup)
- **O(1)** for metrics reads
- Bounded by worker pool size for concurrent execution

## Error Handling

Tasks that throw exceptions:
- One-time tasks: Removed from scheduler
- Recurring tasks: Rescheduled for next interval (fault tolerance)

## Comparison with Java's ScheduledExecutorService

| Feature | This Scheduler | ScheduledExecutorService |
|---------|---------------|--------------------------|
| Absolute time | ✅ Yes (wall-clock) | ❌ No (only delays) |
| Builder API | ✅ Yes | ❌ No |
| Metrics | ✅ Built-in | ❌ Manual |
| Task tracking | ✅ By ID | ❌ By Future only |
| Fixed delay | ✅ Yes | ✅ Yes |
| Fixed rate | ✅ Yes | ✅ Yes |

## Best Practices

1. **Use meaningful names**: Helps with debugging and metrics
2. **Handle exceptions**: Wrap task logic in try-catch
3. **Set worker pool size**: Based on expected task load
4. **Cancel unused tasks**: Prevent memory leaks
5. **Monitor metrics**: Watch for execution failures
6. **Graceful shutdown**: Always call shutdown() on application exit

## Example: Complete Workflow

```java
public class ReportScheduler {
    private final TaskScheduler scheduler;
    
    public ReportScheduler() {
        this.scheduler = new DelayQueueScheduler(2);
    }
    
    public void start() {
        // Daily report at 9 AM
        scheduler.scheduleAt("daily-report", this::generateDailyReport, 
            LocalTime.of(9, 0));
        
        // Hourly status check
        scheduler.scheduleAtFixedRate("status-check", this::checkStatus,
            0, 1, TimeUnit.HOURS);
        
        // Weekly report (limited to 52 runs = 1 year)
        new TaskBuilder(scheduler, "weekly-report", this::generateWeeklyReport)
            .runEvery(7, TimeUnit.DAYS)
            .scheduleAt(LocalTime.of(8, 0))
            .maxTimes(52);
    }
    
    public void stop() {
        scheduler.shutdown(30, TimeUnit.SECONDS);
    }
    
    private void generateDailyReport() {
        System.out.println("Generating daily report at " + LocalTime.now());
        // ... report logic
    }
    
    private void checkStatus() {
        System.out.println("Checking system status...");
        // ... status check logic
    }
    
    private void generateWeeklyReport() {
        System.out.println("Generating weekly report at " + LocalTime.now());
        // ... report logic
    }
}
```
