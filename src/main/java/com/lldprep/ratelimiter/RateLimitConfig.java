package com.lldprep.ratelimiter;

import java.time.Duration;

/**
 * Immutable configuration for rate limiters.
 * 
 * Uses Builder pattern for clean construction with validation.
 * All fields are final to ensure thread-safety and immutability.
 */
public final class RateLimitConfig {
    
    private final long maxRequests;
    private final Duration windowSize;
    private final int burstSize;
    
    private RateLimitConfig(Builder builder) {
        this.maxRequests = builder.maxRequests;
        this.windowSize = builder.windowSize;
        this.burstSize = builder.burstSize;
    }
    
    public long getMaxRequests() {
        return maxRequests;
    }
    
    public Duration getWindowSize() {
        return windowSize;
    }
    
    public int getBurstSize() {
        return burstSize;
    }
    
    /**
     * Calculates the refill rate in permits per nanosecond.
     * 
     * Used by Token Bucket and Leaky Bucket algorithms.
     * 
     * @return Permits per nanosecond
     */
    public double getRefillRatePerNano() {
        return (double) maxRequests / windowSize.toNanos();
    }
    
    /**
     * Calculates the leak rate in permits per nanosecond.
     * 
     * Used by Leaky Bucket algorithm.
     * 
     * @return Permits per nanosecond
     */
    public double getLeakRatePerNano() {
        return (double) maxRequests / windowSize.toNanos();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return String.format("RateLimitConfig{maxRequests=%d, windowSize=%s, burstSize=%d}",
                           maxRequests, windowSize, burstSize);
    }
    
    /**
     * Builder for RateLimitConfig.
     * 
     * Provides fluent API with validation.
     */
    public static class Builder {
        
        private long maxRequests;
        private Duration windowSize;
        private int burstSize;
        
        /**
         * Sets the maximum number of requests allowed in the window.
         * 
         * Required parameter.
         * 
         * @param maxRequests Maximum requests (must be > 0)
         * @return Builder instance
         */
        public Builder maxRequests(long maxRequests) {
            if (maxRequests <= 0) {
                throw new IllegalArgumentException("maxRequests must be positive, got: " + maxRequests);
            }
            this.maxRequests = maxRequests;
            return this;
        }
        
        /**
         * Sets the time window size.
         * 
         * Required parameter.
         * 
         * @param windowSize Window duration (must be positive)
         * @return Builder instance
         */
        public Builder windowSize(Duration windowSize) {
            if (windowSize == null || windowSize.isNegative() || windowSize.isZero()) {
                throw new IllegalArgumentException("windowSize must be positive, got: " + windowSize);
            }
            this.windowSize = windowSize;
            return this;
        }
        
        /**
         * Sets the burst size (bucket capacity for Token Bucket).
         * 
         * Optional parameter. Defaults to maxRequests if not set.
         * 
         * Burst size determines how many tokens can accumulate.
         * Higher burst = more tolerance for traffic spikes.
         * 
         * @param burstSize Burst capacity (must be >= maxRequests)
         * @return Builder instance
         */
        public Builder burstSize(int burstSize) {
            if (burstSize <= 0) {
                throw new IllegalArgumentException("burstSize must be positive, got: " + burstSize);
            }
            this.burstSize = burstSize;
            return this;
        }
        
        /**
         * Builds the immutable RateLimitConfig.
         * 
         * Validates that required parameters are set.
         * Sets default burst size if not specified.
         * 
         * @return Configured RateLimitConfig instance
         */
        public RateLimitConfig build() {
            if (maxRequests <= 0) {
                throw new IllegalStateException("maxRequests must be set");
            }
            if (windowSize == null) {
                throw new IllegalStateException("windowSize must be set");
            }
            
            if (burstSize == 0) {
                burstSize = (int) maxRequests;
            }
            
            if (burstSize < maxRequests) {
                throw new IllegalArgumentException(
                    "burstSize (" + burstSize + ") must be >= maxRequests (" + maxRequests + ")"
                );
            }
            
            return new RateLimitConfig(this);
        }
    }
}
