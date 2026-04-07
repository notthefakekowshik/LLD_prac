package com.lldprep.logging.model;

import com.lldprep.logging.LogLevel;

import java.time.Instant;

/**
 * Immutable snapshot of a single log event.
 * 
 * Created once by Logger.log() and passed through the entire handler chain.
 * Immutability guarantees thread-safety — no handler can mutate another's view.
 */
public final class LogRecord {

    private final Instant timestamp;
    private final LogLevel level;
    private final String loggerName;
    private final String message;
    private final Throwable throwable;

    public LogRecord(LogLevel level, String loggerName, String message, Throwable throwable) {
        this.timestamp  = Instant.now();
        this.level      = level;
        this.loggerName = loggerName;
        this.message    = message;
        this.throwable  = throwable;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getMessage() {
        return message;
    }

    /**
     * May be null — only present when an exception was logged.
     */
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String toString() {
        return "LogRecord{level=" + level + ", logger=" + loggerName + ", message=" + message + "}";
    }
}
