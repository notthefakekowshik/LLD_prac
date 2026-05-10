package com.lldprep.foundations.behavioral.chainofresponsibility.good.notification;

public class PushMethod extends NotificationMethod {
    public PushMethod() {
        super("Push");
    }

    @Override
    protected void send(NotificationRequest request) {
        System.out.println("[Push] Notifying user " + request.getUserId() +
            " with badge + sound");
    }
}
