package com.lldprep.systems.ratelimiter.algorithm;

import com.lldprep.systems.ratelimiter.AlgorithmType;
import com.lldprep.systems.ratelimiter.RateLimitConfig;
import com.lldprep.systems.ratelimiter.RateLimiter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * Striped Executor Rate Limiter — Thread Confinement Pattern.
 *
 * CORE IDEA:
 * Instead of synchronizing access to shared per-user state (locks, CAS), we
 * eliminate sharing entirely. Each user gets a dedicated SingleThreadExecutor,
 * so all rate-limiting decisions for that user run on exactly one thread.
 * The per-user algorithm instance therefore needs zero locks or atomics.
 *
 *   User A requests ──► [Executor for User A (1 thread)] ──► [AlgorithmA — no locks]
 *   User B requests ──► [Executor for User B (1 thread)] ──► [AlgorithmB — no locks]
 *
 * WHY THIS BEATS LOCK-BASED RATE LIMITERS UNDER HIGH CONCENTION:
 *   Lock-based: many threads fight over the same lock → spinning → poor P99 latency
 *   Striped:    each thread submits a task and returns; the executor serializes
 *               access naturally → no contention → predictable latency
 *
 * PLUGGABLE ALGORITHMS (Strategy Pattern):
 * The algorithm is injected via a Supplier<PerUserAlgorithm>, so any of the five
 * built-in algorithms (or a custom one) can be used without modifying this class.
 * Use the static factory methods for the standard choices:
 *
 *   StripedExecutorRateLimiter.withTokenBucket(config)          // allows bursts
 *   StripedExecutorRateLimiter.withLeakyBucket(config)          // constant rate, no bursts
 *   StripedExecutorRateLimiter.withFixedWindow(config)          // simple counter
 *   StripedExecutorRateLimiter.withSlidingWindowLog(config)     // precise, O(n) memory
 *   StripedExecutorRateLimiter.withSlidingWindowCounter(config) // best overall trade-off
 *
 * TRADE-OFFS:
 *   + Zero lock contention, lower and more predictable P99 latency
 *   + Natural per-user isolation — one user's traffic never blocks another's
 *   + Works with any PerUserAlgorithm without code changes here
 *   − One OS thread per active user (~1 MB stack each); not for unbounded user sets
 *
 * Best for: per-user API rate limiting with a bounded user count (< ~10,000 users).
 */
public class StripedExecutorRateLimiter implements RateLimiter {

    private final RateLimitConfig config;
    private final AlgorithmType algorithmType;

    // Why: Strategy Pattern — algorithm is injected, not hardcoded.
    //      Each call creates a fresh instance for a new user.
    private final Supplier<PerUserAlgorithm> algorithmFactory;

    // Why: ConcurrentHashMap — the map itself is shared across threads (multiple callers
    //      submitting requests for different users concurrently), even though each value
    //      (executor / algorithm) is only ever used by one thread at a time.
    private final ConcurrentHashMap<String, ExecutorService> userExecutors;
    private final ConcurrentHashMap<String, PerUserAlgorithm> userAlgorithms;
    private final ConcurrentHashMap<String, PerUserMetrics> userMetrics;

    // Global counters (written from multiple threads via LongAdder to minimize contention)
    private final LongAdder totalRequests;
    private final LongAdder allowedRequests;
    private final LongAdder rejectedRequests;

    /**
     * Primary constructor — use when injecting a custom algorithm.
     * For the five standard algorithms, prefer the static factory methods below.
     *
     * @param config           rate limit configuration applied per user
     * @param algorithmType    identifies which algorithm is in use (for getAlgorithmType())
     * @param algorithmFactory produces a fresh algorithm instance for each new user
     */
    public StripedExecutorRateLimiter(RateLimitConfig config,
                                       AlgorithmType algorithmType,
                                       Supplier<PerUserAlgorithm> algorithmFactory) {
        this.config = config;
        this.algorithmType = algorithmType;
        this.algorithmFactory = algorithmFactory;
        this.userExecutors = new ConcurrentHashMap<>();
        this.userAlgorithms = new ConcurrentHashMap<>();
        this.userMetrics = new ConcurrentHashMap<>();
        this.totalRequests = new LongAdder();
        this.allowedRequests = new LongAdder();
        this.rejectedRequests = new LongAdder();
    }

