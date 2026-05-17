package com.lldprep.systems.vendingmachine.exception;

import java.math.BigDecimal;

/**
 * Thrown when machine cannot provide exact change.
 */
public class InsufficientChangeException extends VendingMachineException {
    private final BigDecimal changeRequired;

    public InsufficientChangeException(BigDecimal changeRequired) {
        super(String.format("Cannot provide change of ₹%s. Exact change required.", changeRequired));
        this.changeRequired = changeRequired;
    }

    public BigDecimal getChangeRequired() {
        return changeRequired;
    }
}
