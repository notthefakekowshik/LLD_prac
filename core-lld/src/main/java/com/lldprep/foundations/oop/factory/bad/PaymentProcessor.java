package com.lldprep.foundations.oop.factory.bad;

/**
 * BAD CODE: Base class with no interface contract.
 * 
 * All concrete processors extend this directly.
 * The Factory must know about every subclass to instantiate them.
 */
public abstract class PaymentProcessor {
    
    public abstract void processPayment(String orderId, double amount);
    
    public abstract String getProcessorName();
}
