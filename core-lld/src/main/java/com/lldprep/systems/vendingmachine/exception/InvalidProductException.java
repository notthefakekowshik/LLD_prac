package com.lldprep.systems.vendingmachine.exception;

/**
 * Thrown when invalid product code is entered.
 */
public class InvalidProductException extends VendingMachineException {
    private final String code;

    public InvalidProductException(String code) {
        super(String.format("Invalid product code: %s", code));
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
