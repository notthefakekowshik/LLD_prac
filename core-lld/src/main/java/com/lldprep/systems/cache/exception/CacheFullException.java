package com.lldprep.systems.cache.exception;

/** Thrown by Storage.add() when the cache has reached capacity and the key is new. */
public class CacheFullException extends RuntimeException {
    public CacheFullException(String message) {
        super(message);
    }
}
