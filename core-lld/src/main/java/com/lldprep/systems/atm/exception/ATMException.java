package com.lldprep.systems.atm.exception;

public class ATMException extends Exception {
    public ATMException(String message) {
        super(message);
    }

    public ATMException(String message, Throwable cause) {
        super(message, cause);
    }
}
