package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;

/**
 * Fixed Window Counter — single-threaded, no locks.
 *
 * Divides time into fixed-length windows and counts requests per window.
 * The counter resets to zero at each window boundary.
 *
 * Known trade-off — the boundary burst problem:
 *   If the limit is 100 req/min, a user can fire 100 at t=59s (end of window 1)
 *   and 100 more at t=61s (start of window 2), producing 200 requests in 2 seconds.
 *   The sliding window algorithms below solve this.
 *
 * Use when: simplicity matters more than precision, or traffic is naturally spread
 * evenly (e.g., daily/hourly analytics quotas, coarse-grained batch limits).
 */
public class PerUserFixedWindow implements PerUserAlgorithm {

    private final long maxRequests;
    private final long windowSizeNanos;
    private long windowStart;
    private long count;

    public PerUserFixedWindow(RateLimitConfig config) {
        this.maxRequests = config.getMaxRequests();
        this.windowSizeNanos = config.getWindowSize().toNanos();
        this.windowStart = System.nanoTime();
        this.count = 0;
    }

    @Override
    public boolean tryConsume(int permits) {
        resetWindowIfExpired();
        if (count + permits <= maxRequests) {
            count += permits;
            return true;
        }
        return false;
    }

    @Override
    public long availablePermits() {
        resetWindowIfExpired();
        return Math.max(0, maxRequests - count);
    }

    @Override
    public void reset() {
        windowStart = System.nanoTime();
        count = 0;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.FIXED_WINDOW;
    }

    private void resetWindowIfExpired() {
        long now = System.nanoTime();
        if (now >= windowStart + windowSizeNanos) {
            windowStart = now;
            count = 0;
        }
    }
}
