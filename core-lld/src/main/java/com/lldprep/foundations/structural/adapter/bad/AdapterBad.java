package com.lldprep.foundations.structural.adapter.bad;

/**
 * BAD: The client is tightly coupled to the legacy third-party library.
 *
 * Problems:
 * 1. If the third-party API changes, every call site in the codebase must change.
 * 2. You cannot swap to a different payment provider without editing client code.
 * 3. The client is forced to know legacy method names like {@code makePayment()} and
 *    pass parameters in the vendor's format — leaking third-party details everywhere.
 * 4. Impossible to unit-test without the real third-party class.
 */
public class AdapterBad {

    public static void main(String[] args) {
        LegacyPaymentGateway gateway = new LegacyPaymentGateway();

        // Client is directly tied to legacy API — method name, param order, return type all exposed
        boolean success = gateway.makePayment("USD", 250.00, "ACC-001");
        System.out.println("Payment result: " + success);
    }
}

class LegacyPaymentGateway {
    /** Third-party method with its own signature — client must know this exactly. */
    public boolean makePayment(String currencyCode, double amount, String accountRef) {
        System.out.printf("  [LegacyGateway] Charging %.2f %s from account %s%n",
                amount, currencyCode, accountRef);
        return true;
    }
}
