package com.lldprep.systems.ratelimiter.factory;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;
import com.lldprep.systems.ratelimiter.algorithm.*;

/**
 * Factory Pattern: Creates RateLimiter instances based on algorithm type.
 * 
 * Benefits:
 * - Centralized creation logic
 * - Easy to add new algorithm types
 * - Encapsulates construction complexity
 * - Follows Open/Closed Principle
 * 
 * Usage:
 *   RateLimiter limiter = RateLimiterFactory.create(
 *       AlgorithmType.TOKEN_BUCKET,
 *       config
 *   );
 */
public class RateLimiterFactory {
    
    /**
     * Creates a rate limiter based on the specified algorithm type.
     * 
     * @param algorithmType Type of rate limiting algorithm
     * @param config Configuration for the rate limiter
     * @return Configured RateLimiter instance
     * @throws IllegalArgumentException if algorithmType is null or unknown
     */
    public static RateLimiter create(AlgorithmType algorithmType, RateLimitConfig config) {
        if (algorithmType == null) {
            throw new IllegalArgumentException("Algorithm type cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        
        switch (algorithmType) {
            case TOKEN_BUCKET:
                return createTokenBucket(config);
            case LEAKY_BUCKET:
                return createLeakyBucket(config);
            case FIXED_WINDOW:
                return createFixedWindow(config);
            case SLIDING_WINDOW_LOG:
                return createSlidingWindowLog(config);
            case SLIDING_WINDOW_COUNTER:
                return createSlidingWindowCounter(config);
            default:
                throw new IllegalArgumentException("Unknown algorithm type: " + algorithmType);
        }
    }
    
    /**
     * Creates a Token Bucket rate limiter.
     * 
     * Best for: API rate limiting, allowing burst traffic
     */
    public static RateLimiter createTokenBucket(RateLimitConfig config) {
        return new TokenBucketRateLimiter(config);
    }
    
    /**
     * Creates a Leaky Bucket rate limiter.
     * 
     * Best for: Traffic shaping, smoothing bursts
     */
    public static RateLimiter createLeakyBucket(RateLimitConfig config) {
        return new LeakyBucketRateLimiter(config);
    }
    
    /**
     * Creates a Fixed Window rate limiter.
     * 
     * Best for: Simple scenarios, low memory usage
     * Warning: Has boundary issue (2x rate at window edges)
     */
    public static RateLimiter createFixedWindow(RateLimitConfig config) {
        return new FixedWindowRateLimiter(config);
    }
    
    /**
     * Creates a Sliding Window Log rate limiter.
     * 
     * Best for: High accuracy requirements, security
     * Warning: High memory usage
     */
    public static RateLimiter createSlidingWindowLog(RateLimitConfig config) {
        return new SlidingWindowLogRateLimiter(config);
    }
    
    /**
     * Creates a Sliding Window Counter rate limiter.
     * 
     * Best for: Production use, high traffic
     * Recommended: Best balance of accuracy and performance
     */
    public static RateLimiter createSlidingWindowCounter(RateLimitConfig config) {
        return new SlidingWindowCounterRateLimiter(config);
    }
}
