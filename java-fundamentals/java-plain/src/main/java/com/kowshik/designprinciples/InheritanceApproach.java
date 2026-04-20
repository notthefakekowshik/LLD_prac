package com.kowshik.designprinciples;

/**
 * ============================================================
 *  APPROACH 1 — INHERITANCE (The "Static" / Tightly-Coupled Way)
 * ============================================================
 *
 *  Core idea:
 *    A concrete class (EmailNotification) IS-A Notification.
 *    The caller decides at compile-time which subclass to use
 *    and that decision is baked into the binary.
 *
 *  The famous one-liner from the problem statement:
 *
 *      Notification note = new EmailNotification();
 *      note.send("Hello");
 *
 *  Notice: "new EmailNotification()" is written in stone.
 *  You cannot swap EmailNotification for SMSNotification at
 *  runtime without recompiling.
 *
 *  Problems this creates
 *  ─────────────────────
 *  1. Open/Closed violation: Adding PushNotification means you
 *     must open Notification and add new subclass logic.
 *  2. Rigid coupling: the caller (Main / a service) must know
 *     the concrete class at compile-time.
 *  3. Cannot switch channels at runtime (e.g., fallback from
 *     Email → SMS if SMTP is down).
 *  4. Combinatorial explosion: if you later add Urgency levels
 *     (HIGH, LOW) ×  channels (EMAIL, SMS, PUSH) = 6 subclasses.
 */

// ── Abstract base class ──────────────────────────────────────
abstract class Notification {
    // Template: subclasses MUST implement send()
    public abstract void send(String message);
}

// ── Concrete subclass 1 ──────────────────────────────────────
class EmailNotification extends Notification {
    @Override
    public void send(String message) {
        System.out.println("[EMAIL] Sending: " + message);
        // imagine SMTP call here
    }
}

// ── Concrete subclass 2 ──────────────────────────────────────
class SMSNotification extends Notification {
    @Override
    public void send(String message) {
        System.out.println("[SMS] Sending:   " + message);
        // imagine Twilio API call here
    }
}

// ── Concrete subclass 3 ──────────────────────────────────────
class PushNotification extends Notification {
    @Override
    public void send(String message) {
        System.out.println("[PUSH] Sending:  " + message);
        // imagine Firebase call here
    }
}

/**
 * Demo runner for the INHERITANCE approach.
 *
 * KEY OBSERVATION: The type (EmailNotification vs SMSNotification)
 * is chosen at the LINE WHERE `new` IS WRITTEN.
 * Once this line is compiled, the channel is frozen.
 */
class InheritanceDemo {

    public static void run() {
        System.out.println("=== Inheritance Approach ===");

        // LOCKED at compile-time — the `new EmailNotification()` literal
        // is what makes this "static".  YOU CANNOT change the channel
        // without editing and recompiling this line.
        Notification note = new EmailNotification();
        note.send("Your order has shipped!");           // always Email

        // To use SMS, you write a completely different line:
        Notification sms = new SMSNotification();
        sms.send("Your OTP is 482910");                // always SMS

        // There is no way to say:
        //   "Based on user preference at runtime, pick the right channel"
        //   without an if-else block in the caller — which pushes
        //   channel-selection logic into business code.
        System.out.println();
    }
}
