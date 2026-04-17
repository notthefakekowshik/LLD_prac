package com.lldprep.systems.logging.formatter;

import com.lldprep.systems.logging.model.LogRecord;

/**
 * Strategy interface for converting a LogRecord into a printable string.
 * 
 * Pattern: Strategy
 * Why: Handlers (ConsoleHandler, FileHandler) are closed for modification.
 * Swapping Plain ↔ JSON means injecting a different Formatter at construction
 * time — no handler code changes.
 */
public interface Formatter {

    /**
     * Converts the given log record into a formatted string ready for output.
     *
     * @param record the log event to format
     * @return formatted string representation
     */
    String format(LogRecord record);
}
