package com.lldprep.foundations.creational.factory.bad;

/**
 * BAD: Direct instantiation with if-else/switch logic everywhere.
 * 
 * PROBLEMS:
 * 1. Tight coupling - client knows all concrete classes
 * 2. Violates Open/Closed Principle - add new type = modify everywhere
 * 3. Duplicated instantiation logic scattered in code
 * 4. Hard to test - cannot mock easily
 * 5. No central place to change/extend object creation
 */
public class NotificationBad {
    
    // Client code scattered throughout application
    public void sendNotification(String type, String message) {
        // VIOLATION: Direct instantiation with if-else
        if (type.equals("EMAIL")) {
            EmailNotification email = new EmailNotification();
            email.send(message);
        } else if (type.equals("SMS")) {
            SMSNotification sms = new SMSNotification();
            sms.send(message);
        } else if (type.equals("PUSH")) {
            PushNotification push = new PushNotification();
            push.send(message);
        }
        // Add new type? Modify every if-else block in codebase!
    }
    
    // Dummy notification classes
    static class EmailNotification {
        void send(String msg) { System.out.println("Email: " + msg); }
    }
    static class SMSNotification {
        void send(String msg) { System.out.println("SMS: " + msg); }
    }
    static class PushNotification {
        void send(String msg) { System.out.println("Push: " + msg); }
    }
}
