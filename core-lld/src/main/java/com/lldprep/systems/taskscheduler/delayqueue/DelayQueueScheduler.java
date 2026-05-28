package com.lldprep.systems.taskscheduler.delayqueue;

import com.lldprep.systems.taskscheduler.*;
import com.lldprep.systems.taskscheduler.exception.TaskSchedulerException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Production-grade Task Scheduler implementation using DelayQueue.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  OPERATION TIME COMPLEXITY  (n = tasks in queue)                        │
 * ├──────────────────────────┬──────────────────────────────────────────────┤
 * │  schedule (insert)       │  O(log n)  — DelayQueue sift-up              │
 * │  dispatch take (dequeue) │  O(log n)  — blocks until delay expires,     │
 * │                          │             then sift-down; ZERO CPU idle     │
 * │  cancel by ID            │  O(1) avg  — ConcurrentHashMap lookup        │
 * │  cancelAll               │  O(n)      — iterate registry + clear both   │
 * │  getScheduledTaskCount   │  O(n)      — stream-filter registry           │
 * │  listIds                 │  O(n)      — copy keySet                      │
 * ├──────────────────────────┴──────────────────────────────────────────────┤
 * │  WAITING STRATEGY: DelayQueue.take() — leader-follower blocking         │
 * │    → dispatcher thread sleeps in OS until the earliest task is ready    │
 * │    → inserting a task with shorter delay immediately signals dispatcher  │
 * │    → zero CPU consumed while queue is idle or all tasks are in future   │
 * │                                                                          │
 * │  HOW DelayQueue WORKS INTERNALLY:                                        │
 * │    1. Wraps a PriorityQueue<Delayed> + ReentrantLock + Condition         │
 * │    2. put()  — acquires lock, offers to heap O(log n), signals if        │
 * │               new element is now the head (earliest deadline)            │
 * │    3. take() — acquires lock, peeks head; if delay > 0, one "leader"    │
 * │               thread calls condition.awaitNanos(delay) and releases      │
 * │               lock; other threads just call condition.await() forever.   │
 * │               When the leader's nanos expire, it polls the head O(log n) │
 * │               and signals the next waiter to become leader.              │
 * │    4. Result: only ONE thread ever times out (leader), rest block free.  │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Architecture:
 * - DelayQueue: Thread-safe priority queue for task ordering by execution time
 * - Dispatcher Thread: Single thread that blocks on take() until a task is ready
 * - Worker Thread Pool: Executes actual task logic (configurable size)
 * - Task Registry: ConcurrentHashMap for O(1) lookup and cancellation
 *
 * Design Patterns:
 * - Producer-Consumer: Main thread produces tasks, dispatcher consumes
 * - Thread Pool: Reuses worker threads for task execution
 * - Facade: Simple interface over complex concurrent machinery
 *
 * Thread Safety: All operations are thread-safe.
 */
public class DelayQueueScheduler implements TaskScheduler {

    // DelayQueue: O(log n) put/take, O(1) peek — blocking take() wakes precisely when delay expires
    private final DelayQueue<DelayedTask> taskQueue;
    // ConcurrentHashMap: O(1) avg get/put/remove — enables O(1) cancellation by task ID
    // (contrast with PriorityQueueScheduler which requires O(n) full-queue scan)
    private final ConcurrentHashMap<String, Task> taskRegistry;
    private final ExecutorService workerPool;
    private final Thread dispatcherThread;
    private final ReentrantLock schedulerLock;

    private final AtomicBoolean shutdown;
    private final int workerPoolSize;

    // Metrics
    private final AtomicLong tasksSubmitted;
    private final AtomicLong tasksExecuted;
    private final AtomicLong tasksCancelled;

    public DelayQueueScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public DelayQueueScheduler(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
        this.taskQueue = new DelayQueue<>();
        this.taskRegistry = new ConcurrentHashMap<>();
        this.workerPool = Executors.newFixedThreadPool(workerPoolSize);
        this.schedulerLock = new ReentrantLock();
        this.shutdown = new AtomicBoolean(false);

        this.tasksSubmitted = new AtomicLong(0);
        this.tasksExecuted = new AtomicLong(0);
        this.tasksCancelled = new AtomicLong(0);

        this.dispatcherThread = new Thread(this::dispatchLoop, "task-dispatcher");
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
    }

