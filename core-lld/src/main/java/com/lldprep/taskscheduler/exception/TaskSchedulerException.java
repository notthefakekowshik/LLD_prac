package com.lldprep.taskscheduler.exception;

/**
 * Base exception for task scheduler errors.
 */
public class TaskSchedulerException extends RuntimeException {
    
    public TaskSchedulerException(String message) {
        super(message);
    }
    
    public TaskSchedulerException(String message, Throwable cause) {
        super(message, cause);
    }
}
