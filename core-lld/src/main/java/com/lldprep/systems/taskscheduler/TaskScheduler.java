package com.lldprep.taskscheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Core interface for task scheduling.
 * 
 * A TaskScheduler manages the execution of tasks at specified times or intervals.
 * It supports:
 * - One-time delayed execution
 * - Recurring execution (fixed rate, fixed delay)
 * - Absolute time scheduling
 * - Task cancellation
 * 
 * Design Patterns:
 * - Strategy: Different scheduling strategies can be plugged in
 * - Observer: Task lifecycle callbacks
 * - Factory: Task creation via schedule methods
 * 
 * Thread Safety: All implementations must be thread-safe.
 */
public interface TaskScheduler {
    
    /**
     * Schedules a one-time task to run after the specified delay.
     * 
     * @param name Task name for identification
     * @param task The runnable to execute
     * @param delay Delay before execution
     * @param unit Time unit for delay
     * @return ScheduledTask handle for cancellation
     */
    ScheduledTask scheduleOnce(String name, Runnable task, long delay, TimeUnit unit);
    
    /**
     * Schedules a one-time task to run at the specified absolute time.
     * If the time is in the past, the task executes immediately.
     * 
     * @param name Task name for identification
     * @param task The runnable to execute
     * @param dateTime When to execute the task
     * @return ScheduledTask handle for cancellation
     */
    ScheduledTask scheduleAt(String name, Runnable task, LocalDateTime dateTime);
    
    /**
     * Schedules a one-time task to run at the specified time today.
     * If time has passed, schedules for tomorrow.
     * 
     * @param name Task name for identification
     * @param task The runnable to execute
     * @param time Time of day to execute
     * @return ScheduledTask handle for cancellation
     */
    ScheduledTask scheduleAt(String name, Runnable task, LocalTime time);
    
    /**
     * Schedules a recurring task with fixed rate execution.
     * Task runs every 'period' time units, regardless of execution time.
     * 
     * @param name Task name for identification
     * @param task The runnable to execute
     * @param initialDelay Initial delay before first execution
     * @param period Time between executions
     * @param unit Time unit
     * @return ScheduledTask handle for cancellation
     */
    ScheduledTask scheduleAtFixedRate(String name, Runnable task, 
                                       long initialDelay, long period, TimeUnit unit);
    
    /**
     * Schedules a recurring task with fixed delay execution.
     * Next execution starts 'delay' time units after previous completion.
     * 
     * @param name Task name for identification
     * @param task The runnable to execute
     * @param initialDelay Initial delay before first execution
     * @param delay Delay between end of one execution and start of next
     * @param unit Time unit
     * @return ScheduledTask handle for cancellation
     */
    ScheduledTask scheduleWithFixedDelay(String name, Runnable task,
                                          long initialDelay, long delay, TimeUnit unit);
    
    /**
     * Schedules a recurring task using a Duration-based interval.
     * 
     * @param name Task name for identification
     * @param task The runnable to execute
     * @param interval Duration between executions
     * @return ScheduledTask handle for cancellation
     */
    ScheduledTask scheduleRecurring(String name, Runnable task, Duration interval);
    
    /**
     * Submits a task for immediate execution.
     * Equivalent to scheduleOnce with 0 delay.
     * 
     * @param name Task name for identification
     * @param task The runnable to execute
     * @return ScheduledTask handle
     */
    ScheduledTask submit(String name, Runnable task);
    
    /**
     * Cancels a scheduled task.
     * 
     * @param taskId ID of task to cancel
     * @return true if task was found and cancelled
     */
    boolean cancel(String taskId);
    
    /**
     * Cancels all scheduled tasks.
     */
    void cancelAll();
    
    /**
     * Returns the number of tasks currently scheduled.
     * 
     * @return Count of pending tasks
     */
    int getScheduledTaskCount();
    
    /**
     * Returns a list of all scheduled task IDs.
     * 
     * @return List of task IDs
     */
    List<String> getScheduledTaskIds();
    
    /**
     * Shuts down the scheduler.
     * Stops accepting new tasks and attempts to cancel pending ones.
     * 
     * @param timeout Maximum time to wait for running tasks
     * @param unit Time unit for timeout
     * @return true if shutdown completed within timeout
     */
    boolean shutdown(long timeout, TimeUnit unit);
    
    /**
     * Checks if the scheduler has been shut down.
     * 
     * @return true if shut down
     */
    boolean isShutdown();
    
    /**
     * Returns scheduler statistics.
     * 
     * @return SchedulerMetrics with performance data
     */
    SchedulerMetrics getMetrics();
}
