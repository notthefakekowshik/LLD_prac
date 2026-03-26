// Concrete processor: only implements what is unique to credit card processing.
// Validation, logging, and the pay/refund template are inherited from AbstractPaymentProcessor.
package com.lldprep.foundations.oop.abstractvsinterface;

public class CreditCardProcessor extends AbstractPaymentProcessor {

    private final String cardLastFour;

    public CreditCardProcessor(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    @Override
    protected void processPayment(double amount) {
        System.out.printf("[CreditCard] Charging $%.2f to card ending in %s%n", amount, cardLastFour);
    }

    @Override
    public String getPaymentMethod() {
        return "CreditCard-" + cardLastFour;
    }
}
