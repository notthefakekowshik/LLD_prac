package com.lldprep.foundations.behavioral.chainofresponsibility.good.notification;

public class SMSMethod extends NotificationMethod {
    public SMSMethod() {
        super("SMS");
    }

    @Override
    protected void send(NotificationRequest request) {
        System.out.println("[SMS] Texting user " + request.getUserId() +
            ": " + request.getMessage().substring(0,
                Math.min(50, request.getMessage().length())) + "...");
    }
}
