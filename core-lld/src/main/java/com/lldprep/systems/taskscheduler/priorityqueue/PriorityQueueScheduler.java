package com.lldprep.systems.taskscheduler.priorityqueue;

import com.lldprep.systems.taskscheduler.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Task Scheduler implementation using PriorityQueue instead of DelayQueue.
 *
 * THIS IS INTENTIONALLY INFERIOR — demonstrates why DelayQueue exists.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  OPERATION TIME COMPLEXITY  (n = tasks in queue)                        │
 * ├──────────────────────────┬──────────────────────────────────────────────┤
 * │  schedule (insert)       │  O(log n)  — PriorityQueue sift-up           │
 * │  dispatch peek           │  O(1)      — read heap root                  │
 * │  dispatch poll (dequeue) │  O(log n)  — remove root + sift-down         │
 * │  cancel by ID            │  O(n)      — FULL SCAN, no index             │
 * │  cancelAll               │  O(n)      — iterate + clear                 │
 * │  size                    │  O(1)      — maintained as internal field     │
 * │  listIds                 │  O(n)      — stream all elements              │
 * ├──────────────────────────┴──────────────────────────────────────────────┤
 * │  WAITING STRATEGY: busy-poll every POLL_INTERVAL_MS                     │
 * │    → CPU burns even when queue is idle                                  │
 * │    → tasks can fire up to POLL_INTERVAL_MS late                         │
 * │    → new shorter-delay task inserted while sleeping = missed wakeup     │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * The Pain Points:
 * 1. BUSY-WAITING: Must constantly poll to check if tasks are ready
 * 2. SLEEP DILEMMA: Short sleep = CPU burn. Long sleep = imprecise timing.
 * 3. NO BLOCKING: Can't block until task ready — must poll repeatedly
 * 4. WAKEUP PROBLEM: New earlier task inserted? Already sleeping, won't wake.
 * 5. O(n) CANCEL: No HashMap index — must scan entire queue to find task.
 *
 * Compare to DelayQueue:
 * - DelayQueue.take() blocks precisely until delay expires
 * - DelayQueue uses "leader" thread pattern — only one thread waits with timeout
 * - DelayQueue wakes immediately when shorter-delay task inserted
 */
public class PriorityQueueScheduler implements TaskScheduler {

    // Core data structure: PriorityQueue ordered by execution time
    // PROBLEM 1: No built-in delay awareness — just a sorted collection
    // Time complexity: offer O(log n), poll O(log n), peek O(1), remove(element) O(n)
    private final PriorityQueue<Task> taskQueue;

    // Need external lock because PriorityQueue is NOT thread-safe
    // DelayQueue has internal ReentrantLock — we must manage our own
    private final ReentrantLock queueLock = new ReentrantLock();

    // Dispatcher thread — must POLL instead of blocking
    private final Thread dispatcherThread;
    private volatile boolean running = true;

    // THE SLEEP DILEMMA: How long to sleep between polls?
    // - 1ms: Responsive but burns CPU
    // - 100ms: CPU-friendly but tasks execute up to 100ms late
    private static final long POLL_INTERVAL_MS = 10; // Trade-off: accuracy vs CPU

    public PriorityQueueScheduler() {
        this.taskQueue = new PriorityQueue<>();

        this.dispatcherThread = new Thread(this::pollLoop, "pq-dispatcher");
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
    }

