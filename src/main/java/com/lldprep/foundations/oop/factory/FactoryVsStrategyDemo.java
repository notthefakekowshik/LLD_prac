package com.lldprep.foundations.oop.factory;

import com.lldprep.foundations.oop.factory.bad.PaymentProcessorFactory;
import com.lldprep.foundations.oop.factory.good.ApplePayStrategy;
import com.lldprep.foundations.oop.factory.good.CreditCardStrategy;
import com.lldprep.foundations.oop.factory.good.CryptoStrategy;
import com.lldprep.foundations.oop.factory.good.OrderService;
import com.lldprep.foundations.oop.factory.good.PayPalStrategy;
import com.lldprep.foundations.oop.factory.good.PaymentStrategyFactory;
import com.lldprep.foundations.oop.factory.good.LazyPaymentStrategyRegistry;
import com.lldprep.foundations.oop.factory.good.PaymentStrategy;
import com.lldprep.foundations.oop.factory.good.PaymentStrategyRegistry;

/**
 * Demonstrates how the standard Factory pattern violates the Open/Closed Principle
 * and how the Strategy pattern + Registry fixes it.
 *
 * Domain: Payment processing (CreditCard, PayPal, Crypto, ApplePay)
 *
 * Key Learning:
 *   Factory alone answers "how to create" but hard-codes the WHAT via if/else.
 *   Strategy + Registry separates "how to create" from "which type exists",
 *   so the factory never needs to change when a new type is added.
 */
public class FactoryVsStrategyDemo {

