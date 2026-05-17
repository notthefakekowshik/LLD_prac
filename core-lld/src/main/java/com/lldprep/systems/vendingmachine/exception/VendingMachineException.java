package com.lldprep.systems.vendingmachine.exception;

/**
 * Base exception for all vending machine errors.
 */
public class VendingMachineException extends Exception {
    public VendingMachineException(String message) {
        super(message);
    }

    public VendingMachineException(String message, Throwable cause) {
        super(message, cause);
    }
}
