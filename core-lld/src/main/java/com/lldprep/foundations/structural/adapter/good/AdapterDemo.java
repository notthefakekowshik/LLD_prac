package com.lldprep.foundations.structural.adapter.good;

/**
 * Adapter Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * You have an existing class (third-party library, legacy code) whose interface is incompatible
 * with what your code expects. You can't modify the existing class. The Adapter wraps it and
 * translates calls so both sides remain unchanged.
 *
 * <p><b>How it works:</b><br>
 * - {@code PaymentProcessor} — Target interface your client depends on.<br>
 * - {@code LegacyPaymentGateway} — Adaptee: the incompatible class you can't change.<br>
 * - {@code LegacyPaymentAdapter} — Adapter: wraps the adaptee, implements the target interface.<br>
 * - Client only ever talks to {@code PaymentProcessor} — it never imports the legacy class.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>Integrating a third-party library whose API doesn't match your interfaces.</li>
 *   <li>Making legacy code work with new systems without modifying it.</li>
 *   <li>You want to swap vendors/implementations without changing client code (DIP).</li>
 * </ul>
 *
 * <p><b>Adapter vs Facade:</b><br>
 * - {@code Adapter} makes one interface look like another — interface translation.<br>
 * - {@code Facade} simplifies a complex subsystem — complexity hiding.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Object Adapter (composition — the standard Java approach)</li>
 *   <li>Swapping adapters at runtime — proves client is fully decoupled</li>
 * </ol>
 */
public class AdapterDemo {

    public static void main(String[] args) {
        demo1_LegacyAdapter();
        demo2_SwapToStripe();
    }

    // -------------------------------------------------------------------------

    private static void demo1_LegacyAdapter() {
        section("Demo 1: LegacyPaymentGateway adapted to PaymentProcessor");

        LegacyPaymentGateway legacyGateway = new LegacyPaymentGateway();
        PaymentProcessor processor = new LegacyPaymentAdapter(legacyGateway, "USD");

        // Client only calls pay() — knows nothing about makePayment(), currency codes, etc.
        checkout(processor, "ACC-001", 250.00);
        checkout(processor, "ACC-002", 89.99);
    }

    private static void demo2_SwapToStripe() {
        section("Demo 2: Swap to Stripe — zero changes to checkout()");

        // Same client method, different adapter — client code is identical
        PaymentProcessor processor = new StripePaymentAdapter();

        checkout(processor, "cus_stripe_001", 250.00);
        checkout(processor, "cus_stripe_002", 89.99);
    }

    // -------------------------------------------------------------------------

    /** Client method — depends only on the PaymentProcessor interface. */
    private static void checkout(PaymentProcessor processor, String accountId, double amount) {
        System.out.printf("  Checking out: accountId=%s amount=%.2f%n", accountId, amount);
        processor.pay(accountId, amount);
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
