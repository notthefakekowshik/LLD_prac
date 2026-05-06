package com.lldprep.systems.atm.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends ATMException {
    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientFundsException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient funds: requested ₹%s, available ₹%s", requested, available));
        this.requested = requested;
        this.available = available;
    }

    public BigDecimal getRequested() {
        return requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}
