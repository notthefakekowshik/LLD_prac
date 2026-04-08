package com.lldprep.logging.exception;

/**
 * Unchecked exception thrown when the logging framework encounters an
 * unrecoverable I/O error (e.g., cannot open or write to the log file).
 * 
 * Unchecked (RuntimeException) because callers should not be forced to
 * wrap every log call in a try-catch — logging failures should be loud
 * at startup (bad file path) and quietly degraded at runtime (write error
 * is caught inside FileHandler.write() and printed to stderr instead).
 */
public class LoggerException extends RuntimeException {

    public LoggerException(String message) {
        super(message);
    }

    public LoggerException(String message, Throwable cause) {
        super(message, cause);
    }
}
