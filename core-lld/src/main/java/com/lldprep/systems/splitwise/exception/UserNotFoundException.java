package com.lldprep.systems.splitwise.exception;

public class UserNotFoundException extends SplitwiseException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}
