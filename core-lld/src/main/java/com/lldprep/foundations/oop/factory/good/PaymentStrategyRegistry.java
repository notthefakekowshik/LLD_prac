package com.lldprep.foundations.oop.factory.good;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * GOOD CODE: Registry that maps payment type keys to strategy instances.
 * 
 * This is the mechanism that lets the factory obey OCP.
 * 
 * Instead of the factory containing if/else logic, strategies register
 * themselves here. The factory only ever talks to this registry.
 * 
 * To add a new payment type:
 *   1. Create a new class implementing PaymentStrategy
 *   2. Call registry.register(new MyNewStrategy())
 *   3. Done — PaymentStrategyFactory is NEVER touched
 * 
 * In a Spring/DI framework this registry is populated automatically
 * by injecting all PaymentStrategy beans — zero manual registration needed.
 */
public class PaymentStrategyRegistry {
    
    private final Map<String, PaymentStrategy> strategies = new HashMap<>();
    
    public void register(PaymentStrategy strategy) {
        strategies.put(strategy.getPaymentType(), strategy);
        System.out.println("[Registry] Registered strategy: " + strategy.getPaymentType());
    }
    
    public PaymentStrategy get(String paymentType) {
        PaymentStrategy strategy = strategies.get(paymentType);
        if (strategy == null) {
            throw new IllegalArgumentException(
                "No strategy registered for payment type: '" + paymentType + "'. " +
                "Registered types: " + strategies.keySet()
            );
        }
        return strategy;
    }
    
    public boolean supports(String paymentType) {
        return strategies.containsKey(paymentType);
    }
    
    public Set<String> getSupportedTypes() {
        return Collections.unmodifiableSet(strategies.keySet());
    }
}
