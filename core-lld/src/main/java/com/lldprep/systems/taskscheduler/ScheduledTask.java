package com.lldprep.systems.taskscheduler;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Handle returned when a task is scheduled.
 * 
 * Allows:
 * - Checking task status
 * - Cancelling the task
 * - Getting execution information
 * 
 * This is the client-facing interface for interacting with scheduled tasks.
 */
public interface ScheduledTask {
    
    /**
     * Returns the unique ID of this scheduled task.
     * 
     * @return Task ID
     */
    String getId();
    
    /**
     * Returns the name of this task.
     * 
     * @return Task name
     */
    String getName();
    
    /**
     * Cancels this task if it hasn't executed yet.
     * Running tasks are not interrupted.
     * 
     * @return true if task was cancelled, false if already executed or cancelled
     */
    boolean cancel();
    
    /**
     * Checks if this task has been cancelled.
     * 
     * @return true if cancelled
     */
    boolean isCancelled();
    
    /**
     * Checks if this task has completed execution.
     * For one-time tasks: true after execution.
     * For recurring: true only if cancelled or max executions reached.
     * 
     * @return true if task is done
     */
    boolean isDone();
    
    /**
     * Checks if this task is currently running.
     * 
     * @return true if executing right now
     */
    boolean isRunning();
    
    /**
     * Returns the time when this task is/was scheduled to run.
     * For recurring tasks, returns next scheduled time.
     * 
     * @return Scheduled execution time
     */
    LocalDateTime getScheduledTime();
    
    /**
     * Returns the delay until this task executes.
     * 
     * @param unit Time unit for result
     * @return Delay in specified unit (negative if already past)
     */
    long getDelay(TimeUnit unit);
    
    /**
     * Returns true if this is a recurring task.
     * 
     * @return true if recurring
     */
    boolean isRecurring();
    
    /**
     * Returns the number of times this task has executed.
     * 
     * @return Execution count
     */
    long getExecutionCount();
    
    /**
     * Returns the last execution time.
     * Null if never executed.
     * 
     * @return Last execution time or null
     */
    LocalDateTime getLastExecutionTime();
    
    /**
     * Returns the next execution time for recurring tasks.
     * Null for one-time tasks.
     * 
     * @return Next execution time or null
     */
    LocalDateTime getNextExecutionTime();
}
