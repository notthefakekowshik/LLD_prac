package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Striped Executor Rate Limiter - Thread Confinement Pattern.
 *
 * PROBLEM WITH TRADITIONAL RATE LIMITERS:
 * ---------------------------------------
 * Traditional rate limiters use locks (ReentrantLock) or atomics (CAS) for thread safety.
 * Under high contention for the SAME user, this creates performance issues:
 * - Lock contention: Threads block waiting for the lock
 * - CAS retries: Atomic operations spin-retry under contention, wasting CPU
 * - Cache line bouncing: Atomic variables ping-pong between CPU cores
 *
 * SOLUTION: Thread Confinement via Striped Executor
 * -------------------------------------------------
 * Instead of synchronizing access to shared state, we eliminate sharing entirely:
 * - Each user gets a dedicated SingleThreadExecutor
 * - All requests for user X run on user X's dedicated thread
 * - Token bucket for user X is accessed ONLY by that thread = NO LOCKS NEEDED
 *
 * Architecture:
 * ```
 * User A requests ──┐
 * User A requests ──┼──► [Executor for User A] ──► [TokenBucket A] ──► Allow/Deny
 * User A requests ──┘         (1 thread)            (no locks!)
 *
 * User B requests ──┐
 * User B requests ──┼──► [Executor for User B] ──► [TokenBucket B] ──► Allow/Deny
 * User B requests ──┘         (1 thread)            (no locks!)
 *
 * Result: Zero lock contention. Each user's bucket is single-threaded.
 * ```
 *
 * Trade-offs:
 * - PRO: Zero lock contention even at high throughput
 * - PRO: Natural per-user isolation (different users can't interfere)
 * - PRO: Simpler token bucket implementation (no synchronization)
 * - CON: O(users) threads - memory overhead per thread (~1MB stack)
 * - CON: Not suitable for unlimited users (use bounded variant for that)
 * - CON: Thread context switching overhead
 *
 * Best For:
 * - Per-user API rate limiting with bounded user count (< 10,000)
 * - High-contention scenarios (many requests for same user)
 * - Systems where predictable latency matters (P99 optimization)
 *
 * Algorithm: Token Bucket per user
 * - Each user has independent token bucket
 * - Tokens refill at configured rate per user
 * - Burst capacity configurable per user
 */
public class StripedExecutorRateLimiter implements RateLimiter {

    // Why: Maps userId to their dedicated executor
    // Thread-safe: ConcurrentHashMap for concurrent access
    private final ConcurrentHashMap<String, ExecutorService> userExecutors;

    // Why: Maps userId to their token bucket state
    // Accessed ONLY by the user's dedicated thread - no synchronization needed!
    private final ConcurrentHashMap<String, PerUserTokenBucket> userBuckets;

    private final RateLimitConfig config;

    // Why: Metrics tracking (thread-safe counters)
    private final LongAdder totalRequests;
    private final LongAdder allowedRequests;
    private final LongAdder rejectedRequests;

    /**
     * Creates a striped executor rate limiter.
     *
     * @param config Rate limit configuration (applied per user)
     */
    public StripedExecutorRateLimiter(RateLimitConfig config) {
        this.config = config;
        this.userExecutors = new ConcurrentHashMap<>();
        this.userBuckets = new ConcurrentHashMap<>();
        this.totalRequests = new LongAdder();
        this.allowedRequests = new LongAdder();
        this.rejectedRequests = new LongAdder();
    }

    /**
     * Attempts to acquire a permit for the specified user.
     *
     * Execution Flow:
     * 1. Route request to user's dedicated executor (based on userId)
     * 2. On that thread, check token bucket (no locks needed!)
     * 3. Return CompletableFuture with result
     *
     * Why CompletableFuture: Rate limiting decision is asynchronous
     * because it runs on the user's dedicated thread.
     *
     * @param userId User identifier (the stripe key)
     * @return Future containing true if allowed, false if rate limited
     */
    public CompletableFuture<Boolean> tryAcquireAsync(String userId) {
        totalRequests.increment();

        // Why: Get or create dedicated executor for this user
        // computeIfAbsent ensures atomic lazy initialization
        ExecutorService executor = userExecutors.computeIfAbsent(userId, this::createUserExecutor);

        // Why: Submit task to user's dedicated thread
        // All operations for this user serialize on this thread
        return CompletableFuture.supplyAsync(() -> {
            // Why: Get or create token bucket for this user
            // Only THIS thread ever accesses this bucket = NO LOCKS NEEDED
            PerUserTokenBucket bucket = userBuckets.computeIfAbsent(
                userId,
                uid -> new PerUserTokenBucket(config.getBurstSize(), config.getRefillRatePerNano())
            );

            // Why: Single-threaded access = no synchronization required
            boolean allowed = bucket.tryConsume(1);

            if (allowed) {
                allowedRequests.increment();
            } else {
                rejectedRequests.increment();
            }

            return allowed;
        }, executor);
    }

    /**
     * Synchronous version - blocks until rate limiting decision is made.
     *
     * Use this for simple cases where async isn't needed.
     * Still benefits from thread confinement (no lock contention).
     *
     * @param userId User identifier
     * @return true if allowed, false if rate limited
     */
    public boolean tryAcquire(String userId) {
        try {
            return tryAcquireAsync(userId).get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @Override
    public boolean tryAcquire() {
        // Why: Default user for global rate limiting interface
        return tryAcquire("default");
    }

    @Override
    public boolean tryAcquire(int permits) {
        // Why: Multi-permit acquisition for weighted requests
        totalRequests.add(permits);

        ExecutorService executor = userExecutors.computeIfAbsent("default", this::createUserExecutor);

        try {
            return CompletableFuture.supplyAsync(() -> {
                PerUserTokenBucket bucket = userBuckets.computeIfAbsent(
                    "default",
                    uid -> new PerUserTokenBucket(config.getBurstSize(), config.getRefillRatePerNano())
                );

                boolean allowed = bucket.tryConsume(permits);

                if (allowed) {
                    allowedRequests.add(permits);
                } else {
                    rejectedRequests.add(permits);
                }

                return allowed;
            }, executor).get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @Override
    public long getAvailablePermits() {
        PerUserTokenBucket bucket = userBuckets.get("default");
        return bucket != null ? (long) bucket.getAvailableTokens() : config.getBurstSize();
    }

    /**
     * Gets available permits for a specific user.
     *
     * @param userId User identifier
     * @return Available tokens for the user
     */
    public long getAvailablePermits(String userId) {
        PerUserTokenBucket bucket = userBuckets.get(userId);
        return bucket != null ? (long) bucket.getAvailableTokens() : config.getBurstSize();
    }

    @Override
    public void reset() {
        PerUserTokenBucket bucket = userBuckets.get("default");
        if (bucket != null) {
            bucket.reset(config.getBurstSize());
        }
    }

    /**
     * Resets rate limiter for a specific user.
     *
     * @param userId User to reset
     */
    public void reset(String userId) {
        PerUserTokenBucket bucket = userBuckets.get(userId);
        if (bucket != null) {
            bucket.reset(config.getBurstSize());
        }
    }

    @Override
    public RateLimitConfig getConfig() {
        return config;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return AlgorithmType.TOKEN_BUCKET;
    }

    /**
     * Returns statistics about this rate limiter.
     *
     * @return Statistics snapshot
     */
    public Stats getStats() {
        return new Stats(
            totalRequests.sum(),
            allowedRequests.sum(),
            rejectedRequests.sum(),
            userExecutors.size()
        );
    }

    /**
     * Returns per-user statistics.
     *
     * @param userId User identifier
     * @return User statistics or null if user not found
     */
    public UserStats getUserStats(String userId) {
        PerUserTokenBucket bucket = userBuckets.get(userId);
        if (bucket == null) {
            return null;
        }

        return new UserStats(
            userId,
            (long) bucket.getAvailableTokens(),
            bucket.getTotalRequests(),
            bucket.getAllowedRequests(),
            bucket.getRejectedRequests()
        );
    }

    /**
     * Shuts down all user executors.
     * Call this on application shutdown.
     */
    public void shutdown() {
        userExecutors.values().forEach(ExecutorService::shutdown);
    }

    /**
     * Creates a dedicated single-thread executor for a user.
     *
     * Why SingleThreadExecutor:
     * - Guarantees sequential execution for that user's requests
     * - No lock contention on the token bucket
     * - Thread name helps with debugging/profiling
     *
     * @param userId User identifier
     * @return New single-thread executor
     */
    private ExecutorService createUserExecutor(String userId) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-" + userId);
            t.setDaemon(true);
            return t;
        });
    }

    // ============== Statistics Classes ==============

    public static class Stats {
        public final long totalRequests;
        public final long allowedRequests;
        public final long rejectedRequests;
        public final int activeUsers;

        Stats(long total, long allowed, long rejected, int users) {
            this.totalRequests = total;
            this.allowedRequests = allowed;
            this.rejectedRequests = rejected;
            this.activeUsers = users;
        }

        @Override
        public String toString() {
            return String.format(
                "StripedRateLimiter{total=%d, allowed=%d, rejected=%d, users=%d, rate=%.1f%%}",
                totalRequests, allowedRequests, rejectedRequests, activeUsers,
                totalRequests > 0 ? 100.0 * allowedRequests / totalRequests : 0.0
            );
        }
    }

    public static class UserStats {
        public final String userId;
        public final long availableTokens;
        public final long totalRequests;
        public final long allowedRequests;
        public final long rejectedRequests;

        UserStats(String userId, long tokens, long total, long allowed, long rejected) {
            this.userId = userId;
            this.availableTokens = tokens;
            this.totalRequests = total;
            this.allowedRequests = allowed;
            this.rejectedRequests = rejected;
        }

        @Override
        public String toString() {
            return String.format(
                "UserStats{user=%s, tokens=%d, total=%d, allowed=%d, rejected=%d}",
                userId, availableTokens, totalRequests, allowedRequests, rejectedRequests
            );
        }
    }

    // ============== Per-User Token Bucket (No Locks!) ==============

    /**
     * Single-threaded token bucket - NO SYNCHRONIZATION NEEDED.
     *
     * CRITICAL: This class is designed for single-threaded access only.
     * The StripedExecutor guarantees all access to a user's bucket happens
     * on that user's dedicated thread.
     *
     * Why no locks:
     * - Thread confinement eliminates need for synchronization
     * - Better performance than lock-based or CAS-based approaches
     * - Simpler code (no lock/unlock, no retry loops)
     */
    private static class PerUserTokenBucket {
        private final double capacity;
        private final double refillRatePerNano;

        // Why: These are accessed by only ONE thread - no volatile/atomic needed!
        private double tokens;
        private long lastRefillTime;

        // Why: Simple counters for metrics (only accessed by one thread)
        private long totalRequests;
        private long allowedRequests;
        private long rejectedRequests;

        PerUserTokenBucket(double capacity, double refillRatePerNano) {
            this.capacity = capacity;
            this.refillRatePerNano = refillRatePerNano;
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
        }

        /**
         * Attempts to consume tokens from the bucket.
         *
         * No synchronization needed - called from single thread only.
         *
         * @param permits Number of tokens to consume
         * @return true if consumed, false if not enough tokens
         */
        boolean tryConsume(int permits) {
            totalRequests++;

            // Why: Refill based on elapsed time
            refill();

            if (tokens >= permits) {
                tokens -= permits;
                allowedRequests++;
                return true;
            }

            rejectedRequests++;
            return false;
        }

        /**
         * Returns current available tokens.
         *
         * @return Available tokens (refills first)
         */
        double getAvailableTokens() {
            refill();
            return tokens;
        }

        /**
         * Resets bucket to initial state.
         *
         * @param initialTokens Starting token count
         */
        void reset(double initialTokens) {
            tokens = initialTokens;
            lastRefillTime = System.nanoTime();
            totalRequests = 0;
            allowedRequests = 0;
            rejectedRequests = 0;
        }

        long getTotalRequests() {
            return totalRequests;
        }

        long getAllowedRequests() {
            return allowedRequests;
        }

        long getRejectedRequests() {
            return rejectedRequests;
        }

        /**
         * Refills tokens based on elapsed time.
         *
         * No synchronization needed - called from single thread only.
         */
        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillTime;

            if (elapsedNanos <= 0) {
                return;
            }

            double tokensToAdd = elapsedNanos * refillRatePerNano;
            tokens = Math.min(tokens + tokensToAdd, capacity);
            lastRefillTime = now;
        }
    }
}
