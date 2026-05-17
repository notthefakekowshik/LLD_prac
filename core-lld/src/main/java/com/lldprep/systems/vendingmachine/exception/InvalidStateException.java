package com.lldprep.systems.vendingmachine.exception;

/**
 * Thrown when operation is not valid for current state.
 */
public class InvalidStateException extends VendingMachineException {
    private final String currentState;
    private final String attemptedOperation;

    public InvalidStateException(String currentState, String attemptedOperation) {
        super(String.format("Cannot %s in %s state", attemptedOperation, currentState));
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
