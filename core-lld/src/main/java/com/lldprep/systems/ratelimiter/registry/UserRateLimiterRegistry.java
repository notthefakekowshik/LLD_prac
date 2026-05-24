package com.lldprep.systems.ratelimiter.registry;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;
import com.lldprep.systems.ratelimiter.factory.RateLimiterFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Registry Pattern: Manages rate limiters for multiple users/resources.
 *
 * Creation logic is fully externalized via a limiterFactory function,
 * so callers control per-user algorithm and config (e.g. free vs premium tiers).
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

    // Why: sensible out-of-the-box default so callers don't have to specify a factory
    private static final Function<String, RateLimiter> DEFAULT_FACTORY = userId ->
            RateLimiterFactory.create(
                    AlgorithmType.TOKEN_BUCKET,
                    RateLimitConfig.builder()
                            .maxRequests(100)
                            .windowSize(Duration.ofSeconds(1))
                            .build()
            );

    private final ConcurrentHashMap<String, RateLimiter> limiters;
    private final Function<String, RateLimiter> limiterFactory;

    private UserRateLimiterRegistry(Builder builder) {
        this.limiters = new ConcurrentHashMap<>();
        this.limiterFactory = builder.limiterFactory;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets or creates a rate limiter for the specified user.
     * Creation is delegated to the configured limiterFactory.
     */
    public RateLimiter getLimiter(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        return limiters.computeIfAbsent(userId, limiterFactory);
    }

    public boolean hasLimiter(String userId) {
        return limiters.containsKey(userId);
    }

    public boolean removeLimiter(String userId) {
        return limiters.remove(userId) != null;
    }

    public boolean resetLimiter(String userId) {
        RateLimiter limiter = limiters.get(userId);
        if (limiter != null) {
            limiter.reset();
            return true;
        }
        return false;
    }

    public void clear() {
        limiters.clear();
    }

    public int size() {
        return limiters.size();
    }

    public String getStats() {
        return String.format("UserRateLimiterRegistry{users=%d}", limiters.size());
    }

    public static class Builder {

        private Function<String, RateLimiter> limiterFactory = DEFAULT_FACTORY;

        /**
         * Provides a function that maps a userId to a RateLimiter.
         * Use this to implement per-tier or per-user policies.
         *
         * Example:
         *   .limiterFactory(userId -> isPremium(userId)
         *       ? RateLimiterFactory.createTokenBucket(premiumConfig)
         *       : RateLimiterFactory.createFixedWindow(freeConfig))
         */
        public Builder limiterFactory(Function<String, RateLimiter> factory) {
            if (factory == null) {
                throw new IllegalArgumentException("limiterFactory cannot be null");
            }
            this.limiterFactory = factory;
            return this;
        }

        public UserRateLimiterRegistry build() {
            return new UserRateLimiterRegistry(this);
        }
    }
}
