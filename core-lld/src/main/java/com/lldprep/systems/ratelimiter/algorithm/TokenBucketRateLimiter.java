package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Token Bucket Rate Limiter Implementation.
 * 
 * Algorithm:
 * - Bucket holds tokens (permits)
 * - Tokens are added at a fixed rate (refill rate)
 * - Each request consumes one token
 * - If no tokens available, request is denied
 * - Tokens accumulate up to bucket capacity (allows bursts)
 * 
 * Characteristics:
 * - Time Complexity: O(1)
 * - Space Complexity: O(1)
 * - Allows bursts: Yes (can accumulate tokens)
 * - Thread-safe: Yes (using ReentrantLock)
 * 
 * Use Cases:
 * - API rate limiting (e.g., 1000 requests/minute)
 * - Cloud service quotas (AWS, GCP)
 * - Allowing burst traffic while maintaining average rate
 * 
 * Thread Safety Strategy:
 * - Uses ReentrantLock because we need to atomically:
 *   1. Calculate elapsed time
 *   2. Refill tokens
 *   3. Check and consume tokens
 * - All three operations must be atomic to prevent race conditions
 */
public class TokenBucketRateLimiter implements RateLimiter {
    
    private final RateLimitConfig config;
    private final ReentrantLock lock;
    
    // CRITICAL SECTION - shared mutable state
    private double tokens;
    private long lastRefillTime;
    
    public TokenBucketRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.lock = new ReentrantLock();
        this.tokens = config.getBurstSize();
        this.lastRefillTime = System.nanoTime();
    }
    
    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }
    
    @Override
    public boolean tryAcquire(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("Permits must be positive, got: " + permits);
        }
        
        lock.lock();
        try {
            refill();
            
            if (tokens >= permits) {
                tokens -= permits;
                return true;
            }
            
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public long getAvailablePermits() {
        lock.lock();
        try {
            refill();
            return (long) tokens;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void reset() {
        lock.lock();
        try {
            tokens = config.getBurstSize();
            lastRefillTime = System.nanoTime();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public RateLimitConfig getConfig() {
        return config;
    }
    
    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.TOKEN_BUCKET;
    }
    
    /**
     * Refills tokens based on elapsed time since last refill.
     * 
     * Formula:
     *   tokensToAdd = elapsedTime * refillRate
     *   newTokens = min(currentTokens + tokensToAdd, capacity)
     * 
     * MUST be called within lock to ensure thread safety.
     */
    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillTime;
        
        if (elapsedNanos <= 0) {
            return;
        }
        
        double tokensToAdd = elapsedNanos * config.getRefillRatePerNano();
        tokens = Math.min(tokens + tokensToAdd, config.getBurstSize());
        lastRefillTime = now;
    }
}
