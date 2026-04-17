package com.lldprep.systems.taskscheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * Adapter that wraps a Task to provide ScheduledTask interface.
 * 
 * Design Patterns:
 * - Adapter Pattern: Adapts Task to ScheduledTask interface
 * - Proxy Pattern: Controls access to underlying task
 */
public class DefaultScheduledTask implements ScheduledTask {
    
    private final Task task;
    
    public DefaultScheduledTask(Task task) {
        this.task = task;
    }
    
    @Override
    public String getId() {
        return task.getId();
    }
    
    @Override
    public String getName() {
        return task.getName();
    }
    
    @Override
    public boolean cancel() {
        if (task.isCancelled() || isDone()) {
            return false;
        }
        task.cancel();
        return true;
    }
    
    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }
    
    @Override
    public boolean isDone() {
        if (task.isCancelled()) {
            return true;
        }
        if (!task.isRecurring() && task.getExecutionCount() > 0) {
            return true;
        }
        if (task.getMaxExecutions() > 0 && task.getExecutionCount() >= task.getMaxExecutions()) {
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isRunning() {
        if (task instanceof DefaultTask) {
            return ((DefaultTask) task).isRunning();
        }
        return false;
    }
    
    @Override
    public LocalDateTime getScheduledTime() {
        long epochMillis = task.getNextExecutionTime();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        if (task instanceof DefaultTask) {
            return ((DefaultTask) task).getDelay(unit);
        }
        long delayMillis = task.getNextExecutionTime() - System.currentTimeMillis();
        return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean isRecurring() {
        return task.isRecurring();
    }
    
    @Override
    public long getExecutionCount() {
        return task.getExecutionCount();
    }
    
    @Override
    public LocalDateTime getLastExecutionTime() {
        if (task instanceof DefaultTask) {
            long lastExec = ((DefaultTask) task).getLastExecutionTime();
            if (lastExec == 0) {
                return null;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastExec), ZoneId.systemDefault());
        }
        return null;
    }
    
    @Override
    public LocalDateTime getNextExecutionTime() {
        if (!task.isRecurring()) {
            return null;
        }
        return getScheduledTime();
    }
    
    /**
     * Returns the underlying task (package-private for scheduler use).
     */
    Task getTask() {
        return task;
    }
    
    @Override
    public String toString() {
        return String.format("ScheduledTask[%s: %s, scheduled=%s, executions=%d, recurring=%b, done=%b]",
            getId(), getName(), getScheduledTime(), getExecutionCount(), isRecurring(), isDone());
    }
}
