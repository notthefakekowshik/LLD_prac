package com.lldprep.foundations.behavioral.chainofresponsibility.good;

/**
 * Abstract handler — defines the chain link structure.
 *
 * Key design:
 * - Each handler has a minimum level it handles.
 * - If the message level is below its threshold, it passes up to the next handler.
 * - {@code setNext()} returns the next handler to allow fluent chain building:
 *   {@code debug.setNext(info).setNext(warn).setNext(error)}
 */
public abstract class LogHandler {

    private LogHandler next;
    private final LogLevel level;

    protected LogHandler(LogLevel level) {
        this.level = level;
    }

    /** Fluent setter — enables chaining: h1.setNext(h2).setNext(h3) */
    public LogHandler setNext(LogHandler next) {
        this.next = next;
        return next;
    }

    /** Entry point — handle if level matches, otherwise pass along the chain. */
    public final void handle(LogLevel messageLevel, String message) {
        if (messageLevel.value >= this.level.value) {
            write(messageLevel, message);
        }
        if (next != null) {
            next.handle(messageLevel, message);
        }
    }

    /** Concrete handlers implement this to perform the actual handling. */
    protected abstract void write(LogLevel level, String message);
}
