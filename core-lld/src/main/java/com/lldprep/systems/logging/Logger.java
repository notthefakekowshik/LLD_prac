package com.lldprep.systems.logging;

import com.lldprep.systems.logging.handler.Handler;
import com.lldprep.systems.logging.model.LogRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for all logging operations.
 *
 * Responsibilities:
 *   1. Gate records at the Logger's own minimum level (avoids allocating
 *      a LogRecord for levels that will definitely be dropped).
 *   2. Create an immutable LogRecord and pass it to every attached Handler.
 *
 * Handlers are stored in insertion order. Each handler manages its own
 * threshold and forwards to the next handler in its own chain — the Logger
 * simply iterates its top-level handler list.
 *
 * Thread-safety:
 *   - addHandler() is synchronized to prevent concurrent modification of
 *     the handlers list during log().
 *   - log() synchronizes on the handlers list snapshot to allow concurrent
 *     log calls while addHandler() is safe.
 *
 * Obtain instances via LoggerFactory — do not call new Logger() directly.
 */
public class Logger {

    private final String name;
    private volatile LogLevel level;
    private final List<Handler> handlers = new ArrayList<>();

    public Logger(String name, LogLevel level) {
        this.name  = name;
        this.level = level;
    }

    // ─── Public logging API ────────────────────────────────────────────────

    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }

    public void fatal(String message) {
        log(LogLevel.FATAL, message, null);
    }

    public void fatal(String message, Throwable throwable) {
        log(LogLevel.FATAL, message, throwable);
    }

    // ─── Core dispatch ─────────────────────────────────────────────────────

    private void log(LogLevel recordLevel, String message, Throwable throwable) {
        // Fast path: drop below Logger threshold without allocating a LogRecord.
        if (!recordLevel.isAtLeast(this.level)) {
            return;
        }

        LogRecord record = new LogRecord(recordLevel, name, message, throwable);

        List<Handler> snapshot;
        synchronized (handlers) {
            snapshot = new ArrayList<>(handlers);
        }
        for (Handler handler : snapshot) {
            handler.handle(record);
        }
    }

    // ─── Configuration ─────────────────────────────────────────────────────

    /**
     * Adds a handler to this logger.
     * Handlers are called in insertion order; each handler manages its own
     * internal chain independently.
     */
    public synchronized void addHandler(Handler handler) {
        handlers.add(handler);
    }

    /**
     * Changes the logger's minimum level at runtime.
     * Declared volatile so changes are visible across threads immediately.
     */
    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Logger{name=" + name + ", level=" + level + ", handlers=" + handlers.size() + "}";
    }
}
