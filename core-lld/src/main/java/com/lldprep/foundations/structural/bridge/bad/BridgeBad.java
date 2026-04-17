package com.lldprep.foundations.structural.bridge.bad;

/**
 * BAD: Class explosion from combining two independent dimensions via inheritance.
 *
 * Dimensions:
 *   Message type:  Simple | Urgent
 *   Sender channel: Email | SMS
 *
 * With inheritance you need a class for every combination:
 *   SimpleEmailMessage, SimpleSmSMessage, UrgentEmailMessage, UrgentSmsMessage = 4 classes
 *
 * Adding a new message type (e.g., Scheduled) → 2 more classes.
 * Adding a new channel (e.g., Push) → 2 more classes per existing type.
 * M types × N channels = M×N classes. This is the "Cartesian product" explosion.
 */
public class BridgeBad {

    public static void main(String[] args) {
        new SimpleEmailMessage("Server is up").send("ops@company.com");
        new UrgentSmsMessage("PROD DOWN!").send("+1-555-0100");
    }
}

class SimpleEmailMessage {
    private final String body;
    SimpleEmailMessage(String body) { this.body = body; }

    public void send(String recipient) {
        System.out.printf("  [Email] To: %s | Body: %s%n", recipient, body);
    }
}

class UrgentSmsMessage {
    private final String body;
    UrgentSmsMessage(String body) { this.body = body; }

    public void send(String recipient) {
        System.out.printf("  [SMS] To: %s | URGENT: %s%n", recipient, body.toUpperCase());
    }
}

// Adding SlackMessage or ScheduledMessage requires yet more subclasses.
// Every new dimension multiplies the class count.
