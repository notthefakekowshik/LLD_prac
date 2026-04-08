package com.lldprep.foundations.oop.factory.bad;

/**
 * BAD CODE: Standard Factory with if/else chains.
 * 
 * THE OCP VIOLATION:
 * Every time a new payment method is added (e.g., "APPLE_PAY", "BANK_TRANSFER"),
 * you MUST open this file and add another else-if branch.
 * 
 * This class can NEVER be closed for modification.
 * It grows forever and becomes the bottleneck for every new payment type.
 * 
 * Problems:
 * 1. Violates OCP — must modify this class for every new payment type
 * 2. This file becomes a merge conflict hotspot on a team
 * 3. The factory has to KNOW about every concrete class (tight coupling)
 * 4. Unit testing requires testing all branches of this one method
 * 5. String-based switching is brittle — typo in "CREDIT_CARD" silently fails
 */
public class PaymentProcessorFactory {
    
    public static PaymentProcessor create(String paymentType) {
        // ❌ Every new payment type requires modifying THIS method
        if (paymentType.equals("CREDIT_CARD")) {
            return new CreditCardProcessor();
        } else if (paymentType.equals("PAYPAL")) {
            return new PayPalProcessor();
        } else if (paymentType.equals("CRYPTO")) {
            return new CryptoProcessor();
        }
        // ❌ What happens when "APPLE_PAY" is needed?
        //    → Edit this file, add another else-if, recompile, retest
        //    → Every developer adding a payment type races to modify this one class
        else {
            throw new IllegalArgumentException("Unknown payment type: " + paymentType);
        }
    }
}
