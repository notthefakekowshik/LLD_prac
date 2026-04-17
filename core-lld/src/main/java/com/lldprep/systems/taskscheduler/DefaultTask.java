package com.lldprep.systems.taskscheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of the Task interface.
 * 
 * Design Patterns:
 * - Builder Pattern: Complex object construction via TaskBuilder
 * - State Pattern: Manages lifecycle states (SCHEDULED, RUNNING, COMPLETED, CANCELLED)
 * - Command Pattern: Encapsulates execution as an object
 * 
 * Thread Safety: All state mutations are atomic.
 */
public class DefaultTask implements Task {
    
    private final String id;
    private final String name;
    private final Runnable action;
    private final long intervalMillis;
    private final long maxExecutions;
    
    private final AtomicLong nextExecutionTime;
    private final AtomicLong executionCount;
    private final AtomicBoolean cancelled;
    private final AtomicBoolean running;
    
    // Volatile for visibility across threads
    private volatile long lastExecutionTime;
    
    /**
     * Creates a one-time task.
     */
    public DefaultTask(String id, String name, Runnable action, long delayMillis) {
        this(id, name, action, delayMillis, 0, 1);
    }
    
    /**
     * Creates a recurring task.
     */
    public DefaultTask(String id, String name, Runnable action, 
                       long initialDelayMillis, long intervalMillis, long maxExecutions) {
        this.id = id;
        this.name = name;
        this.action = action;
        this.intervalMillis = intervalMillis;
        this.maxExecutions = maxExecutions;
        
        this.nextExecutionTime = new AtomicLong(System.currentTimeMillis() + initialDelayMillis);
        this.executionCount = new AtomicLong(0);
        this.cancelled = new AtomicBoolean(false);
        this.running = new AtomicBoolean(false);
        this.lastExecutionTime = 0;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void execute() {
        if (cancelled.get()) {
            return;
        }
        
        if (!running.compareAndSet(false, true)) {
            return; // Already running
        }
        
        try {
            action.run();
            lastExecutionTime = System.currentTimeMillis();
            executionCount.incrementAndGet();
        } finally {
            running.set(false);
        }
    }
    
    @Override
    public long getNextExecutionTime() {
        return nextExecutionTime.get();
    }
    
    @Override
    public boolean isRecurring() {
        return intervalMillis > 0;
    }
    
    @Override
    public long getIntervalMillis() {
        return intervalMillis;
    }
    
    @Override
    public long getExecutionCount() {
        return executionCount.get();
    }
    
    @Override
    public long getMaxExecutions() {
        return maxExecutions;
    }
    
    @Override
    public boolean onExecuted() {
        if (cancelled.get()) {
            return false;
        }
        
        // Check if we've reached max executions
        if (maxExecutions > 0 && executionCount.get() >= maxExecutions) {
            return false;
        }
        
        // For recurring tasks, update next execution time
        if (isRecurring()) {
            nextExecutionTime.set(System.currentTimeMillis() + intervalMillis);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void cancel() {
        cancelled.set(true);
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    public boolean isRunning() {
        return running.get();
    }
    
    public long getLastExecutionTime() {
        return lastExecutionTime;
    }
    
    public LocalDateTime getScheduledDateTime() {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nextExecutionTime.get()),
            ZoneId.systemDefault()
        );
    }
    
    public long getDelay(TimeUnit unit) {
        long delayMillis = nextExecutionTime.get() - System.currentTimeMillis();
        return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public String toString() {
        return String.format("Task[%s: %s, next=%s, count=%d, cancelled=%b]",
            id, name, getScheduledDateTime(), executionCount.get(), cancelled.get());
    }
    
    @Override
    public int compareTo(Task other) {
        return Long.compare(this.getNextExecutionTime(), other.getNextExecutionTime());
    }
}
