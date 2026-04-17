package com.lldprep.systems.taskscheduler;

/**
 * Immutable snapshot of scheduler performance metrics.
 * 
 * Used for monitoring and observability.
 * 
 * @param tasksSubmitted Total tasks ever submitted
 * @param tasksExecuted Total tasks successfully executed
 * @param tasksCancelled Total tasks cancelled
 * @param pendingTasks Currently scheduled tasks
 * @param workerPoolSize Number of worker threads
 */
public record SchedulerMetrics(
    long tasksSubmitted,
    long tasksExecuted,
    long tasksCancelled,
    int pendingTasks,
    int workerPoolSize
) {
    
    /**
     * Returns the number of tasks currently in-flight.
     * (Submitted but not yet executed or cancelled)
     */
    public long tasksInFlight() {
        return tasksSubmitted - tasksExecuted - tasksCancelled;
    }
    
    /**
     * Returns task execution success rate (0.0 to 1.0).
     * Returns 0 if no tasks executed.
     */
    public double successRate() {
        if (tasksExecuted + tasksCancelled == 0) {
            return 0.0;
        }
        return (double) tasksExecuted / (tasksExecuted + tasksCancelled);
    }
    
    @Override
    public String toString() {
        return String.format(
            "SchedulerMetrics{submitted=%d, executed=%d, cancelled=%d, pending=%d, poolSize=%d, inFlight=%d, successRate=%.2f%%}",
            tasksSubmitted, tasksExecuted, tasksCancelled, pendingTasks, 
            workerPoolSize, tasksInFlight(), successRate() * 100);
    }
}
