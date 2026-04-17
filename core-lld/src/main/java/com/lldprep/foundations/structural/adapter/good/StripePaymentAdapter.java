package com.lldprep.foundations.structural.adapter.good;

/**
 * Second adapter — adapts a different provider (Stripe) to the same target interface.
 *
 * <p>The client code is unchanged. Swapping providers = swapping the adapter injected at startup.
 * This is OCP in action: new provider → new adapter class, zero changes to client.
 */
public class StripePaymentAdapter implements PaymentProcessor {

    @Override
    public void pay(String accountId, double amount) {
        // Stripe has its own internal API format
        System.out.printf("  [StripeGateway] POST /v1/charges amount=%d currency=usd customer=%s%n",
                (int) (amount * 100), accountId);
    }
}
