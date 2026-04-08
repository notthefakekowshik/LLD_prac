package com.lldprep.ratelimiter.algorithm;

import com.lldprep.ratelimiter.AlgorithmType;
import com.lldprep.ratelimiter.RateLimitConfig;
import com.lldprep.ratelimiter.RateLimiter;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Leaky Bucket Rate Limiter Implementation.
 * 
 * Algorithm:
 * - Requests enter a bucket (queue)
 * - Requests "leak" out at a fixed rate
 * - If bucket is full, new requests are denied
 * - Enforces constant output rate (smooths bursts)
 * 
 * Characteristics:
 * - Time Complexity: O(1) amortized
 * - Space Complexity: O(n) where n = queue size
 * - Allows bursts: No (smooths all bursts)
 * - Thread-safe: Yes (using ReentrantLock)
 * 
 * Difference from Token Bucket:
 * - Token Bucket: Allows immediate bursts if tokens available
 * - Leaky Bucket: Enforces constant rate, smooths all bursts
 * 
 * Use Cases:
 * - Traffic shaping (network routers)
 * - Smoothing bursty traffic
 * - Background job processing with constant throughput
 * 
 * Thread Safety Strategy:
 * - Uses ReentrantLock for atomic queue operations
 * - Leak operation must be atomic with queue size check
 */
public class LeakyBucketRateLimiter implements RateLimiter {
    
    private final RateLimitConfig config;
    private final ReentrantLock lock;
    
    // CRITICAL SECTION - shared mutable state
    private final Queue<Long> requestQueue;
    private long lastLeakTime;
    
    public LeakyBucketRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.lock = new ReentrantLock();
        this.requestQueue = new LinkedList<>();
        this.lastLeakTime = System.nanoTime();
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
            leak();
            
            // Check if we can add 'permits' number of requests
            if (requestQueue.size() + permits <= config.getBurstSize()) {
                long now = System.nanoTime();
                for (int i = 0; i < permits; i++) {
                    requestQueue.offer(now);
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
            leak();
            return config.getBurstSize() - requestQueue.size();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void reset() {
        lock.lock();
        try {
            requestQueue.clear();
            lastLeakTime = System.nanoTime();
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
        return AlgorithmType.LEAKY_BUCKET;
    }
    
    /**
     * Leaks (processes) requests from the queue based on elapsed time.
     * 
     * Formula:
     *   requestsToLeak = elapsedTime * leakRate
     * 
     * MUST be called within lock to ensure thread safety.
     */
    private void leak() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastLeakTime;
        
        if (elapsedNanos <= 0) {
            return;
        }
        
        double requestsToLeak = elapsedNanos * config.getLeakRatePerNano();
        int leakCount = (int) Math.min(requestsToLeak, requestQueue.size());
        
        for (int i = 0; i < leakCount; i++) {
            requestQueue.poll();
        }
        
        lastLeakTime = now;
    }
}
