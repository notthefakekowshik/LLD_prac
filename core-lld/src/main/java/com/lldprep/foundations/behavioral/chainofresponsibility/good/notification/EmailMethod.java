package com.lldprep.foundations.behavioral.chainofresponsibility.good.notification;

public class EmailMethod extends NotificationMethod {
    public EmailMethod() {
        super("Email");
    }

    @Override
    protected void send(NotificationRequest request) {
        System.out.println("[Email] Sending to user " + request.getUserId() +
            ": " + request.getMessage());
    }
}
