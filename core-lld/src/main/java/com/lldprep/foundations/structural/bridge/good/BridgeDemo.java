package com.lldprep.foundations.structural.bridge.good;

/**
 * Bridge Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * When a class varies along two independent dimensions (e.g., message type × delivery channel),
 * pure inheritance causes a Cartesian product explosion: M types × N channels = M×N classes.
 * Bridge separates the two dimensions so they can vary independently: M + N classes total.
 *
 * <p><b>How it works:</b><br>
 * - {@code Message} (Abstraction) — controls message formatting; holds a reference to a sender.<br>
 * - {@code MessageSender} (Implementor) — controls delivery channel.<br>
 * - They are composed, not inherited — this is the "bridge" between the two hierarchies.<br>
 * - Adding a new message type → one new {@code Message} subclass.<br>
 * - Adding a new channel → one new {@code MessageSender} implementation.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>A class has two or more orthogonal dimensions that must vary independently.</li>
 *   <li>You want to avoid a subclass explosion from combining dimensions.</li>
 *   <li>You want to switch implementations at runtime (e.g., swap Email for SMS).</li>
 * </ul>
 *
 * <p><b>Bridge vs Strategy:</b><br>
 * - {@code Strategy} swaps algorithms within one class — single dimension.<br>
 * - {@code Bridge} decouples two entire class hierarchies — two dimensions.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>2×2 matrix (SimpleMessage / UrgentMessage) × (Email / SMS) — 4 combinations, 2+2 classes</li>
 *   <li>Extending with a 3rd channel (Slack) — zero changes to Message hierarchy</li>
 *   <li>Runtime sender swap on the same message object</li>
 * </ol>
 */
public class BridgeDemo {

    public static void main(String[] args) {
        demo1_TwoDimensionMatrix();
        demo2_AddNewChannel();
        demo3_RuntimeSenderSwap();
    }

    // -------------------------------------------------------------------------

    private static void demo1_TwoDimensionMatrix() {
        section("Demo 1: 2×2 matrix — SimpleMessage/UrgentMessage × Email/SMS");

        MessageSender email = new EmailSender();
        MessageSender sms = new SmsSender();

        new SimpleMessage(email).send("alice@company.com", "Deployment complete.");
        new SimpleMessage(sms).send("+1-555-0100", "Deployment complete.");
        new UrgentMessage(email).send("ops@company.com", "Database connection lost.");
        new UrgentMessage(sms).send("+1-555-0199", "Database connection lost.");
    }

    private static void demo2_AddNewChannel() {
        section("Demo 2: Add Slack channel — zero changes to Message classes");

        MessageSender slack = new SlackSender("alerts");

        // Both existing message types work with the new sender immediately
        new SimpleMessage(slack).send("oncall-engineer", "Scheduled maintenance at 02:00.");
        new UrgentMessage(slack).send("oncall-engineer", "Payment service down.");
    }

    private static void demo3_RuntimeSenderSwap() {
        section("Demo 3: Runtime sender swap on the same message type");

        // Imagine a notification service that picks the channel based on user preference
        String userPreference = "sms"; // could come from config / DB

        MessageSender sender = userPreference.equals("sms") ? new SmsSender() : new EmailSender();
        Message notification = new UrgentMessage(sender);
        notification.send("+1-555-0101", "Your OTP is 482910.");
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
