package com.lldprep.foundations.structural.adapter.good;

/**
 * Target interface — what the client code depends on.
 * The client only ever calls {@code pay()}; it knows nothing about any vendor.
 */
public interface PaymentProcessor {
    void pay(String accountId, double amount);
}
