package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;

import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sliding Window Log Rate Limiter Implementation.
 * 
 * Algorithm:
 * - Keep a log (sorted set) of all request timestamps
 * - For each request, remove timestamps older than window size
 * - Count remaining timestamps
 * - Allow if count < limit
 * 
 * Characteristics:
 * - Time Complexity: O(log n) for timestamp cleanup (TreeSet operations)
 * - Space Complexity: O(n) where n = requests in window
 * - Allows bursts: No (precise enforcement)
 * - Thread-safe: Yes (using ReentrantLock)
 * - Accuracy: Perfect (no boundary issues)
 * 
 * Advantages:
 * - Most accurate rate limiting
 * - No boundary problem (unlike Fixed Window)
 * - Precise sliding window
 * 
 * Disadvantages:
 * - High memory usage (stores all timestamps)
 * - Slower than other algorithms (O(log n) vs O(1))
 * - Not suitable for very high traffic
 * 
 * Use Cases:
 * - Security (login attempt limiting)
 * - Critical systems requiring precision
 * - Low to medium traffic scenarios
 * 
 * Thread Safety Strategy:
 * - ReentrantLock for all operations
 * - TreeSet is not thread-safe, so all access must be synchronized
 */
public class SlidingWindowLogRateLimiter implements RateLimiter {
    
    private final RateLimitConfig config;
    private final ReentrantLock lock;
    
    // CRITICAL SECTION - shared mutable state
    // TreeSet maintains sorted order for efficient range operations
    private final TreeSet<Long> timestamps;
    
    public SlidingWindowLogRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.lock = new ReentrantLock();
        this.timestamps = new TreeSet<>();
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
            long now = System.nanoTime();
            removeOldTimestamps(now);
            
            if (timestamps.size() + permits <= config.getMaxRequests()) {
                // Add 'permits' number of timestamps
                for (int i = 0; i < permits; i++) {
                    // Use slightly different timestamps to avoid duplicates
                    timestamps.add(now + i);
                }
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
            long now = System.nanoTime();
            removeOldTimestamps(now);
            return Math.max(0, config.getMaxRequests() - timestamps.size());
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void reset() {
        lock.lock();
        try {
            timestamps.clear();
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
        return AlgorithmType.SLIDING_WINDOW_LOG;
    }
    
    /**
     * Removes timestamps older than the sliding window.
     * 
     * Uses TreeSet.headSet() to efficiently get all timestamps before cutoff,
     * then clears them.
     * 
     * Time Complexity: O(m) where m = timestamps to remove
     * 
     * MUST be called within lock to ensure thread safety.
     */
    private void removeOldTimestamps(long now) {
        long windowSizeNanos = config.getWindowSize().toNanos();
        long cutoffTime = now - windowSizeNanos;
        
        // headSet returns view of elements < cutoffTime
        // Clearing the view removes elements from the original set
        timestamps.headSet(cutoffTime).clear();
    }
}
