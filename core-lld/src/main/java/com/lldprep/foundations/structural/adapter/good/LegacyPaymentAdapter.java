package com.lldprep.foundations.structural.adapter.good;

/**
 * Adapter — wraps the legacy gateway and translates calls to the target interface.
 *
 * <p>The client calls {@code pay(accountId, amount)} on the {@link PaymentProcessor} interface.
 * The adapter translates that into the legacy {@code makePayment(currency, amount, accountRef)} call.
 * The client never imports or knows about {@link LegacyPaymentGateway}.
 */
public class LegacyPaymentAdapter implements PaymentProcessor {

    private final LegacyPaymentGateway gateway;
    private final String defaultCurrency;

    public LegacyPaymentAdapter(LegacyPaymentGateway gateway, String defaultCurrency) {
        this.gateway = gateway;
        this.defaultCurrency = defaultCurrency;
    }

    @Override
    public void pay(String accountId, double amount) {
        // Translate the target interface call into the adaptee's method signature
        boolean success = gateway.makePayment(defaultCurrency, amount, accountId);
        if (!success) {
            throw new RuntimeException("Payment failed for account: " + accountId);
        }
    }
}
