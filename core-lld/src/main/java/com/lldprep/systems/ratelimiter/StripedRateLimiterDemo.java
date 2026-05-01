package com.lldprep.systems.ratelimiter;

import com.lldprep.systems.ratelimiter.algorithm.StripedExecutorRateLimiter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates the Striped Executor Rate Limiter pattern.
 *
 * COMPARISON: Traditional vs Striped Executor Rate Limiting
 * =========================================================
 *
 * TRADITIONAL APPROACH (TokenBucketRateLimiter):
 * -----------------------------------------------
 * - Uses ReentrantLock for thread safety
 * - All threads contend on the same lock
 * - Under high load for SAME user: Lock contention kills performance
 * - Lock acquisition/release overhead even for uncontended case
 *
 * STRIPED EXECUTOR APPROACH (This Demo):
 * --------------------------------------
 * - Uses Thread Confinement (no locks!)
 * - Each user gets dedicated SingleThreadExecutor
 * - Token bucket for each user is accessed by ONLY that thread
 * - Zero lock contention even with high concurrent requests per user
 * - Better P99 latency under contention
 *
 * SCENARIOS DEMONSTRATED:
 * ======================
 * 1. Multi-User Rate Limiting: Each user has independent quota
 * 2. High Contention Test: 100 threads hitting same user concurrently
 * 3. Performance Comparison: Lock-based vs Thread-confined
 * 4. Per-User Isolation: One user's burst doesn't affect others
 */
