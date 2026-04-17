package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fixed Window Counter Rate Limiter Implementation.
 * 
 * Algorithm:
 * - Divide time into fixed windows (e.g., 1-minute windows)
 * - Count requests in current window
 * - Reset counter at window boundary
 * - Deny requests if counter >= limit
 * 
 * Characteristics:
 * - Time Complexity: O(1)
 * - Space Complexity: O(1)
 * - Allows bursts: Yes (at window boundaries)
 * - Thread-safe: Yes (using AtomicLong)
 * 
 * BOUNDARY PROBLEM:
 * At window edges, can get 2x the intended rate.
 * 
 * Example:
 *   Window 1: [0s - 60s], limit = 100
 *   Window 2: [60s - 120s], limit = 100
 *   
 *   At 59s: 100 requests (allowed)
 *   At 61s: 100 requests (allowed)
 *   Result: 200 requests in 2 seconds! (2x rate)
 * 
 * Use Cases:
 * - Simple rate limiting where precision isn't critical
 * - Analytics (requests per hour/day)
 * - Low-memory scenarios
 * 
 * Thread Safety Strategy:
 * - AtomicLong for counter (simple increment/decrement)
 * - ReentrantLock for window reset (need to atomically check time + reset)
 */
public class FixedWindowRateLimiter implements RateLimiter {
    
    private final RateLimitConfig config;
    private final ReentrantLock lock;
    
    // CRITICAL SECTION - shared mutable state
    private volatile long windowStart;
    private final AtomicLong requestCount;
    
    public FixedWindowRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.lock = new ReentrantLock();
        this.windowStart = System.nanoTime();
        this.requestCount = new AtomicLong(0);
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
        
        resetWindowIfNeeded();
        
        // Optimistic increment
        long newCount = requestCount.addAndGet(permits);
        
        if (newCount <= config.getMaxRequests()) {
            return true;
        }
        
        // Rollback if exceeded
        requestCount.addAndGet(-permits);
        return false;
    }
    
    @Override
    public long getAvailablePermits() {
        resetWindowIfNeeded();
        long current = requestCount.get();
        return Math.max(0, config.getMaxRequests() - current);
    }
    
    @Override
    public void reset() {
        lock.lock();
        try {
            windowStart = System.nanoTime();
            requestCount.set(0);
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
        return AlgorithmType.FIXED_WINDOW;
    }
    
    /**
     * Resets the window if current time has passed the window boundary.
     * 
     * Uses lock to ensure atomic check-and-reset.
     */
    private void resetWindowIfNeeded() {
        long now = System.nanoTime();
        long windowSizeNanos = config.getWindowSize().toNanos();
        
        // Fast path: no reset needed (volatile read)
        if (now < windowStart + windowSizeNanos) {
            return;
        }
        
        // Slow path: need to reset
        lock.lock();
        try {
            // Double-check after acquiring lock
            if (now >= windowStart + windowSizeNanos) {
                windowStart = now;
                requestCount.set(0);
            }
        } finally {
            lock.unlock();
        }
    }
}
