package com.lldprep.threadpool.exception;

public class RejectedExecutionException extends RuntimeException {
    
    public RejectedExecutionException(String message) {
        super(message);
    }
    
    public RejectedExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