    // ── Static factory methods ────────────────────────────────────────────────

    /** Tokens accumulate over time up to burst capacity — allows short traffic spikes. */
    public static StripedExecutorRateLimiter withTokenBucket(RateLimitConfig config) {
        return new StripedExecutorRateLimiter(config, AlgorithmType.TOKEN_BUCKET,
                () -> new PerUserTokenBucket(config));
    }

    /** Requests drain at a constant rate — smooths bursts, no "saved up" capacity. */
    public static StripedExecutorRateLimiter withLeakyBucket(RateLimitConfig config) {
        return new StripedExecutorRateLimiter(config, AlgorithmType.LEAKY_BUCKET,
                () -> new PerUserLeakyBucket(config));
    }

    /** Simple counter per time window — has boundary burst problem, use for coarse limits. */
    public static StripedExecutorRateLimiter withFixedWindow(RateLimitConfig config) {
        return new StripedExecutorRateLimiter(config, AlgorithmType.FIXED_WINDOW,
                () -> new PerUserFixedWindow(config));
    }

    /** Exact rolling window — perfect accuracy, O(n) memory per user. */
    public static StripedExecutorRateLimiter withSlidingWindowLog(RateLimitConfig config) {
        return new StripedExecutorRateLimiter(config, AlgorithmType.SLIDING_WINDOW_LOG,
                () -> new PerUserSlidingWindowLog(config));
    }

    /** Weighted blend of two windows — best accuracy-to-memory ratio, O(1) per user. */
    public static StripedExecutorRateLimiter withSlidingWindowCounter(RateLimitConfig config) {
        return new StripedExecutorRateLimiter(config, AlgorithmType.SLIDING_WINDOW_COUNTER,
                () -> new PerUserSlidingWindowCounter(config));
    }

    // ── Core API ──────────────────────────────────────────────────────────────

    /**
     * Non-blocking async check. The returned future resolves on the user's dedicated
     * thread, so the caller is never blocked.
     *
     * @param userId identifies the rate-limited entity (user ID, tenant, API key…)
     * @return future containing true if the request is allowed
     */
    public CompletableFuture<Boolean> tryAcquireAsync(String userId) {
        totalRequests.increment();
        return submitRequest(userId, 1);
    }

