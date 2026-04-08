package com.kowshik.patterns;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Rate Limiter Demo - Token Bucket Algorithm Implementation
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. Rate Limiting Algorithms:
 *    - Token Bucket: Tokens added at fixed rate, consumed per request
 *    - Leaky Bucket: Requests processed at fixed rate
 *    - Fixed Window: Count requests in fixed time windows
 *    - Sliding Window Log: More accurate but memory intensive
 *
 * 2. Common Interview Questions:
 *    - How to implement a rate limiter?
 *    - Explain token bucket algorithm
 *    - Distributed rate limiting challenges?
 *    - How to handle burst traffic?
 *    - Rate limiter in API gateway?
 *    - Redis-based rate limiting?
 *
 * 3. Token Bucket Algorithm:
 *    - Bucket holds tokens (capacity = bucket size)
 *    - Tokens added at fixed rate
 *    - Each request consumes one token
 *    - If no tokens, request rejected or waits
 *    - Allows bursts up to bucket size
 *
 * 4. Use Cases:
 *    - API rate limiting (GitHub, Twitter APIs)
 *    - Request throttling
 *    - Traffic shaping
 *    - DDoS protection
 *    - QoS (Quality of Service)
 *
 * 5. Implementation Considerations:
 *    - Thread-safe token management
 *    - Accurate timing
 *    - Minimal overhead
 *    - Graceful degradation
 *
 * Implements a thread-safe rate limiter using the token bucket algorithm.
 * Features:
 * - Precise rate limiting
 * - Burst handling
 * - Non-blocking operations
 * - Adaptive rate adjustment
 */
public class RateLimiter {
    private final long ratePerSecond;
    private final AtomicLong lastRefillTime;
    private final AtomicInteger availableTokens;
    private final int bucketSize;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong totalRequests;
    private final AtomicLong rejectedRequests;

    /**
     * Creates a rate limiter with specified rate.
     *
     * @param ratePerSecond maximum requests per second
     */
    public RateLimiter(long ratePerSecond) {
        this(ratePerSecond, (int) ratePerSecond);
    }

    /**
     * Creates a rate limiter with specified rate and bucket size.
     *
     * @param ratePerSecond maximum requests per second
     * @param bucketSize    maximum burst size
     */
    public RateLimiter(long ratePerSecond, int bucketSize) {
        this.ratePerSecond = ratePerSecond;
        this.bucketSize = bucketSize;
        this.availableTokens = new AtomicInteger(bucketSize);
        this.lastRefillTime = new AtomicLong(System.nanoTime());
        this.totalRequests = new AtomicLong(0);
        this.rejectedRequests = new AtomicLong(0);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "rate-limiter-refill");
            thread.setDaemon(true);
            return thread;
        });

        // Schedule periodic token refill
        scheduler.scheduleAtFixedRate(
            this::refillTokens,
            0,
            Math.max(1, 1000 / ratePerSecond),
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Attempts to acquire a token without blocking.
     *
     * @return true if token acquired, false otherwise
     */
    public boolean tryAcquire() {
        totalRequests.incrementAndGet();

        while (true) {
            int currentTokens = availableTokens.get();
            if (currentTokens <= 0) {
                rejectedRequests.incrementAndGet();
                return false;
            }

            if (availableTokens.compareAndSet(currentTokens, currentTokens - 1)) {
                return true;
            }
        }
    }

    /**
     * Acquires a token, blocking if necessary.
     * Uses spin-wait strategy.
     */
    public void acquire() {
        while (!tryAcquire()) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        }
    }

    /**
     * Refills tokens based on elapsed time.
     */
    private void refillTokens() {
        long now = System.nanoTime();
        long timePassed = now - lastRefillTime.get();

        // Calculate tokens to add based on time passed
        long tokensToAdd = TimeUnit.NANOSECONDS.toSeconds(timePassed) * ratePerSecond;

        if (tokensToAdd > 0) {
            int newTokens = (int) Math.min(bucketSize, availableTokens.get() + tokensToAdd);
            availableTokens.set(newTokens);
            lastRefillTime.set(now);
        }
    }

    /**
     * Shuts down the rate limiter.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Gets current statistics.
     *
     * @return rate limiter statistics
     */
    public RateLimiterStats getStats() {
        return new RateLimiterStats(
            totalRequests.get(),
            rejectedRequests.get(),
            availableTokens.get()
        );
    }

    /**
     * Statistics holder for rate limiter metrics.
     */
    public static class RateLimiterStats {
        private final long totalRequests;
        private final long rejectedRequests;
        private final int availableTokens;

        RateLimiterStats(long totalRequests, long rejectedRequests, int availableTokens) {
            this.totalRequests = totalRequests;
            this.rejectedRequests = rejectedRequests;
            this.availableTokens = availableTokens;
        }

        @Override
        public String toString() {
            return String.format(
                "RateLimiterStats{total=%d, rejected=%d, available=%d, acceptance=%.2f%%}",
                totalRequests, rejectedRequests, availableTokens,
                totalRequests > 0 ? 100.0 * (totalRequests - rejectedRequests) / totalRequests : 0.0
            );
        }
    }

    /**
     * Example usage demonstrating rate limiter in action.
     *
     * @param args command line arguments
     * @throws InterruptedException if interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        // Create a rate limiter with 10 requests per second
        RateLimiter limiter = new RateLimiter(10);

        System.out.println("Rate Limiter Demo: 10 requests/second");
        System.out.println("Submitting 50 requests rapidly...\n");

        // Create multiple threads trying to acquire tokens
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 50; i++) {
            final int requestId = i;
            executor.submit(() -> {
                if (limiter.tryAcquire()) {
                    System.out.printf("Request %d: ACCEPTED at %d ms%n",
                        requestId, System.currentTimeMillis() % 100000);
                } else {
                    System.out.printf("Request %d: REJECTED at %d ms%n",
                        requestId, System.currentTimeMillis() % 100000);
                }
            });
            Thread.sleep(50); // Submit requests every 50ms (20 requests/second attempt)
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\nFinal stats: " + limiter.getStats());
        limiter.shutdown();
    }
}
