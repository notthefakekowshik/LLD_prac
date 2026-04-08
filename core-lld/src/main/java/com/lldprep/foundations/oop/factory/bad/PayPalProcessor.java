package com.lldprep.foundations.oop.factory.bad;

public class PayPalProcessor extends PaymentProcessor {
    
    @Override
    public void processPayment(String orderId, double amount) {
        System.out.println("[PAYPAL] Redirecting to PayPal login for order " + orderId);
        System.out.println("[PAYPAL] Verifying PayPal account balance...");
        System.out.println("[PAYPAL] $" + amount + " transferred successfully.");
    }
    
    @Override
    public String getProcessorName() {
        return "PayPal";
    }
}
