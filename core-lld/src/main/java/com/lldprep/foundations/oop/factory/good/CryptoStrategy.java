package com.lldprep.foundations.oop.factory.good;

public class CryptoStrategy implements PaymentStrategy {
    
    @Override
    public void processPayment(String orderId, double amount) {
        System.out.println("[CRYPTO] Generating wallet address for order " + orderId);
        System.out.println("[CRYPTO] Waiting for blockchain confirmation...");
        System.out.println("[CRYPTO] $" + amount + " received in BTC equivalent.");
    }
    
    @Override
    public String getPaymentType() {
        return "CRYPTO";
    }
}