    /**
     * Blocks on DelayQueue.take() — zero CPU until a task is ready.
     * Automatically woken when a task with shorter delay is inserted.
     */
    private void dispatchLoop() {
        while (!shutdown.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // CRITICAL SECTION — shared mutable state
                DelayedTask delayedTask = taskQueue.take(); // O(log n) — blocks until delay expires, then sift-down
                Task task = delayedTask.getTask();

                if (task.isCancelled()) {
                    taskRegistry.remove(task.getId()); // O(1) avg
                    continue;
                }

                workerPool.submit(() -> executeTask(task));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Dispatcher error: " + e.getMessage());
            }
        }
    }

    private void executeTask(Task task) {
        try {
            if (task.isCancelled()) {
                taskRegistry.remove(task.getId()); // O(1) avg
                return;
            }

            task.execute();
            tasksExecuted.incrementAndGet();

            boolean shouldReschedule = task.onExecuted();

            if (shouldReschedule) {
                taskQueue.put(new DelayedTask(task)); // O(log n) — re-enqueue with updated delay
            } else {
                taskRegistry.remove(task.getId()); // O(1) avg
            }

        } catch (Exception e) {
            System.err.println("Task execution failed: " + task.getName() + " — " + e.getMessage());
            if (!task.isRecurring()) {
                taskRegistry.remove(task.getId()); // O(1) avg
            } else if (task.getExecutionCount() < task.getMaxExecutions() || task.getMaxExecutions() <= 0) {
                boolean shouldReschedule = task.onExecuted();
                if (shouldReschedule) {
                    taskQueue.put(new DelayedTask(task)); // O(log n)
                }
            }
        }
    }

    @Override
    public ScheduledTask scheduleOnce(String name, Runnable action, long delay, TimeUnit unit) {
        checkShutdown();
        long delayMillis = unit.toMillis(delay);
        return scheduleInternal(name, action, delayMillis, 0, 1);
    }

    @Override
    public ScheduledTask scheduleAt(String name, Runnable action, LocalDateTime dateTime) {
        checkShutdown();
        long delayMillis = calculateDelayFromDateTime(dateTime);
        return scheduleInternal(name, action, delayMillis, 0, 1);
    }

    @Override
    public ScheduledTask scheduleAt(String name, Runnable action, LocalTime time) {
        checkShutdown();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledTime = now.with(time);
        if (scheduledTime.isBefore(now)) {
            scheduledTime = scheduledTime.plusDays(1);
        }
        return scheduleAt(name, action, scheduledTime);
    }

    @Override
    public ScheduledTask scheduleAtFixedRate(String name, Runnable action,
                                              long initialDelay, long period, TimeUnit unit) {
        checkShutdown();
        long initialDelayMillis = unit.toMillis(initialDelay);
        long periodMillis = unit.toMillis(period);
        return scheduleInternal(name, action, initialDelayMillis, periodMillis, 0);
    }

    @Override
    public ScheduledTask scheduleWithFixedDelay(String name, Runnable action,
                                                 long initialDelay, long delay, TimeUnit unit) {
        checkShutdown();
        long initialDelayMillis = unit.toMillis(initialDelay);
        long delayMillis = unit.toMillis(delay);
        return scheduleInternal(name, action, initialDelayMillis, delayMillis, 0);
    }

    @Override
    public ScheduledTask scheduleRecurring(String name, Runnable action, Duration interval) {
        checkShutdown();
        long intervalMillis = interval.toMillis();
        return scheduleInternal(name, action, intervalMillis, intervalMillis, 0);
    }

    @Override
    public ScheduledTask submit(String name, Runnable action) {
        return scheduleOnce(name, action, 0, TimeUnit.MILLISECONDS);
    }

    private ScheduledTask scheduleInternal(String name, Runnable action,
                                           long initialDelayMillis, long intervalMillis,
                                           long maxExecutions) {
        schedulerLock.lock();
        try {
            String taskId = generateTaskId();
            DefaultTask task = new DefaultTask(taskId, name, action,
                initialDelayMillis, intervalMillis, maxExecutions);

            taskRegistry.put(taskId, task);        // O(1) avg
            taskQueue.put(new DelayedTask(task));   // O(log n) — sift-up; wakes dispatcher if new head
            tasksSubmitted.incrementAndGet();

            return new DefaultScheduledTask(task);
        } finally {
            schedulerLock.unlock();
        }
    }

    @Override
    public boolean cancel(String taskId) {
        Task task = taskRegistry.get(taskId); // O(1) avg
        if (task == null) {
            return false;
        }

        if (task.isCancelled() || task.getExecutionCount() > 0 && !task.isRecurring()) {
            return false;
        }

        task.cancel();
        tasksCancelled.incrementAndGet();
        taskRegistry.remove(taskId); // O(1) avg — key advantage over PriorityQueueScheduler's O(n)
        return true;
    }

    @Override
    public void cancelAll() {
        for (Task task : taskRegistry.values()) { // O(n)
            task.cancel();
        }
        tasksCancelled.addAndGet(taskRegistry.size());
        taskRegistry.clear(); // O(n)
        taskQueue.clear();    // O(n)
    }

    @Override
    public int getScheduledTaskCount() {
        return (int) taskRegistry.values().stream() // O(n) — iterates all registered tasks
            .filter(t -> !t.isCancelled())
            .filter(t -> t.isRecurring() || t.getExecutionCount() == 0)
            .count();
    }

    @Override
    public List<String> getScheduledTaskIds() {
        return new ArrayList<>(taskRegistry.keySet()); // O(n) — copies all keys
    }

    @Override
    public boolean shutdown(long timeout, TimeUnit unit) {
        shutdown.set(true);
        dispatcherThread.interrupt();
        cancelAll();
        workerPool.shutdown();
        try {
            return workerPool.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public SchedulerMetrics getMetrics() {
        return new SchedulerMetrics(
            tasksSubmitted.get(),
            tasksExecuted.get(),
            tasksCancelled.get(),
            getScheduledTaskCount(),
            workerPoolSize
        );
    }

    private void checkShutdown() {
        if (shutdown.get()) {
            throw new TaskSchedulerException("Scheduler has been shut down");
        }
    }

    private long calculateDelayFromDateTime(LocalDateTime dateTime) {
        long targetMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long nowMillis = System.currentTimeMillis();
        return Math.max(0, targetMillis - nowMillis);
    }

    private String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Adapts Task to Delayed for use in DelayQueue.
     */
    private static class DelayedTask implements Delayed {
        private final Task task;

        DelayedTask(Task task) {
            this.task = task;
        }

        Task getTask() {
            return task;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delayMillis = task.getNextExecutionTime() - System.currentTimeMillis(); // O(1)
            return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) { // O(1) — required by DelayQueue for heap ordering
            long diff = this.getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(diff, 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DelayedTask that = (DelayedTask) o;
            return task.getId().equals(that.task.getId());
        }

        @Override
        public int hashCode() {
            return task.getId().hashCode();
        }
    }
}
