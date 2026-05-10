package com.lldprep.foundations.behavioral.chainofresponsibility.good.notification;

public abstract class NotificationMethod {
    protected NotificationMethod next;
    protected final String channelName;

    public NotificationMethod(String channelName) {
        this.channelName = channelName;
    }

    public void setNext(NotificationMethod next) {
        this.next = next;
    }

    // KEY: This is "pass-and-process" - ALL handlers run, no stopping
    public final void sendNotification(NotificationRequest request) {
        // Process this notification
        send(request);

        // ALWAYS pass to next handler (even if this one "failed")
        if (next != null) {
            next.sendNotification(request);
        }
    }

    protected abstract void send(NotificationRequest request);
}
