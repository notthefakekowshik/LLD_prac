package com.lldprep.systems.atm.exception;

import java.math.BigDecimal;

public class InsufficientCashException extends ATMException {
    private final BigDecimal requested;

    public InsufficientCashException(BigDecimal requested) {
        super(String.format("ATM cannot dispense ₹%s. Insufficient cash or cannot form exact amount.", requested));
        this.requested = requested;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
