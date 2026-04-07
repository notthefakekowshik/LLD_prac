package com.lldprep.foundations.oop.factory.good;

/**
 * GOOD CODE: Strategy interface — the contract every payment type must satisfy.
 * 
 * This interface is the key to fixing the OCP violation.
 * 
 * Instead of the factory knowing about every concrete class,
 * each concrete class registers itself. The factory only ever
 * depends on THIS interface — it never needs to change.
 */
public interface PaymentStrategy {
    
    /**
     * Processes a payment for the given order.
     * 
     * @param orderId the order identifier
     * @param amount  the amount to charge
     */
    void processPayment(String orderId, double amount);
    
    /**
     * Returns the unique key this strategy is registered under.
     * Used by PaymentStrategyRegistry to self-register.
     * 
     * @return payment type key (e.g., "CREDIT_CARD")
     */
    String getPaymentType();
}
