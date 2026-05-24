package com.lldprep.systems.ratelimiter;

import com.lldprep.systems.ratelimiter.algorithm.StripedExecutorRateLimiter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the Striped Executor Rate Limiter with pluggable algorithms.
 *
 * The striped executor pattern is orthogonal to the rate limiting algorithm:
 * - PATTERN:   Thread Confinement — each user's requests run on a dedicated thread,
 *              so the algorithm needs no locks.
 * - ALGORITHM: Token Bucket, Leaky Bucket, Fixed Window, Sliding Window Log/Counter —
 *              any of the five can be swapped in via a static factory method.
 *
 * SCENARIOS:
 *   1. Token Bucket  — allows bursts; each user has an independent quota
 *   2. Leaky Bucket  — enforces a constant output rate; bursts are flattened
 *   3. Sliding Window Counter — production-grade accuracy with O(1) memory
 */
public class StripedRateLimiterDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║     Striped Executor Rate Limiter — Pluggable Algorithms        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");

        demonstrateTokenBucket();
        demonstrateLeakyBucket();
        demonstrateSlidingWindowCounter();

        System.out.println("All scenarios completed.");
    }

    // ── Scenario 1: Token Bucket ──────────────────────────────────────────────

    /**
     * Token Bucket allows bursts up to the configured burst capacity.
     * User A exhausts the burst quickly; User B is unaffected (per-user isolation).
     */
    static void demonstrateTokenBucket() throws Exception {
        System.out.println("SCENARIO 1: Token Bucket — per-user burst isolation");
        System.out.println("════════════════════════════════════════════════════");
        System.out.println("Config: 5 req/s, burst capacity 5 per user\n");

        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(java.time.Duration.ofSeconds(1))
                .burstSize(5)
                .build();

        StripedExecutorRateLimiter limiter = StripedExecutorRateLimiter.withTokenBucket(config);

        System.out.println("User A sending 10 rapid requests (burst capacity = 5)...");
        int allowedA = fireRequests(limiter, "user-a", 10);
        System.out.printf("  User A: %d/10 allowed (burst exhausted after 5)%n%n", allowedA);

        System.out.println("User B sending 3 requests (should all pass — isolated from User A)...");
        int allowedB = fireRequests(limiter, "user-b", 3);
        System.out.printf("  User B: %d/3 allowed%n", allowedB);

        System.out.println("\n  " + limiter.getUserStats("user-a"));
        System.out.println("  " + limiter.getUserStats("user-b"));

        limiter.shutdown();
        System.out.println("\n" + "─".repeat(65) + "\n");
    }

    // ── Scenario 2: Leaky Bucket ──────────────────────────────────────────────

    /**
     * Leaky Bucket enforces a constant output rate — no burst tolerance.
     * Even the first few requests above the queue capacity are denied immediately.
     * Contrast with Token Bucket where the first N requests always succeed.
     */
    static void demonstrateLeakyBucket() throws Exception {
        System.out.println("SCENARIO 2: Leaky Bucket — constant rate enforcement");
        System.out.println("════════════════════════════════════════════════════");
        System.out.println("Config: 5 req/s, queue capacity 5\n");

        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(java.time.Duration.ofSeconds(1))
                .burstSize(5)
                .build();

        StripedExecutorRateLimiter limiter = StripedExecutorRateLimiter.withLeakyBucket(config);

        System.out.println("Spammer sending 20 rapid requests...");
        int spamAllowed = fireRequests(limiter, "spammer", 20);
        System.out.printf("  spammer: %d/20 allowed (queue full after %d)%n%n",
                spamAllowed, config.getBurstSize());

        System.out.println("Good user sending 3 requests concurrently with spammer...");
        int goodAllowed = fireRequests(limiter, "good-user", 3);
        System.out.printf("  good-user: %d/3 allowed (independent queue — unaffected)%n", goodAllowed);

        System.out.println("\n  Key difference from Token Bucket: Leaky Bucket has NO burst tolerance.");
        System.out.println("  Requests above queue capacity are dropped immediately, not queued.");

        System.out.println("\n  " + limiter.getStats());

        limiter.shutdown();
        System.out.println("\n" + "─".repeat(65) + "\n");
    }

    // ── Scenario 3: Sliding Window Counter ───────────────────────────────────

    /**
     * Sliding Window Counter approximates a true rolling window using two fixed windows.
     * It avoids the boundary-burst problem of Fixed Window while using O(1) memory.
     *
     * This scenario runs 100 threads against the same user to demonstrate
     * that the striped pattern handles high concurrency with low latency.
     */
    static void demonstrateSlidingWindowCounter() throws Exception {
        System.out.println("SCENARIO 3: Sliding Window Counter — high-concurrency accuracy test");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("Config: 100 req/s, 100 threads hitting the same user simultaneously\n");

        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(100)
                .windowSize(java.time.Duration.ofSeconds(1))
                .burstSize(100)
                .build();

        StripedExecutorRateLimiter limiter = StripedExecutorRateLimiter.withSlidingWindowCounter(config);

        int threadCount = 100;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger allowed   = new AtomicInteger(0);
        long[] latencies         = new long[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    startGate.await();
                    long start = System.nanoTime();
                    if (limiter.tryAcquire("hot-user")) allowed.incrementAndGet();
                    latencies[idx] = System.nanoTime() - start;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        long testStart = System.currentTimeMillis();
        startGate.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStart;

        long min = Long.MAX_VALUE, max = Long.MIN_VALUE, sum = 0;
        for (long lat : latencies) {
            min = Math.min(min, lat);
            max = Math.max(max, lat);
            sum += lat;
        }

        System.out.printf("  Completed in %d ms%n", testDuration);
        System.out.printf("  Allowed: %d/%d%n", allowed.get(), threadCount);
        System.out.printf("  Latency (µs): min=%d, avg=%d, max=%d%n",
                min / 1000, (sum / threadCount) / 1000, max / 1000);
        System.out.println("\n  Why Sliding Window Counter beats Fixed Window here:");
        System.out.println("  At window boundaries, Fixed Window can let 2× the limit through.");
        System.out.println("  Sliding Window Counter blends prev/current windows → no boundary spike.");

        System.out.println("\n  " + limiter.getUserStats("hot-user"));

        limiter.shutdown();
    }

    // ── Shared helper ─────────────────────────────────────────────────────────

    /** Fires n async requests for a user and returns how many were allowed. */
    private static int fireRequests(StripedExecutorRateLimiter limiter,
                                    String userId, int count) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(count);
        AtomicInteger allowed = new AtomicInteger(0);

        for (int i = 0; i < count; i++) {
            limiter.tryAcquireAsync(userId).thenAccept(permit -> {
                if (permit) allowed.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await(3, TimeUnit.SECONDS);
        return allowed.get();
    }
}
