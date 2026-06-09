package com.lldprep.systems.lfucache.exception;

/** Programming error — invalid cache configuration or usage. */
public class CacheException extends RuntimeException {
    public CacheException(String message) {
        super(message);
    }
}
