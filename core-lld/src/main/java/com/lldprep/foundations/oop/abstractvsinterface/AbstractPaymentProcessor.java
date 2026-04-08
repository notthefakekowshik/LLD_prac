// Abstract class provides shared validation + logging template. Interface alone cannot share state or implementation.
// WHY abstract class here:
//   1. All processors share identical validateAmount and logTransaction logic — define it ONCE.
//   2. The pay() template method enforces a fixed sequence (validate → process → log) for ALL subclasses.
//   3. Interface default methods could share code, but abstract classes better express "is-a" relationships
//      and can hold shared state (e.g., a transaction counter or config) if needed.
// WHEN to choose abstract class over interface:
//   - You have shared implementation (not just contracts)
//   - You want to enforce a template method pattern
//   - Subclasses belong to the same conceptual family (all are PaymentProcessors)
package com.lldprep.foundations.oop.abstractvsinterface;

public abstract class AbstractPaymentProcessor implements Payable {

    // Shared validation — every processor benefits, written once
    protected void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive. Got: " + amount);
        }
    }

    // Shared logging — every processor benefits, written once
    protected void logTransaction(String type, double amount) {
        System.out.printf("[LOG] %s | Method: %-15s | Amount: $%.2f%n",
            type, getPaymentMethod(), amount);
    }

    // Template Method pattern: enforces the sequence validate → processPayment → log.
    // Subclasses cannot accidentally skip validation or logging.
    @Override
    public void pay(double amount) {
        validateAmount(amount);
        processPayment(amount);
        logTransaction("PAY    ", amount);
    }

    // Refund uses shared validate + log; no custom processing needed for this demo
    @Override
    public void refund(double amount) {
        validateAmount(amount);
        logTransaction("REFUND ", amount);
        System.out.println("[" + getPaymentMethod() + "] Refund of $" + amount + " processed.");
    }

    // The only thing subclasses MUST provide — their specific payment processing logic
    protected abstract void processPayment(double amount);
}
