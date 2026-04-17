package com.lldprep.systems.taskscheduler;

/**
 * Represents a unit of work that can be scheduled and executed.
 * 
 * This is the core abstraction for tasks in the scheduler.
 * Tasks are comparable based on their next execution time to support
 * priority ordering in the scheduling queue.
 * 
 * Design Patterns:
 * - Comparable: Natural ordering for priority queue
 * - Builder: Task construction via TaskBuilder
 */
public interface Task extends Comparable<Task> {
    
    /**
     * Returns the unique identifier for this task.
     * 
     * @return Task ID (non-null, unique within scheduler)
     */
    String getId();
    
    /**
     * Returns the human-readable name of this task.
     * 
     * @return Task name (for logging/debugging)
     */
    String getName();
    
    /**
     * The actual work to be executed.
     * Called by the scheduler when the task's time comes.
     */
    void execute();
    
    /**
     * Returns the next execution time in epoch milliseconds.
     * 
     * @return Time when this task should next run
     */
    long getNextExecutionTime();
    
    /**
     * Checks if this is a recurring task.
     * 
     * @return true if task should run multiple times
     */
    boolean isRecurring();
    
    /**
     * For recurring tasks, returns the delay between executions.
     * Returns 0 for one-time tasks.
     * 
     * @return Interval in milliseconds between executions
     */
    long getIntervalMillis();
    
    /**
     * Returns the number of times this task has been executed.
     * 
     * @return Execution count
     */
    long getExecutionCount();
    
    /**
     * Returns the maximum number of times this task can run.
     * 0 or negative means unlimited.
     * 
     * @return Max executions, or 0 for unlimited
     */
    long getMaxExecutions();
    
    /**
     * Called by the scheduler after each execution.
     * Updates internal state (execution count, next time, etc.).
     * 
     * @return true if task should be rescheduled, false if complete
     */
    boolean onExecuted();
    
    /**
     * Cancels this task. After cancellation, the task will not be executed.
     */
    void cancel();
    
    /**
     * Checks if this task has been cancelled.
     * 
     * @return true if cancelled
     */
    boolean isCancelled();
    
    /**
     * Natural ordering: tasks with earlier execution times come first.
     * Used by the priority queue for scheduling.
     * 
     * @param other the task to compare against
     * @return negative if this task executes before other
     */
    @Override
    default int compareTo(Task other) {
        return Long.compare(this.getNextExecutionTime(), other.getNextExecutionTime());
    }
}
