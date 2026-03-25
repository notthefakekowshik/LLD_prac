package com.lldprep.cache.exception;

public class CacheFullException extends RuntimeException {
    public CacheFullException(String message) {
        super(message);
    }
}
