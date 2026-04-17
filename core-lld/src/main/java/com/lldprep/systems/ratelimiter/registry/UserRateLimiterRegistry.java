package com.lldprep.systems.ratelimiter.registry;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;
import com.lldprep.systems.ratelimiter.factory.RateLimiterFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry Pattern: Manages rate limiters for multiple users/resources.
 * 
 * Features:
 * - Per-user rate limiting
 * - Lazy initialization (creates limiter on first access)
 * - Thread-safe using ConcurrentHashMap
 * - Easy cleanup of inactive limiters
 * 
 * Use Cases:
 * - API rate limiting per user/API key
 * - Resource quotas per tenant
 * - Per-IP rate limiting
 * 
 * Thread Safety:
 * - ConcurrentHashMap for thread-safe map operations
 * - computeIfAbsent ensures atomic lazy initialization
 */
public class UserRateLimiterRegistry {
    
    private final ConcurrentHashMap<String, RateLimiter> limiters;
    private final AlgorithmType defaultAlgorithm;
    private final RateLimitConfig defaultConfig;
    
    /**
     * Creates a registry with default algorithm and configuration.
     * 
     * @param defaultAlgorithm Algorithm to use for new limiters
     * @param defaultConfig Configuration for new limiters
     */
    public UserRateLimiterRegistry(AlgorithmType defaultAlgorithm, RateLimitConfig defaultConfig) {
        this.limiters = new ConcurrentHashMap<>();
        this.defaultAlgorithm = defaultAlgorithm;
        this.defaultConfig = defaultConfig;
    }
    
    /**
     * Gets or creates a rate limiter for the specified user.
     * 
     * Thread-safe: Uses computeIfAbsent for atomic lazy initialization.
     * 
     * @param userId User identifier (e.g., user ID, API key, IP address)
     * @return RateLimiter for the user
     */
    public RateLimiter getLimiter(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        return limiters.computeIfAbsent(userId, id -> 
            RateLimiterFactory.create(defaultAlgorithm, defaultConfig)
        );
    }
    
    /**
     * Gets or creates a rate limiter with custom configuration.
     * 
     * @param userId User identifier
     * @param algorithm Algorithm type for this user
     * @param config Configuration for this user
     * @return RateLimiter for the user
     */
    public RateLimiter getLimiter(String userId, AlgorithmType algorithm, RateLimitConfig config) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        return limiters.computeIfAbsent(userId, id -> 
            RateLimiterFactory.create(algorithm, config)
        );
    }
    
    /**
     * Checks if a limiter exists for the user.
     * 
     * @param userId User identifier
     * @return true if limiter exists
     */
    public boolean hasLimiter(String userId) {
        return limiters.containsKey(userId);
    }
    
    /**
     * Removes the rate limiter for the specified user.
     * 
     * Useful for cleanup when user is inactive or deleted.
     * 
     * @param userId User identifier
     * @return true if limiter was removed, false if didn't exist
     */
    public boolean removeLimiter(String userId) {
        return limiters.remove(userId) != null;
    }
    
    /**
     * Resets the rate limiter for the specified user.
     * 
     * @param userId User identifier
     * @return true if limiter was reset, false if didn't exist
     */
    public boolean resetLimiter(String userId) {
        RateLimiter limiter = limiters.get(userId);
        if (limiter != null) {
            limiter.reset();
            return true;
        }
        return false;
    }
    
    /**
     * Clears all rate limiters.
     * 
     * Useful for testing or system-wide reset.
     */
    public void clear() {
        limiters.clear();
    }
    
    /**
     * Returns the number of active rate limiters.
     * 
     * @return Number of users with active limiters
     */
    public int size() {
        return limiters.size();
    }
    
    /**
     * Returns statistics about the registry.
     * 
     * @return Human-readable statistics
     */
    public String getStats() {
        return String.format("UserRateLimiterRegistry{users=%d, algorithm=%s, config=%s}",
                           limiters.size(), defaultAlgorithm, defaultConfig);
    }
}
