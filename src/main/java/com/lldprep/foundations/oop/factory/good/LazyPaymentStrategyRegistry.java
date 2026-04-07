package com.lldprep.foundations.oop.factory.good;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * GOOD CODE: Supplier-based registry for on-demand strategy creation.
 *
 * Contrast with PaymentStrategyRegistry which stores pre-created instances:
 *
 *   Eager registry  → register(new CreditCardStrategy())
 *                     The object exists in memory from startup,
 *                     even if no one ever pays by credit card that run.
 *
 *   Lazy registry   → register("CREDIT_CARD", CreditCardStrategy::new)
 *                     The Supplier is stored (a method reference, ~few bytes).
 *                     A fresh CreditCardStrategy is instantiated only when
 *                     get() is actually called for that payment type.
 *
 * When to prefer Supplier-based (lazy) registration:
 *   - Strategies hold expensive resources (DB connections, HTTP clients)
 *   - Strategies are stateful and must NOT be shared across requests
 *   - You want a fresh instance per transaction (no shared-state bugs)
 *   - Many strategies are registered but only a few are used per run
 *
 * When to prefer eager (instance) registration:
 *   - Strategies are stateless (safe to share — no fields, pure functions)
 *   - Construction is cheap and you want to fail fast on startup
 */
public class LazyPaymentStrategyRegistry {

    private final Map<String, Supplier<PaymentStrategy>> suppliers = new HashMap<>();

    public void register(String paymentType, Supplier<PaymentStrategy> supplier) {
        suppliers.put(paymentType, supplier);
        System.out.println("[LazyRegistry] Registered supplier for: " + paymentType
            + "  (no object created yet)");
    }

    public PaymentStrategy get(String paymentType) {
        Supplier<PaymentStrategy> supplier = suppliers.get(paymentType);
        if (supplier == null) {
            throw new IllegalArgumentException(
                "No supplier registered for payment type: '" + paymentType + "'. "
                + "Registered types: " + suppliers.keySet()
            );
        }
        System.out.println("[LazyRegistry] get(\"" + paymentType
            + "\") called → instantiating NOW via Supplier.get()");
        return supplier.get();
    }

    public boolean supports(String paymentType) {
        return suppliers.containsKey(paymentType);
    }

    public Set<String> getSupportedTypes() {
        return Collections.unmodifiableSet(suppliers.keySet());
    }
}
