package com.lldprep.systems.splitwise.exception;

public class DuplicateEmailException extends SplitwiseException {
    public DuplicateEmailException(String email) {
        super("User with email already exists: " + email);
    }
}
