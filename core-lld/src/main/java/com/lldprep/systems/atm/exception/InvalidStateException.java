package com.lldprep.systems.atm.exception;

public class InvalidStateException extends ATMException {
    private final String currentState;
    private final String attemptedOperation;

    public InvalidStateException(String currentState, String attemptedOperation) {
        super(String.format("Cannot perform '%s' in state '%s'", attemptedOperation, currentState));
        this.currentState = currentState;
        this.attemptedOperation = attemptedOperation;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getAttemptedOperation() {
        return attemptedOperation;
    }
}
