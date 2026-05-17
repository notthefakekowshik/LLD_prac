package com.lldprep.systems.vendingmachine.exception;

import java.math.BigDecimal;

/**
 * Thrown when inserted amount is less than product price.
 */
public class InsufficientFundsException extends VendingMachineException {
    private final BigDecimal required;
    private final BigDecimal inserted;

    public InsufficientFundsException(BigDecimal required, BigDecimal inserted) {
        super(String.format("Insufficient funds. Required: ₹%s, Inserted: ₹%s", required, inserted));
        this.required = required;
        this.inserted = inserted;
    }

    public BigDecimal getRequired() {
        return required;
    }

    public BigDecimal getInserted() {
        return inserted;
    }

    public BigDecimal getShortfall() {
        return required.subtract(inserted);
    }
}
