package com.lldprep.foundations.oop.factory.good;

public class CreditCardStrategy implements PaymentStrategy {
    
    @Override
    public void processPayment(String orderId, double amount) {
        System.out.println("[CREDIT CARD] Charging $" + amount + " for order " + orderId);
        System.out.println("[CREDIT CARD] Validating card number, CVV, expiry...");
        System.out.println("[CREDIT CARD] Payment authorised via Visa/Mastercard network.");
    }
    
    @Override
    public String getPaymentType() {
        return "CREDIT_CARD";
    }
}
