package com.lldprep.foundations.behavioral.chainofresponsibility.good;

/** Handles ERROR and above — simulates sending an email alert to on-call. */
public class EmailAlertHandler extends LogHandler {

    private final String recipient;

    public EmailAlertHandler(String recipient) {
        super(LogLevel.ERROR);
        this.recipient = recipient;
    }

    @Override
    protected void write(LogLevel level, String message) {
        System.out.println("  [EMAIL  ][" + level + "] Alerting " + recipient + ": " + message);
    }
}
