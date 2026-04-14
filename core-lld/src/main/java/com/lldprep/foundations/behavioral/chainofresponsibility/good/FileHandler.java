package com.lldprep.foundations.behavioral.chainofresponsibility.good;

/** Handles WARN and above — simulates writing to a log file. */
public class FileHandler extends LogHandler {

    public FileHandler() { super(LogLevel.WARN); }

    @Override
    protected void write(LogLevel level, String message) {
        System.out.println("  [FILE   ][" + level + "] Appending to app.log: " + message);
    }
}
