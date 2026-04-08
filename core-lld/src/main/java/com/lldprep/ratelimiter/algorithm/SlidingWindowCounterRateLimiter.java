package com.lldprep.ratelimiter.algorithm;

import com.lldprep.ratelimiter.AlgorithmType;
import com.lldprep.ratelimiter.RateLimitConfig;
import com.lldprep.ratelimiter.RateLimiter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sliding Window Counter Rate Limiter Implementation (Hybrid Approach).
 * 
 * Algorithm:
 * - Combines Fixed Window Counter with weighted calculation
 * - Uses current window count + weighted previous window count
 * - Approximates true sliding window behavior
 * 
 * Formula:
 *   estimatedCount = (prevWindowCount * overlapPercentage) + currentWindowCount
 *   
 *   where:
 *     overlapPercentage = (windowSize - elapsedTimeInCurrentWindow) / windowSize
 * 
 * Example:
 *   Window size: 60s, Max: 100
 *   Current time: 30s into current window
 *   Previous window: 80 requests
 *   Current window: 40 requests
 *   
 *   Overlap: (60 - 30) / 60 = 50%
 *   Estimated: (80 * 0.5) + 40 = 80 requests
 *   If 80 < 100: ALLOW
 * 
 * Characteristics:
 * - Time Complexity: O(1)
 * - Space Complexity: O(1)
 * - Allows bursts: Yes (but controlled)
 * - Thread-safe: Yes (using AtomicLong + ReentrantLock)
 * - Accuracy: High (better than Fixed Window, cheaper than Sliding Window Log)
 * 
 * Advantages:
 * - No boundary problem (unlike Fixed Window)
 * - Low memory usage (unlike Sliding Window Log)
 * - Good accuracy approximation
 * - O(1) performance
 * 
 * Use Cases:
 * - Production rate limiting (best balance)
 * - High-traffic APIs
 * - Cloud services (Cloudflare, AWS use similar approach)
 * 
 * Thread Safety Strategy:
 * - AtomicLong for counters (lock-free increments)
 * - ReentrantLock for window transitions (atomic multi-field update)
 */
public class SlidingWindowCounterRateLimiter implements RateLimiter {
    
    private final RateLimitConfig config;
    private final ReentrantLock lock;
    
    // CRITICAL SECTION - shared mutable state
    private volatile long currentWindowStart;
    private final AtomicLong currentWindowCount;
    private final AtomicLong previousWindowCount;
    
    public SlidingWindowCounterRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.lock = new ReentrantLock();
        this.currentWindowStart = System.nanoTime();
        this.currentWindowCount = new AtomicLong(0);
        this.previousWindowCount = new AtomicLong(0);
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
        
        slideWindowIfNeeded();
        
        long estimatedCount = getEstimatedCount();
        
        if (estimatedCount + permits <= config.getMaxRequests()) {
            currentWindowCount.addAndGet(permits);
            return true;
        }
        
        return false;
    }
    
    @Override
    public long getAvailablePermits() {
        slideWindowIfNeeded();
        long estimatedCount = getEstimatedCount();
        return Math.max(0, config.getMaxRequests() - estimatedCount);
    }
    
    @Override
    public void reset() {
        lock.lock();
        try {
            currentWindowStart = System.nanoTime();
            currentWindowCount.set(0);
            previousWindowCount.set(0);
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
        return AlgorithmType.SLIDING_WINDOW_COUNTER;
    }
    
    /**
     * Calculates estimated request count using weighted previous window.
     * 
     * This is the core of the sliding window approximation.
     * 
     * Formula:
     *   estimatedCount = (prevCount * overlapPercentage) + currentCount
     * 
     * Example visualization:
     * 
     *   Previous Window    Current Window
     *   [------------]     [------------]
     *                      ^
     *                      30s into window
     *   
     *   Overlap = last 30s of previous window
     *   Weight = 30/60 = 0.5
     */
    private long getEstimatedCount() {
        long now = System.nanoTime();
        long windowSizeNanos = config.getWindowSize().toNanos();
        long elapsedInCurrentWindow = now - currentWindowStart;
        
        // Calculate overlap percentage with previous window
        double overlapPercentage = 1.0 - ((double) elapsedInCurrentWindow / windowSizeNanos);
        overlapPercentage = Math.max(0, Math.min(1, overlapPercentage));
        
        long weightedPreviousCount = (long) (previousWindowCount.get() * overlapPercentage);
        long currentCount = currentWindowCount.get();
        
        return weightedPreviousCount + currentCount;
    }
    
    /**
     * Slides the window forward if current time has passed window boundary.
     * 
     * Transitions:
     * - Previous window count = current window count
     * - Current window count = 0
     * - Window start = now
     */
    private void slideWindowIfNeeded() {
        long now = System.nanoTime();
        long windowSizeNanos = config.getWindowSize().toNanos();
        
        // Fast path: no slide needed (volatile read)
        if (now < currentWindowStart + windowSizeNanos) {
            return;
        }
        
        // Slow path: need to slide window
        lock.lock();
        try {
            // Double-check after acquiring lock
            if (now >= currentWindowStart + windowSizeNanos) {
                // Slide the window
                previousWindowCount.set(currentWindowCount.get());
                currentWindowCount.set(0);
                currentWindowStart = now;
            }
        } finally {
            lock.unlock();
        }
    }
}
