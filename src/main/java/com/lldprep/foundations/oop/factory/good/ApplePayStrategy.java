package com.lldprep.foundations.oop.factory.good;

/**
 * GOOD CODE: New payment type added without touching ANY existing class.
 * 
 * This is OCP in action:
 * - PaymentStrategyFactory    → NOT modified
 * - PaymentStrategyRegistry   → NOT modified
 * - OrderService              → NOT modified
 * - Existing strategies       → NOT modified
 * 
 * Just create this class, register it, and it works.
 */
public class ApplePayStrategy implements PaymentStrategy {
    
    @Override
    public void processPayment(String orderId, double amount) {
        System.out.println("[APPLE PAY] Requesting Face ID / Touch ID for order " + orderId);
        System.out.println("[APPLE PAY] Tokenising card via Apple Wallet...");
        System.out.println("[APPLE PAY] $" + amount + " charged via Apple Pay.");
    }
    
    @Override
    public String getPaymentType() {
        return "APPLE_PAY";
    }
}