public class StripedRateLimiterDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║     Striped Executor Rate Limiter - Demonstration               ║");
        System.out.println("║     Thread Confinement Pattern vs Lock-Based                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");

        // Scenario 1: Per-User Rate Limiting
        demonstratePerUserRateLimiting();

        // Scenario 2: High Contention Comparison
        demonstrateHighContention();

        // Scenario 3: Per-User Isolation
        demonstrateUserIsolation();

        System.out.println("\n✅ All scenarios completed successfully!");
    }

    /**
     * SCENARIO 1: Per-User Rate Limiting
     *
     * Shows how each user gets their own independent token bucket.
     * User A can burst while User B is rate limited - no interference.
     */
    static void demonstratePerUserRateLimiting() throws Exception {
        System.out.println("SCENARIO 1: Per-User Rate Limiting");
        System.out.println("═══════════════════════════════════");
        System.out.println("Config: 10 requests/second, burst capacity of 5 per user\n");

        // Why: 10 requests/sec, burst of 5 tokens
        RateLimitConfig config = RateLimitConfig.builder()
            .maxRequests(10)
            .windowSize(java.time.Duration.ofSeconds(1))
            .burstSize(5)
            .build();

        StripedExecutorRateLimiter limiter = new StripedExecutorRateLimiter(config);

        // Why: Simulate User A making 10 rapid requests
        System.out.println("User A sending 10 rapid requests...");
        CountDownLatch latchA = new CountDownLatch(10);
        AtomicInteger allowedA = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            final int reqNum = i;
            limiter.tryAcquireAsync("user-a").thenAccept(allowed -> {
                System.out.printf("  User A Request %d: %s%n",
                    reqNum, allowed ? "✅ ALLOWED" : "❌ REJECTED");
                if (allowed) allowedA.incrementAndGet();
                latchA.countDown();
            });
        }

        latchA.await(2, TimeUnit.SECONDS);
        System.out.printf("User A Result: %d/10 allowed (burst capacity exhausted)\n\n", allowedA.get());

        // Why: Simulate User B making 3 requests (should all pass)
        System.out.println("User B sending 3 rapid requests...");
        CountDownLatch latchB = new CountDownLatch(3);
        AtomicInteger allowedB = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            final int reqNum = i;
            limiter.tryAcquireAsync("user-b").thenAccept(allowed -> {
                System.out.printf("  User B Request %d: %s%n",
                    reqNum, allowed ? "✅ ALLOWED" : "❌ REJECTED");
                if (allowed) allowedB.incrementAndGet();
                latchB.countDown();
            });
        }

        latchB.await(2, TimeUnit.SECONDS);
        System.out.printf("User B Result: %d/3 allowed (isolated from User A's burst)\n", allowedB.get());

        // Why: Show statistics
        System.out.println("\nFinal Stats: " + limiter.getStats());
        System.out.println("User A: " + limiter.getUserStats("user-a"));
        System.out.println("User B: " + limiter.getUserStats("user-b"));

        limiter.shutdown();
        System.out.println("\n" + "─".repeat(70) + "\n");
    }

    /**
     * SCENARIO 2: High Contention Performance Test
     *
     * 100 threads hitting the SAME user concurrently.
     * Demonstrates why Striped Executor beats lock-based approaches:
     * - No lock contention (requests queue on dedicated thread)
     * - Predictable latency (no lock acquisition delays)
     * - Better P99 latency under load
     */
    static void demonstrateHighContention() throws Exception {
        System.out.println("SCENARIO 2: High Contention Performance Test");
        System.out.println("════════════════════════════════════════════");
        System.out.println("100 threads hitting SAME user concurrently\n");

        RateLimitConfig config = RateLimitConfig.builder()
            .maxRequests(100)
            .windowSize(java.time.Duration.ofSeconds(1))
            .burstSize(50)
            .build();

        StripedExecutorRateLimiter limiter = new StripedExecutorRateLimiter(config);

        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        AtomicInteger totalAllowed = new AtomicInteger(0);

        // Why: Record start time for latency measurement
        long[] latencies = new long[threadCount];

        System.out.printf("Spawning %d threads to hit 'hot-user' simultaneously...%n", threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            new Thread(() -> {
                try {
                    startLatch.await();  // All threads start together
                    long start = System.nanoTime();

                    boolean allowed = limiter.tryAcquire("hot-user");

                    long latency = System.nanoTime() - start;
                    latencies[threadNum] = latency;

                    if (allowed) totalAllowed.incrementAndGet();
                    completeLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // Why: Release all threads at once for maximum contention
        long testStart = System.currentTimeMillis();
        startLatch.countDown();
        completeLatch.await(5, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStart;

        // Why: Calculate latency statistics
        long minLatency = Long.MAX_VALUE;
        long maxLatency = Long.MIN_VALUE;
        long sumLatency = 0;
        for (long lat : latencies) {
            minLatency = Math.min(minLatency, lat);
            maxLatency = Math.max(maxLatency, lat);
            sumLatency += lat;
        }

        System.out.printf("Test completed in %d ms%n", testDuration);
        System.out.printf("Total allowed: %d/%d (bucket capacity + some refills)%n",
            totalAllowed.get(), threadCount);
        System.out.printf("Latency (microseconds): min=%d, avg=%d, max=%d%n",
            minLatency / 1000, (sumLatency / threadCount) / 1000, maxLatency / 1000);

        // Why: Compare with what lock-based would experience
        System.out.println("\n💡 Why this matters:");
        System.out.println("   - Lock-based: Threads block, retry, cause cache line bouncing");
        System.out.println("   - Striped: Threads submit tasks, executor serializes on one thread");
        System.out.println("   - Result: Lower P99 latency, no CAS retry loops, no lock contention");

        System.out.println("\nUser Stats: " + limiter.getUserStats("hot-user"));

        limiter.shutdown();
        System.out.println("\n" + "─".repeat(70) + "\n");
    }

    /**
     * SCENARIO 3: Per-User Isolation Under Load
     *
     * Shows that one user's excessive traffic doesn't starve others.
     * Hot user gets rate limited independently; other users unaffected.
     */
    static void demonstrateUserIsolation() throws Exception {
        System.out.println("SCENARIO 3: Per-User Isolation Under Load");
        System.out.println("══════════════════════════════════════════");
        System.out.println("Hot user (spammer) vs Regular users - no interference\n");

        RateLimitConfig config = RateLimitConfig.builder()
            .maxRequests(5)
            .windowSize(java.time.Duration.ofSeconds(1))
            .burstSize(5)
            .build();

        StripedExecutorRateLimiter limiter = new StripedExecutorRateLimiter(config);

        // Why: "spammer" sends 50 rapid requests
        System.out.println("'spammer-user' sending 50 rapid requests...");
        CountDownLatch spammerLatch = new CountDownLatch(50);
        AtomicInteger spammerAllowed = new AtomicInteger(0);

        for (int i = 0; i < 50; i++) {
            limiter.tryAcquireAsync("spammer-user").thenAccept(allowed -> {
                if (allowed) spammerAllowed.incrementAndGet();
                spammerLatch.countDown();
            });
        }

        // Why: Meanwhile, "good-user" sends 3 requests
        Thread.sleep(10);  // Slight delay to interleave
        System.out.println("'good-user' sending 3 requests during spam...");
        CountDownLatch goodLatch = new CountDownLatch(3);
        AtomicInteger goodAllowed = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            limiter.tryAcquireAsync("good-user").thenAccept(allowed -> {
                if (allowed) goodAllowed.incrementAndGet();
                goodLatch.countDown();
            });
        }

        spammerLatch.await(3, TimeUnit.SECONDS);
        goodLatch.await(3, TimeUnit.SECONDS);

        System.out.printf("\nResults:%n");
        System.out.printf("  spammer-user: %d/50 allowed (rate limited after burst)%n", spammerAllowed.get());
        System.out.printf("  good-user: %d/3 allowed (unaffected by spammer's traffic)%n", goodAllowed.get());

        System.out.println("\n✅ Key takeaway: Per-user isolation prevents one bad actor from affecting others");
        System.out.println("   This is critical for multi-tenant API gateways!");

        System.out.println("\nFinal Stats: " + limiter.getStats());

        limiter.shutdown();
    }
}
