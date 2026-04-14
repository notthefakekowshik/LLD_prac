package com.lldprep.taskscheduler;

import com.lldprep.taskscheduler.exception.TaskSchedulerException;

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
 * Architecture:
 * - DelayQueue: Thread-safe priority queue for task ordering by execution time
 * - Dispatcher Thread: Single thread that waits for tasks and dispatches to workers
 * - Worker Thread Pool: Executes actual task logic (configurable size)
 * - Task Registry: Tracks all scheduled tasks
 * 
 * Design Patterns:
 * - Producer-Consumer: Main thread produces tasks, dispatcher consumes
 * - Thread Pool: Reuses worker threads for task execution
 * - Facade: Simple interface over complex concurrent machinery
 * 
 * Thread Safety: All operations are thread-safe.
 */
public class DelayQueueScheduler implements TaskScheduler {
    
    private final DelayQueue<DelayedTask> taskQueue;
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
    
    /**
     * Creates a scheduler with default worker pool size (CPU cores).
     */
    public DelayQueueScheduler() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates a scheduler with specified worker pool size.
     * 
     * @param workerPoolSize Number of worker threads for task execution
     */
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
        
        // Start the dispatcher thread
        this.dispatcherThread = new Thread(this::dispatchLoop, "task-dispatcher");
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
    }
    
    /**
     * Main dispatcher loop. Blocks on DelayQueue until tasks are ready.
     */
    private void dispatchLoop() {
        while (!shutdown.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Block until a task's delay expires
                DelayedTask delayedTask = taskQueue.take();
                Task task = delayedTask.getTask();
                
                if (task.isCancelled()) {
                    taskRegistry.remove(task.getId());
                    continue;
                }
                
                // Submit to worker pool for execution
                workerPool.submit(() -> executeTask(task));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log and continue - don't let one bad task kill the scheduler
                System.err.println("Dispatcher error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Executes a single task and handles rescheduling if recurring.
     */
    private void executeTask(Task task) {
        try {
            if (task.isCancelled()) {
                taskRegistry.remove(task.getId());
                return;
            }
            
            // Execute the task
            task.execute();
            tasksExecuted.incrementAndGet();
            
            // Handle rescheduling for recurring tasks
            boolean shouldReschedule = task.onExecuted();
            
            if (shouldReschedule) {
                // Re-queue the task with updated delay
                taskQueue.put(new DelayedTask(task));
            } else {
                // Task is complete, remove from registry
                taskRegistry.remove(task.getId());
            }
            
        } catch (Exception e) {
            System.err.println("Task execution failed: " + task.getName() + " - " + e.getMessage());
            // For recurring tasks, we might want to retry or still reschedule
            // For simplicity, we remove failed one-time tasks but keep recurring ones
            if (!task.isRecurring()) {
                taskRegistry.remove(task.getId());
            } else if (task.getExecutionCount() < task.getMaxExecutions() || task.getMaxExecutions() <= 0) {
                // Try to reschedule even after failure
                boolean shouldReschedule = task.onExecuted();
                if (shouldReschedule) {
                    taskQueue.put(new DelayedTask(task));
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
        
        // If time has passed today, schedule for tomorrow
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
        // For fixed delay, we use the same mechanism but calculate delay after execution
        // This is a simplified implementation that treats it like fixed rate
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
    
    /**
     * Internal scheduling method.
     */
    private ScheduledTask scheduleInternal(String name, Runnable action, 
                                           long initialDelayMillis, long intervalMillis, 
                                           long maxExecutions) {
        schedulerLock.lock();
        try {
            String taskId = generateTaskId();
            DefaultTask task = new DefaultTask(taskId, name, action, 
                                               initialDelayMillis, intervalMillis, maxExecutions);
            
            taskRegistry.put(taskId, task);
            taskQueue.put(new DelayedTask(task));
            tasksSubmitted.incrementAndGet();
            
            return new DefaultScheduledTask(task);
        } finally {
            schedulerLock.unlock();
        }
    }
    
    @Override
    public boolean cancel(String taskId) {
        Task task = taskRegistry.get(taskId);
        if (task == null) {
            return false;
        }
        
        if (task.isCancelled() || task.getExecutionCount() > 0 && !task.isRecurring()) {
            return false;
        }
        
        task.cancel();
        tasksCancelled.incrementAndGet();
        taskRegistry.remove(taskId);
        return true;
    }
    
    @Override
    public void cancelAll() {
        for (Task task : taskRegistry.values()) {
            task.cancel();
        }
        tasksCancelled.addAndGet(taskRegistry.size());
        taskRegistry.clear();
        taskQueue.clear();
    }
    
    @Override
    public int getScheduledTaskCount() {
        return (int) taskRegistry.values().stream()
            .filter(t -> !t.isCancelled())
            .filter(t -> t.isRecurring() || t.getExecutionCount() == 0)
            .count();
    }
    
    @Override
    public List<String> getScheduledTaskIds() {
        return new ArrayList<>(taskRegistry.keySet());
    }
    
    @Override
    public boolean shutdown(long timeout, TimeUnit unit) {
        shutdown.set(true);
        dispatcherThread.interrupt();
        
        // Cancel all pending tasks
        cancelAll();
        
        // Shutdown worker pool
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
     * Wrapper that adapts Task to Delayed for use in DelayQueue.
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
            long delayMillis = task.getNextExecutionTime() - System.currentTimeMillis();
            return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
        }
        
        @Override
        public int compareTo(Delayed other) {
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
