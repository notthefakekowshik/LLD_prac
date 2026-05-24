package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;

/**
 * Token Bucket — single-threaded, no locks.
 *
 * Tokens accumulate over time up to a burst capacity. Each request consumes one
 * token; requests are denied when the bucket is empty. Saved-up tokens let users
 * absorb short traffic spikes before being throttled.
 *
 * Use when: bursty traffic is acceptable (e.g., REST API quotas, cloud service limits).
 */
public class PerUserTokenBucket implements PerUserAlgorithm {

    private final double capacity;
    private final double refillRatePerNano;

    private double tokens;
    private long lastRefillTime;

    public PerUserTokenBucket(RateLimitConfig config) {
        this.capacity = config.getBurstSize();
        this.refillRatePerNano = config.getRefillRatePerNano();
        this.tokens = capacity;
        this.lastRefillTime = System.nanoTime();
    }

    @Override
    public boolean tryConsume(int permits) {
        refill();
        if (tokens >= permits) {
            tokens -= permits;
            return true;
        }
        return false;
    }

    @Override
    public long availablePermits() {
        refill();
        return (long) tokens;
    }

    @Override
    public void reset() {
        tokens = capacity;
        lastRefillTime = System.nanoTime();
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.TOKEN_BUCKET;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillTime;
        if (elapsed <= 0) return;
        tokens = Math.min(tokens + elapsed * refillRatePerNano, capacity);
        lastRefillTime = now;
    }
}
