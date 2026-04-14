package com.lldprep.taskscheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Fluent Builder for creating and scheduling tasks.
 * 
 * Design Patterns:
 * - Builder Pattern: Step-by-step construction with method chaining
 * - Fluent Interface: Readable, chainable method calls
 * 
 * Usage:
 * <pre>
 * ScheduledTask task = scheduler.newTask("daily-report", () -> generateReport())
 *     .runOnce()
 *     .withDelay(5, TimeUnit.MINUTES)
 *     .schedule();
 * 
 * ScheduledTask recurring = scheduler.newTask("heartbeat", () -> sendPing())
 *     .runEvery(30, TimeUnit.SECONDS)
 *     .schedule();
 * </pre>
 */
public class TaskBuilder {
    
    private final TaskScheduler scheduler;
    private final String name;
    private final Runnable action;
    
    private long initialDelayMillis = 0;
    private long intervalMillis = 0;
    private long maxExecutions = 1;
    private boolean isRecurring = false;
    
    public TaskBuilder(TaskScheduler scheduler, String name, Runnable action) {
        this.scheduler = scheduler;
        this.name = name;
        this.action = action;
    }
    
    /**
     * Configure as one-time task (default).
     */
    public TaskBuilder runOnce() {
        this.isRecurring = false;
        this.intervalMillis = 0;
        this.maxExecutions = 1;
        return this;
    }
    
    /**
     * Configure as recurring task with fixed rate.
     */
    public TaskBuilder runEvery(long interval, TimeUnit unit) {
        this.isRecurring = true;
        this.intervalMillis = unit.toMillis(interval);
        this.maxExecutions = 0; // Unlimited
        return this;
    }
    
    /**
     * Configure as recurring task with Duration.
     */
    public TaskBuilder runEvery(Duration interval) {
        this.isRecurring = true;
        this.intervalMillis = interval.toMillis();
        this.maxExecutions = 0;
        return this;
    }
    
    /**
     * Set delay before first execution.
     */
    public TaskBuilder withDelay(long delay, TimeUnit unit) {
        this.initialDelayMillis = unit.toMillis(delay);
        return this;
    }
    
    /**
     * Set delay before first execution using Duration.
     */
    public TaskBuilder withDelay(Duration delay) {
        this.initialDelayMillis = delay.toMillis();
        return this;
    }
    
    /**
     * Set initial delay and make recurring.
     */
    public TaskBuilder startingIn(long delay, TimeUnit unit) {
        this.initialDelayMillis = unit.toMillis(delay);
        return this;
    }
    
    /**
     * Limit the number of executions.
     */
    public TaskBuilder maxTimes(long count) {
        this.maxExecutions = count;
        return this;
    }
    
    /**
     * Schedule at a specific time today (or tomorrow if passed).
     */
    public ScheduledTask scheduleAt(LocalTime time) {
        return scheduler.scheduleAt(name, action, time);
    }
    
    /**
     * Schedule at a specific date and time.
     */
    public ScheduledTask scheduleAt(LocalDateTime dateTime) {
        return scheduler.scheduleAt(name, action, dateTime);
    }
    
    /**
     * Build and schedule the task with configured parameters.
     */
    public ScheduledTask schedule() {
        if (isRecurring) {
            return scheduler.scheduleAtFixedRate(name, action, initialDelayMillis, intervalMillis, TimeUnit.MILLISECONDS);
        } else {
            return scheduler.scheduleOnce(name, action, initialDelayMillis, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Creates a unique task ID.
     */
    private String generateId() {
        return name.toLowerCase().replaceAll("\\s+", "-") + "-" + 
               UUID.randomUUID().toString().substring(0, 6);
    }
}
