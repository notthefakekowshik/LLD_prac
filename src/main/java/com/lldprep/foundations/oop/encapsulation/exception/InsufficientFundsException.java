// Custom unchecked exception for BankAccount domain invariant violations.
// Using RuntimeException keeps the API clean — callers can handle it if needed,
// but are not forced to declare it in every method signature.
package com.lldprep.foundations.oop.encapsulation.exception;

public class InsufficientFundsException extends RuntimeException {

    private final double requestedAmount;
    private final double availableBalance;

    public InsufficientFundsException(double requestedAmount, double availableBalance) {
        super(String.format(
            "Insufficient funds: requested $%.2f but available balance is $%.2f",
            requestedAmount, availableBalance));
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }

    public double getRequestedAmount() {
        return requestedAmount;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }
}
