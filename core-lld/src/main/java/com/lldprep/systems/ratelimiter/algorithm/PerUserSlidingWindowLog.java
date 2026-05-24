package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;

import java.util.TreeSet;

/**
 * Sliding Window Log — single-threaded, no locks.
 *
 * Keeps a log of request timestamps. Before each request, entries older than the
 * window are evicted. The current count equals the remaining log size. Since we
 * look at a true rolling window (not a fixed boundary), the boundary burst problem
 * of Fixed Window does not exist here.
 *
 * Cost: O(n) memory per user, where n = requests in the current window.
 *       O(log n) time per request due to TreeSet operations.
 *
 * Use when: accuracy is critical and traffic is low-to-medium (e.g., login attempt
 * limiting, financial transaction rate limits, security-sensitive endpoints).
 */
public class PerUserSlidingWindowLog implements PerUserAlgorithm {

    private final long maxRequests;
    private final long windowSizeNanos;
    private final TreeSet<Long> timestamps;

    // Ensures unique TreeSet keys even when System.nanoTime() returns the same value twice
    private long sequence;

    public PerUserSlidingWindowLog(RateLimitConfig config) {
        this.maxRequests = config.getMaxRequests();
        this.windowSizeNanos = config.getWindowSize().toNanos();
        this.timestamps = new TreeSet<>();
        this.sequence = 0;
    }

    @Override
    public boolean tryConsume(int permits) {
        long now = System.nanoTime();
        evictExpired(now);
        if (timestamps.size() + permits <= maxRequests) {
            for (int i = 0; i < permits; i++) {
                timestamps.add(now + sequence++);
            }
            return true;
        }
        return false;
    }

    @Override
    public long availablePermits() {
        evictExpired(System.nanoTime());
        return Math.max(0, maxRequests - timestamps.size());
    }

    @Override
    public void reset() {
        timestamps.clear();
        sequence = 0;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.SLIDING_WINDOW_LOG;
    }

    private void evictExpired(long now) {
        // headSet() returns a view of all entries strictly before the cutoff;
        // clear() on that view removes them from the original TreeSet in O(m).
        timestamps.headSet(now - windowSizeNanos).clear();
    }
}
