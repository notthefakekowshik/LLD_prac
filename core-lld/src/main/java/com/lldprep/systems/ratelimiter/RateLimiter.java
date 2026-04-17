package com.lldprep.systems.ratelimiter;

/**
 * Strategy Pattern: Interface for rate limiting algorithms.
 * 
 * Different algorithms can be swapped without modifying client code.
 * This follows the Open/Closed Principle - new algorithms can be added
 * by creating new implementations without changing existing code.
 * 
 * Thread Safety: All implementations MUST be thread-safe.
 */
public interface RateLimiter {
    
    /**
     * Attempts to acquire a single permit.
     * 
     * Returns immediately with true if permit is available, false otherwise.
     * This is a non-blocking operation.
     * 
     * Thread-safe: Can be called concurrently from multiple threads.
     * 
     * @return true if request is allowed, false if rate limit exceeded
     */
    boolean tryAcquire();
    
    /**
     * Attempts to acquire the specified number of permits.
     * 
     * Useful for operations that consume multiple quota units.
     * For example, uploading a 5MB file might consume 5 permits.
     * 
     * @param permits Number of permits to acquire (must be > 0)
     * @return true if all permits acquired, false otherwise
     * @throws IllegalArgumentException if permits <= 0
     */
    boolean tryAcquire(int permits);
    
    /**
     * Returns the number of permits currently available.
     * 
     * This is a snapshot value - by the time you use it, it may have changed
     * due to concurrent access or time-based refills.
     * 
     * Use this for monitoring/metrics, not for decision making.
     * Always use tryAcquire() for actual rate limiting decisions.
     * 
     * @return Number of available permits (0 if none available)
     */
    long getAvailablePermits();
    
    /**
     * Resets the rate limiter to its initial state.
     * 
     * Clears all state (tokens, counters, timestamps, queues).
     * Useful for testing or manual intervention.
     * 
     * Thread-safe: Can be called concurrently.
     */
    void reset();
    
    /**
     * Returns the configuration used by this rate limiter.
     * 
     * @return Immutable configuration object
     */
    RateLimitConfig getConfig();
    
    /**
     * Returns the algorithm type of this rate limiter.
     * 
     * Useful for logging, monitoring, and debugging.
     * 
     * @return Algorithm type enum
     */
    AlgorithmType getAlgorithmType();
}
