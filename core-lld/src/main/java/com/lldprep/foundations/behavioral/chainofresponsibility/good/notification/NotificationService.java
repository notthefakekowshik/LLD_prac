package com.lldprep.foundations.behavioral.chainofresponsibility.good.notification;

/**
 * Facade that encapsulates the notification chain construction.
 * Client just calls sendNotification() - doesn't know about chain linking.
 */
public class NotificationService {

    private NotificationMethod chainHead;

    /**
     * Build the chain from given methods. Links them in order.
     * Example: buildChain(new EmailMethod(), new SMSMethod(), new PushMethod())
     * Creates: Email → SMS → Push
     */
    @SafeVarargs
    public final void buildChain(NotificationMethod... methods) {
        if (methods.length == 0) {
            throw new IllegalArgumentException("At least one notification method required");
        }

        // Link each method to the next 
        for (int i = 0; i < methods.length - 1; i++) {
            methods[i].setNext(methods[i + 1]);
        }

        this.chainHead = methods[0];
    }

    public void sendNotification(NotificationRequest request) {
        if (chainHead == null) {
            throw new IllegalStateException("Notification chain not built. Call buildChain() first.");
        }
        chainHead.sendNotification(request);
    }
}
