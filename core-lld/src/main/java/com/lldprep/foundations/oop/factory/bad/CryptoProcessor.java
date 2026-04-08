package com.lldprep.foundations.oop.factory.bad;

public class CryptoProcessor extends PaymentProcessor {
    
    @Override
    public void processPayment(String orderId, double amount) {
        System.out.println("[CRYPTO] Generating wallet address for order " + orderId);
        System.out.println("[CRYPTO] Waiting for blockchain confirmation...");
        System.out.println("[CRYPTO] $" + amount + " received in BTC equivalent.");
    }
    
    @Override
    public String getProcessorName() {
        return "Crypto";
    }
}
