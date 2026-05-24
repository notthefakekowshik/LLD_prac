package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;

/**
 * Contract for a single-threaded rate limiting algorithm.
 *
 * Designed exclusively for thread confinement: a single instance is accessed
 * by exactly one thread (the user's dedicated executor inside StripedExecutorRateLimiter).
 * Because only one thread ever touches an instance, no locks or atomics are needed
 * in any implementation.
 *
 * IMPORTANT: Implementations MUST NOT be thread-safe. Thread safety is the
 * responsibility of the caller (StripedExecutorRateLimiter), not the algorithm.
 * Keeping synchronization out of the algorithm makes it simpler, faster, and easier
 * to reason about.
 *
 * Contrast with RateLimiter (the other interface in this package), which IS
 * thread-safe and can be used standalone. PerUserAlgorithm is a lower-level
 * building block meant to be wrapped by StripedExecutorRateLimiter.
 */
public interface PerUserAlgorithm {

    /**
     * Attempts to consume the given number of permits.
     *
     * @param permits number of permits to consume (must be > 0)
     * @return true if permits were granted, false if rate limit exceeded
     */
    boolean tryConsume(int permits);

    /**
     * Returns the approximate number of currently available permits.
     *
     * Intended for monitoring — not for making rate-limiting decisions.
     * Always use tryConsume() for actual allow/deny logic.
     */
    long availablePermits();

    /** Resets all algorithm state as if no requests have ever been made. */
    void reset();

    AlgorithmType getAlgorithmType();
}
