package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;

/**
 * Sliding Window Counter — single-threaded, no locks.
 *
 * Approximates a true sliding window by blending the current window's count with
 * a time-weighted fraction of the previous window's count:
 *
 *   estimated = (previousCount × overlapRatio) + currentCount
 *
 * where overlapRatio = 1 − (elapsed / windowSize).
 *
 * Example — window = 60s, limit = 100:
 *   At t=45s into the current window (75% elapsed):
 *     overlapRatio = 1 − 0.75 = 0.25  →  last 25% of the previous window counts
 *     previous = 80, current = 30
 *     estimated = (80 × 0.25) + 30 = 50  →  ALLOW
 *
 * This eliminates the hard boundary problem of Fixed Window while using O(1) memory
 * instead of O(n) like Sliding Window Log.
 *
 * Use when: you need the best accuracy-to-cost trade-off for production rate limiting.
 * This approach (or a close variant) is used by Cloudflare, AWS, and most cloud providers.
 */
public class PerUserSlidingWindowCounter implements PerUserAlgorithm {

    private final long maxRequests;
    private final long windowSizeNanos;
    private long currentWindowStart;
    private long currentCount;
    private long previousCount;

    public PerUserSlidingWindowCounter(RateLimitConfig config) {
        this.maxRequests = config.getMaxRequests();
        this.windowSizeNanos = config.getWindowSize().toNanos();
        this.currentWindowStart = System.nanoTime();
        this.currentCount = 0;
        this.previousCount = 0;
    }

    @Override
    public boolean tryConsume(int permits) {
        slideIfNeeded();
        if (estimatedCount() + permits <= maxRequests) {
            currentCount += permits;
            return true;
        }
        return false;
    }

    @Override
    public long availablePermits() {
        slideIfNeeded();
        return Math.max(0, maxRequests - estimatedCount());
    }

    @Override
    public void reset() {
        currentWindowStart = System.nanoTime();
        currentCount = 0;
        previousCount = 0;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.SLIDING_WINDOW_COUNTER;
    }

    private long estimatedCount() {
        long elapsed = System.nanoTime() - currentWindowStart;
        double overlapRatio = Math.max(0.0, 1.0 - ((double) elapsed / windowSizeNanos));
        return (long) (previousCount * overlapRatio) + currentCount;
    }

    private void slideIfNeeded() {
        long now = System.nanoTime();
        if (now >= currentWindowStart + windowSizeNanos) {
            previousCount = currentCount;
            currentCount = 0;
            currentWindowStart = now;
        }
    }
}
