package com.lldprep.bloomfilter.exception;

public class BloomFilterException extends RuntimeException {
    
    public BloomFilterException(String message) {
        super(message);
    }
    
    public BloomFilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
