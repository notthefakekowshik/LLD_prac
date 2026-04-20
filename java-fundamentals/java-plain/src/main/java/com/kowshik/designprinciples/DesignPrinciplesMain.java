package com.kowshik.designprinciples;

/**
 * ============================================================
 *  ENTRY POINT — runs both demos side-by-side
 * ============================================================
 *
 *  Run:
 *    mvn compile exec:java -Dexec.mainClass="com.kowshik.designprinciples.DesignPrinciplesMain"
 *    OR from your IDE, right-click → Run
 */
public class DesignPrinciplesMain {

    public static void main(String[] args) {

        // ── PART 1: Inheritance — channel is frozen at compile-time ──
        InheritanceDemo.run();

        // ── PART 2: Strategy Pattern — channel swapped at runtime ────
        StrategyDemo.run();

        // ── SUMMARY printed to console ───────────────────────────────
        System.out.println("=========================================");
        System.out.println("  KEY TAKEAWAY");
        System.out.println("=========================================");
        System.out.println("""
                Inheritance (IS-A):
                  • Channel decided at compile-time via `new ConcreteClass()`.
                  • Adding a channel may break or force changes to existing code.
                  • Violates OCP (Open/Closed Principle).
                
                Strategy Pattern (HAS-A / Composition):
                  • NotificationService HAS-A NotificationStrategy.
                  • Channel decided at RUNTIME via setStrategy() or constructor injection.
                  • Adding PushNotification = add one class, zero changes elsewhere.
                  • Satisfies OCP, SRP, DIP — the core SOLID trio for this problem.
                  • Strategies are composable (FallbackStrategy wraps two others).
                """);
    }
}
