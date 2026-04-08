package com.lldprep.foundations.oop.factory.good;

/**
 * GOOD CODE: Factory that is truly closed for modification.
 * 
 * Compare to the BAD version:
 * 
 *   BAD factory:   if CREDIT_CARD → ...
 *                  else if PAYPAL → ...
 *                  else if CRYPTO → ...      ← grows forever
 *                  else if APPLE_PAY → ...   ← requires editing this file
 * 
 *   GOOD factory:  return registry.get(paymentType)  ← never changes
 * 
 * The factory delegates ALL knowledge of concrete types to the registry.
 * Adding a new payment type means registering a new strategy — this class
 * is NEVER opened again.
 * 
 * OCP satisfied:
 *   ✅ OPEN for extension  — register any new PaymentStrategy
 *   ✅ CLOSED for modification — this file never changes
 */
public class PaymentStrategyFactory {
    
    private final PaymentStrategyRegistry registry;
    
    public PaymentStrategyFactory(PaymentStrategyRegistry registry) {
        this.registry = registry;
    }
    
    public PaymentStrategy create(String paymentType) {
        return registry.get(paymentType);
    }
}
