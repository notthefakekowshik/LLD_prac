package com.lldprep.systems.logging.handler;

import com.lldprep.systems.logging.LogLevel;
import com.lldprep.systems.logging.model.LogRecord;

/**
 * Contract for every node in the logging handler chain.
 * 
 * Pattern: Chain of Responsibility
 * Why: The Logger doesn't need to know how many handlers are attached or
 * what each one does. It simply calls handle() on the chain head.
 * Each handler independently decides: process this record? pass it on?
 */
public interface Handler {

    /**
     * Processes the log record and optionally forwards it to the next handler.
     *
     * @param record the log event to handle
     */
    void handle(LogRecord record);

    /**
     * Appends a handler to the end of this handler's chain.
     * Returns the handler passed in to allow fluent chaining:
     *   consoleHandler.setNext(fileHandler).setNext(slackHandler);
     *
     * @param next the handler to delegate to after this one
     * @return the handler that was set as next (enables fluent chaining)
     */
    Handler setNext(Handler next);

    /**
     * Sets the minimum level this handler will process.
     * Records below this level are skipped (but still forwarded).
     */
    void setLevel(LogLevel level);
}
