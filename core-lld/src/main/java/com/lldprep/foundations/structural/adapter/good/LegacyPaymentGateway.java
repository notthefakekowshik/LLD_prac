package com.lldprep.foundations.structural.adapter.good;

/**
 * Adaptee — the existing third-party class with an incompatible interface.
 * We cannot (or should not) modify this class.
 */
public class LegacyPaymentGateway {

    public boolean makePayment(String currencyCode, double amount, String accountRef) {
        System.out.printf("  [LegacyGateway] Charging %.2f %s from account %s%n",
                amount, currencyCode, accountRef);
        return true;
    }
}