    /**
     * Synchronous check. Blocks until the user's dedicated thread makes the decision.
     * Still benefits from thread confinement — no lock contention in the algorithm.
     *
     * @param userId identifies the rate-limited entity
     * @return true if the request is allowed
     */
    public boolean tryAcquire(String userId) {
        totalRequests.increment();
        try {
            return submitRequest(userId, 1).get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire("default");
    }

    @Override
    public boolean tryAcquire(int permits) {
        totalRequests.add(permits);
        try {
            return submitRequest("default", permits).get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            return false;
        }
    }

    @Override
    public long getAvailablePermits() {
        return getAvailablePermits("default");
    }

    /**
     * Returns the approximate available permits for a user.
     *
     * Reads the algorithm state from the calling thread rather than the user's dedicated
     * thread. The value may be slightly stale under concurrent writes. Use for monitoring
     * only — never for rate-limiting decisions.
     */
    public long getAvailablePermits(String userId) {
        PerUserAlgorithm algorithm = userAlgorithms.get(userId);
        return algorithm != null ? algorithm.availablePermits() : config.getBurstSize();
    }

    @Override
    public void reset() {
        reset("default");
    }

    public void reset(String userId) {
        PerUserAlgorithm algorithm = userAlgorithms.get(userId);
        if (algorithm != null) algorithm.reset();
    }

    @Override
    public RateLimitConfig getConfig() {
        return config;
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public Stats getStats() {
        return new Stats(
            totalRequests.sum(),
            allowedRequests.sum(),
            rejectedRequests.sum(),
            userExecutors.size()
        );
    }

    public UserStats getUserStats(String userId) {
        PerUserAlgorithm algorithm = userAlgorithms.get(userId);
        PerUserMetrics metrics = userMetrics.get(userId);
        if (algorithm == null || metrics == null) return null;
        return new UserStats(
            userId,
            algorithm.availablePermits(),
            metrics.total.sum(),
            metrics.allowed.sum(),
            metrics.rejected.sum()
        );
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void shutdown() {
        userExecutors.values().forEach(ExecutorService::shutdown);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Routes a rate-limiting request to the user's dedicated thread.
     * The algorithm and metrics updates both happen on that thread.
     */
    private CompletableFuture<Boolean> submitRequest(String userId, int permits) {
        ExecutorService executor = userExecutors.computeIfAbsent(userId, this::createExecutor);

        return CompletableFuture.supplyAsync(() -> {
            // computeIfAbsent is safe here because only this user's thread ever writes
            // to this key — the executor guarantees single-threaded access per userId.
            PerUserAlgorithm algorithm = userAlgorithms.computeIfAbsent(userId,
                    uid -> algorithmFactory.get());

            boolean allowed = algorithm.tryConsume(permits);
            recordMetrics(userId, allowed);
            return allowed;
        }, executor);
    }

    private void recordMetrics(String userId, boolean allowed) {
        PerUserMetrics metrics = userMetrics.computeIfAbsent(userId, uid -> new PerUserMetrics());
        metrics.total.increment();
        if (allowed) {
            allowedRequests.increment();
            metrics.allowed.increment();
        } else {
            rejectedRequests.increment();
            metrics.rejected.increment();
        }
    }

    private ExecutorService createExecutor(String userId) {
        // Why: daemon thread so it doesn't prevent JVM shutdown.
        //      Named thread helps with debugging thread dumps.
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-" + userId);
            t.setDaemon(true);
            return t;
        });
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    /**
     * Per-user request counters.
     *
     * Written on the user's dedicated thread; read from any thread (e.g., getUserStats).
     * LongAdder is used instead of plain longs to guarantee visibility across threads.
     */
    private static class PerUserMetrics {
        final LongAdder total   = new LongAdder();
        final LongAdder allowed  = new LongAdder();
        final LongAdder rejected = new LongAdder();
    }

    public static class Stats {
        public final long totalRequests;
        public final long allowedRequests;
        public final long rejectedRequests;
        public final int  activeUsers;

        Stats(long total, long allowed, long rejected, int users) {
            this.totalRequests  = total;
            this.allowedRequests = allowed;
            this.rejectedRequests = rejected;
            this.activeUsers    = users;
        }

        @Override
        public String toString() {
            double rate = totalRequests > 0 ? 100.0 * allowedRequests / totalRequests : 0.0;
            return String.format(
                "Stats{total=%d, allowed=%d, rejected=%d, users=%d, allowRate=%.1f%%}",
                totalRequests, allowedRequests, rejectedRequests, activeUsers, rate);
        }
    }

    public static class UserStats {
        public final String userId;
        public final long   availablePermits;
        public final long   totalRequests;
        public final long   allowedRequests;
        public final long   rejectedRequests;

        UserStats(String userId, long available, long total, long allowed, long rejected) {
            this.userId           = userId;
            this.availablePermits = available;
            this.totalRequests    = total;
            this.allowedRequests  = allowed;
            this.rejectedRequests = rejected;
        }

        @Override
        public String toString() {
            return String.format(
                "UserStats{user=%s, available=%d, total=%d, allowed=%d, rejected=%d}",
                userId, availablePermits, totalRequests, allowedRequests, rejectedRequests);
        }
    }
}
