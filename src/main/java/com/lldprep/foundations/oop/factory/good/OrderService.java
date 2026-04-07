package com.lldprep.foundations.oop.factory.good;

/**
 * GOOD CODE: OrderService depends only on PaymentStrategyFactory.
 * 
 * It has no knowledge of CreditCardStrategy, PayPalStrategy, etc.
 * Adding ApplePayStrategy doesn't require a single change here.
 */
public class OrderService {
    
    private final PaymentStrategyFactory factory;
    
    public OrderService(PaymentStrategyFactory factory) {
        this.factory = factory;
    }
    
    public void placeOrder(String orderId, double amount, String paymentType) {
        System.out.println("[OrderService] Placing order " + orderId + " for $" + amount);
        
        PaymentStrategy strategy = factory.create(paymentType);
        strategy.processPayment(orderId, amount);
        
        System.out.println("[OrderService] Order " + orderId + " confirmed.\n");
    }
}