    /**
     * THE PAIN: Polling loop instead of blocking take().
     *
     * With DelayQueue:
     *   DelayedTask task = queue.take(); // Blocks precisely until ready
     *
     * With PriorityQueue:
     *   while (true) {
     *       Task task = queue.peek();
     *       if (task != null && task.getNextExecutionTime() <= now) {
     *           queue.poll();
     *       } else {
     *           Thread.sleep(POLL_INTERVAL); // IMPRECISE!
     *       }
     *   }
     */
    private void pollLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            queueLock.lock();
            try {
                Task task = taskQueue.peek(); // O(1) — reads heap root without removing

                if (task == null) {
                    queueLock.unlock();
                    sleep(POLL_INTERVAL_MS);
                    continue;
                }

                long now = System.currentTimeMillis();
                long delay = task.getNextExecutionTime() - now;

                if (delay <= 0) {
                    taskQueue.poll(); // O(log n) — same as DelayQueue
                    queueLock.unlock();
                    executeTask(task);
                } else {
                    // PROBLEM: If a new task with shorter delay is inserted while we sleep,
                    // we won't know until we wake up!
                    queueLock.unlock();
                    sleep(Math.min(delay, POLL_INTERVAL_MS));
                }

            } catch (Exception e) {
                System.err.println("Poll error: " + e.getMessage());
                if (queueLock.isHeldByCurrentThread()) {
                    queueLock.unlock();
                }
            }
        }
    }

    /**
     * THE PAIN: Signaling complexity on insert.
     *
     * If this task is now the earliest in the queue, the dispatcher is
     * sleeping and won't know. With DelayQueue, inserting a shorter-delay
     * task immediately signals the waiting thread.
     */
    @Override
    public ScheduledTask scheduleOnce(String name, Runnable action, long delay, TimeUnit unit) {
        long delayMillis = unit.toMillis(delay);
        String taskId = generateTaskId();

        DefaultTask task = new DefaultTask(taskId, name, action, delayMillis, 0, 1);

        queueLock.lock();
        try {
            taskQueue.offer(task); // O(log n)
        } finally {
            queueLock.unlock();
        }

        return new DefaultScheduledTask(task);
    }

    @Override
    public ScheduledTask scheduleAt(String name, Runnable action, LocalDateTime dateTime) {
        long delayMillis = calculateDelayFromDateTime(dateTime);
        return scheduleOnce(name, action, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledTask scheduleAt(String name, Runnable action, LocalTime time) {
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
        long initialDelayMillis = unit.toMillis(initialDelay);
        long periodMillis = unit.toMillis(period);
        String taskId = generateTaskId();

        DefaultTask task = new DefaultTask(taskId, name, action, initialDelayMillis, periodMillis, 0);

        queueLock.lock();
        try {
            taskQueue.offer(task); // O(log n) — sift-up to maintain heap order
        } finally {
            queueLock.unlock();
        }

        return new DefaultScheduledTask(task);
    }

    @Override
    public ScheduledTask scheduleWithFixedDelay(String name, Runnable action,
                                                 long initialDelay, long delay, TimeUnit unit) {
        return scheduleAtFixedRate(name, action, initialDelay, delay, unit);
    }

    @Override
    public ScheduledTask scheduleRecurring(String name, Runnable action, Duration interval) {
        return scheduleAtFixedRate(name, action, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledTask submit(String name, Runnable action) {
        return scheduleOnce(name, action, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean cancel(String taskId) {
        queueLock.lock();
        try {
            // PROBLEM: PriorityQueue.remove() is O(n) — must scan entire queue!
            // DelayQueue uses HashMap for O(1) removal
            for (Task task : taskQueue) {
                if (task.getId().equals(taskId)) {
                    taskQueue.remove(task); // O(n) — yuck!
                    task.cancel();
                    return true;
                }
            }
            return false;
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public void cancelAll() {
        queueLock.lock();
        try {
            for (Task task : taskQueue) { // O(n) — iterate entire queue
                task.cancel();
            }
            taskQueue.clear(); // O(n) — nulls all element references
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public int getScheduledTaskCount() {
        queueLock.lock();
        try {
            return taskQueue.size(); // O(1) — maintained as a field inside PriorityQueue
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public List<String> getScheduledTaskIds() {
        queueLock.lock();
        try {
            return taskQueue.stream() // O(n) — visits every element
                .map(Task::getId)
                .toList();
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public boolean shutdown(long timeout, TimeUnit unit) {
        running = false;
        dispatcherThread.interrupt();
        return true;
    }

    @Override
    public boolean isShutdown() {
        return !running;
    }

    @Override
    public SchedulerMetrics getMetrics() {
        return new SchedulerMetrics(0, 0, 0, getScheduledTaskCount(), 1);
    }

    private void executeTask(Task task) {
        new Thread(task::execute).start();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long calculateDelayFromDateTime(LocalDateTime dateTime) {
        long targetMillis = dateTime.atZone(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli();
        long nowMillis = System.currentTimeMillis();
        return Math.max(0, targetMillis - nowMillis);
    }

    private String generateTaskId() {
        return "pq-task-" + System.currentTimeMillis() % 10000;
    }
}

/*
 * SUMMARY OF PAIN vs DelayQueue:
 *
 * Aspect          | PriorityQueue                          | DelayQueue
 * ----------------+----------------------------------------+---------------------------
 * Waiting         | Busy-polling with Thread.sleep()       | take() blocks precisely
 * Precision       | ±POLL_INTERVAL_MS drift                | Nanosecond precision
 * Cancellation    | O(n) scan entire queue                 | O(1) HashMap lookup
 * Wakeup          | Sleeping thread misses earlier tasks   | Signal wakes immediately
 * CPU             | Burns cycles polling                   | Zero CPU when idle
 * Thread-safety   | Must manage own ReentrantLock          | Internally synchronized
 */
