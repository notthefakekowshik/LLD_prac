package com.lldprep.foundations.behavioral.chainofresponsibility.good;

/** Handles DEBUG and above — prints to console. */
public class ConsoleHandler extends LogHandler {

    public ConsoleHandler() { super(LogLevel.DEBUG); }

    @Override
    protected void write(LogLevel level, String message) {
        System.out.println("  [CONSOLE][" + level + "] " + message);
    }
}
