// Concrete processor: only implements what is unique to PayPal processing.
// Validation, logging, and the pay/refund template are inherited from AbstractPaymentProcessor.
package com.lldprep.foundations.oop.abstractvsinterface;

public class PayPalProcessor extends AbstractPaymentProcessor {

    private final String email;

    public PayPalProcessor(String email) {
        this.email = email;
    }

    @Override
    protected void processPayment(double amount) {
        System.out.printf("[PayPal] Charging $%.2f to PayPal account: %s%n", amount, email);
    }

    @Override
    public String getPaymentMethod() {
        return "PayPal-" + email;
    }
}
