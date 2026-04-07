package com.lldprep.logging.handler;

import com.lldprep.logging.LogLevel;
import com.lldprep.logging.formatter.Formatter;
import com.lldprep.logging.model.LogRecord;

/**
 * Writes formatted log records to the console.
 * 
 * ERROR and FATAL go to System.err; all others go to System.out.
 * This mirrors the convention used by most logging frameworks.
 * 
 * Thread-safety: System.out/err are internally synchronized in the JVM.
 */
public class ConsoleHandler extends AbstractHandler {

    public ConsoleHandler() {
        super(LogLevel.DEBUG, null);
    }

    public ConsoleHandler(LogLevel level) {
        super(level, null);
    }

    public ConsoleHandler(LogLevel level, Formatter formatter) {
        super(level, formatter);
    }

    @Override
    protected void write(String formatted, LogRecord record) {
        if (record.getLevel().isAtLeast(LogLevel.ERROR)) {
            System.err.println(formatted);
        } else {
            System.out.println(formatted);
        }
    }
}
