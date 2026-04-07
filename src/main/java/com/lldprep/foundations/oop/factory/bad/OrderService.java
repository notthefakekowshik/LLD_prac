package com.lldprep.foundations.oop.factory.bad;

/**
 * BAD CODE: OrderService uses the factory, but is still brittle.
 * 
 * The string "CREDIT_CARD" is passed around like a magic value.
 * Any typo compiles fine but fails at runtime with an exception.
 */
public class OrderService {
    
    public void placeOrder(String orderId, double amount, String paymentType) {
        System.out.println("[OrderService] Placing order " + orderId + " for $" + amount);
        
        // ❌ String magic value — no compile-time safety
        PaymentProcessor processor = PaymentProcessorFactory.create(paymentType);
        processor.processPayment(orderId, amount);
        
        System.out.println("[OrderService] Order " + orderId + " confirmed.\n");
    }
}