    public static void main(String[] args) {
        System.out.println("===== FACTORY vs STRATEGY: OCP VIOLATION & FIX =====\n");

        demonstrateBadFactory();
        demonstrateOcpViolation();
        demonstrateGoodStrategyFactory();
        demonstrateExtensionWithoutModification();
        demonstrateSupplierRegistry();
        printSideBySideComparison();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BAD: Standard Factory
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shows the standard factory working fine for known types.
     * The problem isn't visible until you try to add a new type.
     */
    private static void demonstrateBadFactory() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║         BAD: STANDARD FACTORY (if/else chain)             ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        System.out.println("The factory works for existing types...\n");

        // Direct factory usage to show the if/else in action
        com.lldprep.foundations.oop.factory.bad.PaymentProcessor p1 =
            PaymentProcessorFactory.create("CREDIT_CARD");
        p1.processPayment("ORD-001", 99.99);
        System.out.println();

        com.lldprep.foundations.oop.factory.bad.PaymentProcessor p2 =
            PaymentProcessorFactory.create("PAYPAL");
        p2.processPayment("ORD-002", 49.99);
        System.out.println();

        System.out.println("❌ Look at PaymentProcessorFactory.java — it's a chain of if/else.");
        System.out.println("   Every payment type is hard-coded inside that one method.\n");
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Explicitly demonstrates the OCP violation: adding a new type forces
     * modification of the factory.
     */
    private static void demonstrateOcpViolation() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║         THE OCP VIOLATION — Adding APPLE_PAY              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        System.out.println("Requirement: Add Apple Pay support.\n");

        System.out.println("What you MUST do in the BAD design:");
        System.out.println("  1. Open  PaymentProcessorFactory.java          ← MODIFICATION");
        System.out.println("  2. Add:  else if (type.equals(\"APPLE_PAY\")) {  ← MODIFICATION");
        System.out.println("  3.           return new ApplePayProcessor();    ← MODIFICATION");
        System.out.println("  4.       }                                      ← MODIFICATION");
        System.out.println("  5. Retest the entire factory (all branches)     ← REGRESSION RISK");
        System.out.println();
        System.out.println("  Every developer adding a payment type edits the same file.");
        System.out.println("  On a team of 5, that's 5 merge conflicts on one method.\n");

        System.out.println("Trying to use APPLE_PAY in the bad factory:");
        try {
            PaymentProcessorFactory.create("APPLE_PAY");
        } catch (IllegalArgumentException e) {
            System.out.println("  ❌ Runtime Exception: " + e.getMessage());
            System.out.println("  ❌ And fixing it requires modifying PaymentProcessorFactory.\n");
        }

        System.out.println("=".repeat(80) + "\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GOOD: Strategy + Registry
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shows the Strategy + Registry approach where all known types are registered
     * and the factory never contains a single if/else.
     */
    private static void demonstrateGoodStrategyFactory() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║       GOOD: STRATEGY + REGISTRY (OCP satisfied)           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        System.out.println("Setting up the registry (typically done at app startup / DI container):\n");

        PaymentStrategyRegistry registry = new PaymentStrategyRegistry();
        registry.register(new CreditCardStrategy());
        registry.register(new PayPalStrategy());
        registry.register(new CryptoStrategy());
        System.out.println();

        PaymentStrategyFactory factory = new PaymentStrategyFactory(registry);
        OrderService orderService = new OrderService(factory);

        System.out.println("Processing orders — factory has ZERO if/else:\n");

        orderService.placeOrder("ORD-001", 99.99,  "CREDIT_CARD");
        orderService.placeOrder("ORD-002", 49.99,  "PAYPAL");
        orderService.placeOrder("ORD-003", 199.99, "CRYPTO");

        System.out.println("✅ PaymentStrategyFactory.java contains: return registry.get(paymentType)");
        System.out.println("   That one line handles ALL payment types — past, present, and future.\n");
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * The centrepiece: adding Apple Pay without touching any existing class.
     */
    private static void demonstrateExtensionWithoutModification() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║       BENEFIT: Adding APPLE_PAY — zero existing edits     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        System.out.println("Files changed to add Apple Pay:");
        System.out.println("  ✅ ApplePayStrategy.java  — NEW file (extension, not modification)");
        System.out.println("  ❌ PaymentStrategyFactory.java  — NOT touched");
        System.out.println("  ❌ PaymentStrategyRegistry.java — NOT touched");
        System.out.println("  ❌ OrderService.java            — NOT touched");
        System.out.println("  ❌ CreditCardStrategy.java      — NOT touched");
        System.out.println("  ❌ PayPalStrategy.java          — NOT touched");
        System.out.println("  ❌ CryptoStrategy.java          — NOT touched\n");

        PaymentStrategyRegistry registry = new PaymentStrategyRegistry();
        registry.register(new CreditCardStrategy());
        registry.register(new PayPalStrategy());
        registry.register(new CryptoStrategy());
        registry.register(new ApplePayStrategy());  // ← only new line at wiring time
        System.out.println();

        PaymentStrategyFactory factory = new PaymentStrategyFactory(registry);
        OrderService orderService = new OrderService(factory);

        System.out.println("Apple Pay order processed with zero changes to existing code:\n");
        orderService.placeOrder("ORD-004", 299.99, "APPLE_PAY");

        System.out.println("Supported payment types: " +
            registry.getSupportedTypes() + "\n");

        System.out.println("✅ Open/Closed Principle fully satisfied:");
        System.out.println("   OPEN for extension  — just implement PaymentStrategy");
        System.out.println("   CLOSED for modification — no existing file was edited\n");
        System.out.println("=".repeat(80) + "\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BONUS: Supplier-based (lazy) registry
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Demonstrates Supplier<PaymentStrategy> registration so that strategy
     * objects are created ON DEMAND — only when a payment is actually processed.
     *
     * The existing demonstrateGoodStrategyFactory() stores pre-created instances:
     *   registry.register(new CreditCardStrategy())   ← object created at registration
     *
     * Here we store a Supplier (a method reference) instead:
     *   registry.register("CREDIT_CARD", CreditCardStrategy::new)  ← just a reference
     *   registry.get("CREDIT_CARD")  ← object created HERE, fresh per call
     *
     * This matters when strategies are stateful (hold per-transaction data)
     * or expensive to construct — you don't pay the cost until you need them.
     */
    private static void demonstrateSupplierRegistry() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║     BONUS: SUPPLIER REGISTRY — On-Demand Instantiation    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        System.out.println("Registering suppliers (method references) — NO objects created yet:\n");

        LazyPaymentStrategyRegistry lazyRegistry = new LazyPaymentStrategyRegistry();

        // Method references: CreditCardStrategy::new is a Supplier<PaymentStrategy>
        // The constructor is NOT called here — only stored as a reference.
        lazyRegistry.register("CREDIT_CARD", CreditCardStrategy::new);
        lazyRegistry.register("PAYPAL",      PayPalStrategy::new);
        lazyRegistry.register("CRYPTO",      CryptoStrategy::new);
        lazyRegistry.register("APPLE_PAY",   ApplePayStrategy::new);

        System.out.println();
        System.out.println("Registered types: " + lazyRegistry.getSupportedTypes());
        System.out.println("No PaymentStrategy instance exists in memory yet.\n");

        System.out.println("─".repeat(60));
        System.out.println("Now calling get(\"CREDIT_CARD\") — instantiation happens here:\n");

        PaymentStrategy s1 = lazyRegistry.get("CREDIT_CARD");
        s1.processPayment("ORD-010", 79.99);
        System.out.println();

        System.out.println("─".repeat(60));
        System.out.println("Calling get(\"CREDIT_CARD\") again — a FRESH instance every time:\n");

        PaymentStrategy s2 = lazyRegistry.get("CREDIT_CARD");
        System.out.println("s1 == s2 (same instance?): " + (s1 == s2)
            + "  ← false because Supplier.get() calls 'new' each time");
        System.out.println();

        System.out.println("─".repeat(60));
        System.out.println("APPLE_PAY was registered but never requested — zero construction cost:\n");
        System.out.println("  lazyRegistry.supports(\"APPLE_PAY\"): " + lazyRegistry.supports("APPLE_PAY"));
        System.out.println("  But no ApplePayStrategy object was ever created.\n");

        System.out.println("\n✅ Eager (instance) registry  → one shared instance, created at startup");
        System.out.println(  "✅ Lazy  (Supplier) registry  → fresh instance per call, created on demand");
        System.out.println();
        System.out.println(  "   Choose EAGER  when strategies are stateless (safe to share)");
        System.out.println(  "   Choose LAZY   when strategies are stateful or expensive to build");
        System.out.println();
        System.out.println("=" .repeat(80) + "\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Summary
    // ─────────────────────────────────────────────────────────────────────────

    private static void printSideBySideComparison() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║               SIDE-BY-SIDE COMPARISON                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        System.out.println("  Aspect                  BAD (Factory if/else)       GOOD (Strategy + Registry)");
        System.out.println("  ─────────────────────── ─────────────────────────── ────────────────────────────");
        System.out.println("  Adding a new type       Edit PaymentProcessorFactory  Create a new Strategy class");
        System.out.println("  OCP compliance          ❌ Violated                  ✅ Satisfied");
        System.out.println("  Factory body grows?     ❌ Yes, forever              ✅ No, stays 1 line");
        System.out.println("  Merge conflicts         ❌ Yes (1 shared file)       ✅ No (separate files)");
        System.out.println("  Compile-time safety     ❌ String typo = RuntimeEx   ✅ Registry lookup throws early");
        System.out.println("  Testability             ❌ Must test all branches     ✅ Test each strategy in isolation");
        System.out.println("  Runtime registration    ❌ Not possible              ✅ Possible (plugins, DI)");
        System.out.println();

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  KEY INSIGHT                                                 │");
        System.out.println("│                                                              │");
        System.out.println("│  Factory answers: \"how do I CREATE the right object?\"       │");
        System.out.println("│  Strategy answers: \"how do I BEHAVE differently per type?\"  │");
        System.out.println("│                                                              │");
        System.out.println("│  The factory's if/else is SELECTING a strategy.             │");
        System.out.println("│  Replace that selection logic with a Registry lookup and    │");
        System.out.println("│  the factory obeys OCP — it never needs to change again.    │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }
}
