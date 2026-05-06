package com.lldprep.systems.atm.exception;

public class InvalidPINException extends ATMException {
    private final int remainingAttempts;

    public InvalidPINException(int remainingAttempts) {
        super(String.format("Invalid PIN. %d attempts remaining.", remainingAttempts));
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
