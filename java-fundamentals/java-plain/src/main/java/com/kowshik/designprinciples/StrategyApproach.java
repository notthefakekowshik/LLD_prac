package com.kowshik.designprinciples;

/**
 * ============================================================
 *  APPROACH 2 — STRATEGY PATTERN (The "Flexible" / Abstraction Way)
 * ============================================================
 *
 *  Core idea:
 *    Instead of inheriting a channel, we *inject* it.
 *    NotificationService doesn't care HOW the message is sent —
 *    it delegates to whatever strategy is currently set.
 *
 *  This is "programming to an interface, not an implementation"
 *  — one of the most fundamental OOP mantras.
 *
 *  What changes compared to Inheritance?
 *  ──────────────────────────────────────
 *  ┌────────────────────┬──────────────────────────────────────┐
 *  │ Inheritance        │ Strategy Pattern                     │
 *  ├────────────────────┼──────────────────────────────────────┤
 *  │ Channel chosen at  │ Channel chosen at RUNTIME            │
 *  │ compile-time       │ (from DB, config, user pref, etc.)   │
 *  ├────────────────────┼──────────────────────────────────────┤
 *  │ Caller knows the   │ Caller only knows the interface      │
 *  │ concrete class     │ (NotificationStrategy)               │
 *  ├────────────────────┼──────────────────────────────────────┤
 *  │ Adding a channel   │ Just add a new Strategy class        │
 *  │ may break existing │ — zero changes to existing code.     │
 *  │ code (OCP broken)  │ (OCP satisfied ✓)                   │
 *  ├────────────────────┼──────────────────────────────────────┤
 *  │ Testing requires   │ Easily mock the strategy             │
 *  │ real channels      │ (SRP + DIP satisfied ✓)             │
 *  └────────────────────┴──────────────────────────────────────┘
 */

// ── The Strategy contract (the "abstraction" everyone depends on) ─
interface NotificationStrategy {
    /**
     * Send a notification message via whatever channel this
     * strategy represents.  The service knows nothing beyond
     * this method signature.
     */
    void send(String message);
}

// ── Concrete Strategy 1: Email ────────────────────────────────
class EmailStrategy implements NotificationStrategy {
    private final String smtpServer;

    EmailStrategy(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    @Override
    public void send(String message) {
        System.out.println("[EMAIL via " + smtpServer + "] " + message);
    }
}

// ── Concrete Strategy 2: SMS ──────────────────────────────────
class SMSStrategy implements NotificationStrategy {
    private final String apiKey;

    SMSStrategy(String apiKey) {
        this.apiKey = apiKey;
        // apiKey redacted in real output — just showing constructor injection
    }

    @Override
    public void send(String message) {
        System.out.println("[SMS  via Twilio   ] " + message);
    }
}

// ── Concrete Strategy 3: Push ─────────────────────────────────
class PushStrategy implements NotificationStrategy {
    @Override
    public void send(String message) {
        System.out.println("[PUSH via Firebase ] " + message);
    }
}

// ── Concrete Strategy 4: Fallback chain (demonstrates composability) ─
/**
 * FallbackStrategy is itself a strategy that wraps two others.
 * It tries the primary channel first; if that throws, it falls
 * back to the secondary — without the caller knowing.
 *
 * This is impossible to express cleanly with pure inheritance.
 */
class FallbackStrategy implements NotificationStrategy {
    private final NotificationStrategy primary;
    private final NotificationStrategy fallback;

    FallbackStrategy(NotificationStrategy primary, NotificationStrategy fallback) {
        this.primary  = primary;
        this.fallback = fallback;
    }

    @Override
    public void send(String message) {
        try {
            primary.send(message);
        } catch (Exception e) {
            System.out.println("[FALLBACK] Primary failed (" + e.getMessage() + "), trying fallback...");
            fallback.send(message);
        }
    }
}

// ── A strategy that simulates a broken channel ───────────────
class BrokenEmailStrategy implements NotificationStrategy {
    @Override
    public void send(String message) {
        throw new RuntimeException("SMTP server unreachable");
    }
}

// ── The Service: only depends on the interface ────────────────
/**
 * NotificationService is completely decoupled from any concrete
 * channel.  It obeys:
 *
 *  • Single Responsibility  — it coordinates notification, not transport.
 *  • Open/Closed            — new channels add zero changes here.
 *  • Dependency Inversion   — depends on NotificationStrategy (abstraction),
 *                             not on EmailStrategy (detail).
 */
class NotificationService {

    // Holds whatever strategy is currently active
    private NotificationStrategy strategy;

    /** Inject initial strategy via constructor (preferred for mandatory deps). */
    public NotificationService(NotificationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * RUNTIME swap — this is the core power of the Strategy pattern.
     * You could read user preferences from a DB and call this setter
     * before notifyUser() without recompiling anything.
     */
    public void setStrategy(NotificationStrategy strategy) {
        System.out.println("[SERVICE] Switching strategy to: "
                + strategy.getClass().getSimpleName());
        this.strategy = strategy;
    }

    /** Business method — delegates the HOW to the strategy. */
    public void notifyUser(String message) {
        if (strategy == null) {
            throw new IllegalStateException("No notification strategy configured!");
        }
        strategy.send(message);
    }
}

// ── Demo runner ───────────────────────────────────────────────
class StrategyDemo {

    public static void run() {
        System.out.println("=== Strategy Pattern (Abstraction) Approach ===");

        // ── Scenario 1: Start with Email ─────────────────────
        NotificationService service = new NotificationService(
                new EmailStrategy("smtp.myapp.com")
        );
        service.notifyUser("Welcome to MyApp!");        // → Email

        // ── Scenario 2: Swap to SMS at runtime ───────────────
        // Imagine: user updated preference in account settings.
        // We simply swap the strategy — NO recompilation, NO if-else.
        service.setStrategy(new SMSStrategy("twilio-api-key-xyz"));
        service.notifyUser("Your OTP is 982341");       // → SMS

        // ── Scenario 3: Swap to Push ─────────────────────────
        service.setStrategy(new PushStrategy());
        service.notifyUser("Flash sale: 50% off for 1 hour!"); // → Push

        // ── Scenario 4: Fallback chain ────────────────────────
        // Primary (broken email) will fail → auto-fallback to SMS.
        NotificationStrategy withFallback = new FallbackStrategy(
                new BrokenEmailStrategy(),
                new SMSStrategy("backup-key")
        );
        service.setStrategy(withFallback);
        service.notifyUser("Critical alert: payment failed!"); // → tries Email, falls to SMS

        System.out.println();
    }
}
