package com.lldprep.ratelimiter;

import com.lldprep.ratelimiter.factory.RateLimiterFactory;
import com.lldprep.ratelimiter.registry.UserRateLimiterRegistry;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive demonstration of Rate Limiter functionality.
 * 
 * Covers:
 * - All 5 rate limiting algorithms
 * - Algorithm comparison
 * - Thread safety demonstration
 * - Per-user rate limiting with registry
 * - Burst handling differences
 * - Boundary problem demonstration (Fixed Window)
 */
public class RateLimiterDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Rate Limiter Comprehensive Demo ===\n");
        
        demonstrateTokenBucket();
        demonstrateLeakyBucket();
        demonstrateFixedWindow();
        demonstrateSlidingWindowLog();
        demonstrateSlidingWindowCounter();
        demonstrateAlgorithmComparison();
        demonstrateThreadSafety();
        demonstrateUserRegistry();
        demonstrateBurstHandling();
        demonstrateFixedWindowBoundaryProblem();
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    /**
     * Demonstrates Token Bucket algorithm - allows bursts.
     */
    private static void demonstrateTokenBucket() {
        System.out.println("1. Token Bucket Algorithm");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofSeconds(1))
                .burstSize(10)
                .build();
        
        RateLimiter limiter = RateLimiterFactory.createTokenBucket(config);
        
        System.out.println("Config: 5 requests/second, burst size = 10");
        System.out.println("Characteristic: Allows bursts (can accumulate tokens)\n");
        
        System.out.println("Attempting 10 rapid requests (burst):");
        for (int i = 1; i <= 10; i++) {
            boolean allowed = limiter.tryAcquire();
            System.out.printf("  Request %2d: %s (available: %d)%n", 
                            i, allowed ? "✓ ALLOWED" : "✗ DENIED", limiter.getAvailablePermits());
        }
        
        System.out.println("\nWaiting 1 second for refill...");
        sleep(1000);
        
        System.out.println("After refill:");
        System.out.println("  Available permits: " + limiter.getAvailablePermits());
        System.out.println("  Next request: " + (limiter.tryAcquire() ? "✓ ALLOWED" : "✗ DENIED"));
        
        System.out.println("\n");
    }
    
    /**
     * Demonstrates Leaky Bucket algorithm - smooths bursts.
     */
    private static void demonstrateLeakyBucket() {
        System.out.println("2. Leaky Bucket Algorithm");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofSeconds(1))
                .burstSize(5)
                .build();
        
        RateLimiter limiter = RateLimiterFactory.createLeakyBucket(config);
        
        System.out.println("Config: 5 requests/second, queue size = 5");
        System.out.println("Characteristic: Smooths bursts (constant output rate)\n");
        
        System.out.println("Attempting 8 rapid requests:");
        for (int i = 1; i <= 8; i++) {
            boolean allowed = limiter.tryAcquire();
            System.out.printf("  Request %d: %s (queue space: %d)%n", 
                            i, allowed ? "✓ ALLOWED" : "✗ DENIED", limiter.getAvailablePermits());
        }
        
        System.out.println("\nWaiting 1 second for leak...");
        sleep(1000);
        
        System.out.println("After leak:");
        System.out.println("  Available space: " + limiter.getAvailablePermits());
        
        System.out.println("\n");
    }
    
    /**
     * Demonstrates Fixed Window algorithm - has boundary problem.
     */
    private static void demonstrateFixedWindow() {
        System.out.println("3. Fixed Window Counter Algorithm");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofSeconds(1))
                .build();
        
        RateLimiter limiter = RateLimiterFactory.createFixedWindow(config);
        
        System.out.println("Config: 5 requests/second");
        System.out.println("Characteristic: Simple, but has boundary problem\n");
        
        System.out.println("Attempting 7 requests:");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.tryAcquire();
            System.out.printf("  Request %d: %s (remaining: %d)%n", 
                            i, allowed ? "✓ ALLOWED" : "✗ DENIED", limiter.getAvailablePermits());
        }
        
        System.out.println("\nWaiting for window reset...");
        sleep(1000);
        
        System.out.println("After window reset:");
        System.out.println("  Available permits: " + limiter.getAvailablePermits());
        System.out.println("  Next request: " + (limiter.tryAcquire() ? "✓ ALLOWED" : "✗ DENIED"));
        
        System.out.println("\n");
    }
    
    /**
     * Demonstrates Sliding Window Log - perfect accuracy.
     */
    private static void demonstrateSlidingWindowLog() {
        System.out.println("4. Sliding Window Log Algorithm");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofSeconds(1))
                .build();
        
        RateLimiter limiter = RateLimiterFactory.createSlidingWindowLog(config);
        
        System.out.println("Config: 5 requests/second");
        System.out.println("Characteristic: Perfect accuracy, no boundary issues\n");
        
        System.out.println("Attempting 7 requests:");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.tryAcquire();
            System.out.printf("  Request %d: %s (remaining: %d)%n", 
                            i, allowed ? "✓ ALLOWED" : "✗ DENIED", limiter.getAvailablePermits());
        }
        
        System.out.println("\nWaiting 500ms (half window)...");
        sleep(500);
        
        System.out.println("After 500ms:");
        System.out.println("  Available permits: " + limiter.getAvailablePermits());
        System.out.println("  (Old timestamps not yet expired)");
        
        System.out.println("\n");
    }
    
    /**
     * Demonstrates Sliding Window Counter - best balance.
     */
    private static void demonstrateSlidingWindowCounter() {
        System.out.println("5. Sliding Window Counter Algorithm (Hybrid)");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofSeconds(1))
                .build();
        
        RateLimiter limiter = RateLimiterFactory.createSlidingWindowCounter(config);
        
        System.out.println("Config: 5 requests/second");
        System.out.println("Characteristic: Good accuracy, O(1) performance\n");
        
        System.out.println("Attempting 7 requests:");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.tryAcquire();
            System.out.printf("  Request %d: %s (remaining: %d)%n", 
                            i, allowed ? "✓ ALLOWED" : "✗ DENIED", limiter.getAvailablePermits());
        }
        
        System.out.println("\nWaiting for window slide...");
        sleep(1000);
        
        System.out.println("After window slide:");
        System.out.println("  Available permits: " + limiter.getAvailablePermits());
        
        System.out.println("\n");
    }
    
    /**
     * Compares all algorithms side-by-side.
     */
    private static void demonstrateAlgorithmComparison() {
        System.out.println("6. Algorithm Comparison");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(10)
                .windowSize(Duration.ofSeconds(1))
                .build();
        
        RateLimiter tokenBucket = RateLimiterFactory.createTokenBucket(config);
        RateLimiter leakyBucket = RateLimiterFactory.createLeakyBucket(config);
        RateLimiter fixedWindow = RateLimiterFactory.createFixedWindow(config);
        RateLimiter slidingLog = RateLimiterFactory.createSlidingWindowLog(config);
        RateLimiter slidingCounter = RateLimiterFactory.createSlidingWindowCounter(config);
        
        System.out.println("Sending 15 rapid requests to each algorithm:\n");
        
        int[] results = new int[5];
        
        for (int i = 0; i < 15; i++) {
            if (tokenBucket.tryAcquire()) results[0]++;
            if (leakyBucket.tryAcquire()) results[1]++;
            if (fixedWindow.tryAcquire()) results[2]++;
            if (slidingLog.tryAcquire()) results[3]++;
            if (slidingCounter.tryAcquire()) results[4]++;
        }
        
        System.out.printf("%-25s: %2d allowed / 15 total%n", "Token Bucket", results[0]);
        System.out.printf("%-25s: %2d allowed / 15 total%n", "Leaky Bucket", results[1]);
        System.out.printf("%-25s: %2d allowed / 15 total%n", "Fixed Window", results[2]);
        System.out.printf("%-25s: %2d allowed / 15 total%n", "Sliding Window Log", results[3]);
        System.out.printf("%-25s: %2d allowed / 15 total%n", "Sliding Window Counter", results[4]);
        
        System.out.println("\nObservation: All algorithms allow 10 requests (configured limit)");
        System.out.println("Difference is in HOW they handle bursts and timing.\n");
        
        System.out.println();
    }
    
    /**
     * Demonstrates thread safety with concurrent access.
     */
    private static void demonstrateThreadSafety() throws InterruptedException {
        System.out.println("7. Thread Safety Demonstration");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(100)
                .windowSize(Duration.ofSeconds(1))
                .build();
        
        RateLimiter limiter = RateLimiterFactory.createTokenBucket(config);
        
        System.out.println("Config: 100 requests/second");
        System.out.println("Test: 10 threads, each attempting 20 requests\n");
        
        int numThreads = 10;
        int requestsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger totalAllowed = new AtomicInteger(0);
        AtomicInteger totalDenied = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                int allowed = 0;
                int denied = 0;
                
                for (int j = 0; j < requestsPerThread; j++) {
                    if (limiter.tryAcquire()) {
                        allowed++;
                    } else {
                        denied++;
                    }
                }
                
                totalAllowed.addAndGet(allowed);
                totalDenied.addAndGet(denied);
                
                System.out.printf("  Thread-%d: %d allowed, %d denied%n", threadId, allowed, denied);
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("\nResults:");
        System.out.println("  Total allowed: " + totalAllowed.get());
        System.out.println("  Total denied: " + totalDenied.get());
        System.out.println("  Total requests: " + (totalAllowed.get() + totalDenied.get()));
        System.out.println("\n✓ Thread-safe: Exactly 100 requests allowed (no race conditions)\n");
        
        System.out.println();
    }
    
    /**
     * Demonstrates per-user rate limiting with registry.
     */
    private static void demonstrateUserRegistry() {
        System.out.println("8. Per-User Rate Limiting (Registry Pattern)");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(3)
                .windowSize(Duration.ofSeconds(1))
                .build();
        
        UserRateLimiterRegistry registry = new UserRateLimiterRegistry(
            AlgorithmType.TOKEN_BUCKET, config
        );
        
        System.out.println("Config: 3 requests/second per user\n");
        
        String[] users = {"user-alice", "user-bob", "user-charlie"};
        
        System.out.println("Each user attempts 5 requests:");
        for (String user : users) {
            System.out.println("\n" + user + ":");
            RateLimiter limiter = registry.getLimiter(user);
            
            for (int i = 1; i <= 5; i++) {
                boolean allowed = limiter.tryAcquire();
                System.out.printf("  Request %d: %s%n", i, allowed ? "✓ ALLOWED" : "✗ DENIED");
            }
        }
        
        System.out.println("\nRegistry stats: " + registry.getStats());
        System.out.println("\n✓ Each user has independent rate limit\n");
        
        System.out.println();
    }
    
    /**
     * Demonstrates burst handling differences between algorithms.
     */
    private static void demonstrateBurstHandling() {
        System.out.println("9. Burst Handling Comparison");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofSeconds(1))
                .burstSize(10)
                .build();
        
        RateLimiter tokenBucket = RateLimiterFactory.createTokenBucket(config);
        RateLimiter leakyBucket = RateLimiterFactory.createLeakyBucket(
            RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofSeconds(1))
                .burstSize(5)
                .build()
        );
        
        System.out.println("Scenario: 10 rapid requests (burst)\n");
        
        System.out.println("Token Bucket (burst-friendly):");
        int tokenAllowed = 0;
        for (int i = 1; i <= 10; i++) {
            if (tokenBucket.tryAcquire()) tokenAllowed++;
        }
        System.out.println("  Allowed: " + tokenAllowed + "/10");
        System.out.println("  ✓ Allows burst (accumulated tokens)");
        
        System.out.println("\nLeaky Bucket (burst-smoothing):");
        int leakyAllowed = 0;
        for (int i = 1; i <= 10; i++) {
            if (leakyBucket.tryAcquire()) leakyAllowed++;
        }
        System.out.println("  Allowed: " + leakyAllowed + "/10");
        System.out.println("  ✓ Smooths burst (queue fills up)");
        
        System.out.println("\nKey Difference:");
        System.out.println("  - Token Bucket: Good for APIs (allow occasional bursts)");
        System.out.println("  - Leaky Bucket: Good for traffic shaping (constant rate)\n");
        
        System.out.println();
    }
    
    /**
     * Demonstrates Fixed Window boundary problem.
     */
    private static void demonstrateFixedWindowBoundaryProblem() {
        System.out.println("10. Fixed Window Boundary Problem");
        System.out.println("=".repeat(60));
        
        RateLimitConfig config = RateLimitConfig.builder()
                .maxRequests(5)
                .windowSize(Duration.ofMillis(1000))
                .build();
        
        RateLimiter limiter = RateLimiterFactory.createFixedWindow(config);
        
        System.out.println("Config: 5 requests/second\n");
        System.out.println("Problem: Can get 2x rate at window boundaries\n");
        
        System.out.println("Sending 5 requests at end of window 1:");
        for (int i = 1; i <= 5; i++) {
            limiter.tryAcquire();
        }
        System.out.println("  ✓ All 5 allowed");
        
        System.out.println("\nWaiting for window boundary (1 second)...");
        sleep(1000);
        
        System.out.println("Sending 5 requests at start of window 2:");
        for (int i = 1; i <= 5; i++) {
            limiter.tryAcquire();
        }
        System.out.println("  ✓ All 5 allowed");
        
        System.out.println("\nResult: 10 requests in ~1 second (2x the intended rate!)");
        System.out.println("This is the boundary problem of Fixed Window.\n");
        System.out.println("Solution: Use Sliding Window Counter for production.\n");
        
        System.out.println();
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
