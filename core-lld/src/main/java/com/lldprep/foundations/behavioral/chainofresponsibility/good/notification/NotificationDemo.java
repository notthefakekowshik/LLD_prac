package com.lldprep.foundations.behavioral.chainofresponsibility.good.notification;

/**
 * SIMPLEST pass-and-process Chain of Responsibility.
 *
 * KEY POINT: ALL handlers run. No early termination.
 *
 * Real-world: Send notification via ALL channels (Email + SMS + Push).
 * Even if email fails, we still try SMS and Push.
 */
public class NotificationDemo {

    public static void main(String[] args) {
        // Service encapsulates chain construction - client just provides methods
        NotificationService notificationService = new NotificationService();

        // Build chain: Email → SMS → Push (service handles the linking)
        notificationService.buildChain(
            new EmailMethod(),
            new SMSMethod(),
            new PushMethod()
        );

        // Send one notification - ALL three methods will process it
        System.out.println("=== Sending notification (ALL methods run) ===\n");

        NotificationRequest request = new NotificationRequest(
            "user123",
            "Your order #12345 has been shipped and will arrive tomorrow",
            "NORMAL"
        );

        // This runs Email, then SMS, then Push - always all three
        notificationService.sendNotification(request);

        System.out.println("\n=== Done - all channels notified ===");
    }
}
