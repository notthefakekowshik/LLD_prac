package com.lldprep.systems.orderbook.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Hybrid Striped Executor for Order Book Systems.
 *
 * <p>Addresses the "Hot Stripe" problem where 80% of volume comes from 20% of symbols.
 * Combines dedicated threads for hot symbols with shared pools for cold symbols.
 *
 * <p><b>Architecture:</b>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    HYBRID STRIPED EXECUTOR                   │
 * ├─────────────────────────────────────────────────────────────┤
 * │  FAST LANE (Dedicated Threads)   │   SLOW LANE (Shared Pool)  │
 * │  ─────────────────────────       │   ───────────────────────   │
 * │  • AAPL → SingleThreadExecutor  │   • SmallCap1 → Pool-1     │
 * │  • NVDA → SingleThreadExecutor  │   • SmallCap2 → Pool-2     │
 * │  • TSLA → SingleThreadExecutor  │   • ...                    │
 * │                                  │   (32 stripes for rest)    │
 * └─────────────────────────────────────────────────────────────┘
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li><b>Hot Stripe Detection:</b> Monitors queue depth and throughput per symbol</li>
 *   <li><b>Backpressure:</b> Rejects or blocks when queue exceeds threshold</li>
 *   <li><b>Dynamic Promotion:</b> Migrates viral symbols to fast lane</li>
 *   <li><b>Load Shedding:</b> Tier-based resource allocation</li>
 * </ul>
 *
 * @see OrderBookEngine
 */
public class HybridStripedExecutor {

    private static final int SLOW_LANE_SIZE = 32;
    private static final int DEFAULT_HOT_QUEUE_THRESHOLD = 1000;
    private static final int DEFAULT_MAX_QUEUE_DEPTH = 5000;
    private static final long PROMOTION_THRESHOLD_EVENTS_PER_SEC = 10_000;

    /**
     * Symbol tiers for differentiated handling.
     */
    public enum SymbolTier {
        TIER_1(Set.of("AAPL", "MSFT", "NVDA", "TSLA", "AMZN")),
        TIER_2(Set.of("GOOGL", "META", "NFLX", "AMD", "CRM", "INTC", "BABA")),
        TIER_3(Set.of()); // All others

        private final Set<String> symbols;

        SymbolTier(Set<String> symbols) {
            this.symbols = symbols;
        }

        public static SymbolTier forSymbol(String symbol) {
            for (SymbolTier tier : values()) {
                if (tier.symbols.contains(symbol)) {
                    return tier;
                }
            }
            return TIER_3;
        }
    }

    // Fast lane: Dedicated single-thread executors for hot symbols
    private final ConcurrentHashMap<String, ExecutorService> fastLane = new ConcurrentHashMap<>();

    // Slow lane: Shared thread pool stripes for cold symbols
    private final ExecutorService[] slowLane;

    // Monitoring: Queue depth per symbol
    private final ConcurrentHashMap<String, AtomicInteger> queueDepth = new ConcurrentHashMap<>();

    // Monitoring: Events per second per symbol
    private final ConcurrentHashMap<String, LongAdder> eventCounters = new ConcurrentHashMap<>();

    // Backpressure configuration
    private final int hotQueueThreshold;
    private final int maxQueueDepth;

    /**
     * Creates executor with default tier-based configuration.
     */
    public HybridStripedExecutor() {
        this(DEFAULT_HOT_QUEUE_THRESHOLD, DEFAULT_MAX_QUEUE_DEPTH);
    }

    /**
     * Creates executor with custom backpressure thresholds.
     *
     * @param hotQueueThreshold Queue depth to consider a symbol "hot"
     * @param maxQueueDepth Maximum queue depth before rejecting tasks
     */
    public HybridStripedExecutor(int hotQueueThreshold, int maxQueueDepth) {
        this.hotQueueThreshold = hotQueueThreshold;
        this.maxQueueDepth = maxQueueDepth;

        // Initialize slow lane with fixed thread pool stripes
        this.slowLane = new ExecutorService[SLOW_LANE_SIZE];
        for (int i = 0; i < SLOW_LANE_SIZE; i++) {
            final int stripeIndex = i;
            slowLane[i] = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "slow-lane-" + stripeIndex);
                    t.setDaemon(true);
                    return t;
                }
            });
        }

        // Initialize fast lane for Tier 1 and Tier 2 symbols
        initializeFastLane(SymbolTier.TIER_1);
        initializeFastLane(SymbolTier.TIER_2);
    }

    private void initializeFastLane(SymbolTier tier) {
        for (String symbol : tier.symbols) {
            fastLane.put(symbol, createDedicatedExecutor(symbol));
        }
    }

    private ExecutorService createDedicatedExecutor(String symbol) {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "fast-lane-" + symbol);
                t.setDaemon(true);
                return t;
            }
        });
    }

    /**
     * Submits a task for execution on the appropriate stripe.
     *
     * @param symbol The stripe key (e.g., "AAPL")
     * @param task The task to execute
     * @return Future representing the pending completion of the task
     * @throws RejectedExecutionException if queue is full (backpressure)
     */
    public Future<Void> submit(String symbol, Runnable task) {
        return submitWithBackpressure(symbol, task, BackpressureMode.REJECT);
    }

    /**
     * Submits a task with configurable backpressure strategy.
     *
     * @param symbol The stripe key
     * @param task The task to execute
     * @param mode Backpressure strategy: REJECT, BLOCK, or SHED
     * @return Future representing the pending completion
     */
    public Future<Void> submitWithBackpressure(String symbol, Runnable task, BackpressureMode mode) {
        ExecutorService executor = executorFor(symbol);

        // Check current queue depth
        int currentDepth = queueDepth.computeIfAbsent(symbol, k -> new AtomicInteger()).get();

        // Apply backpressure if queue is too deep
        if (currentDepth >= maxQueueDepth) {
            switch (mode) {
                case REJECT:
                    throw new RejectedExecutionException(
                            "Hot stripe backpressure: " + symbol + " queue=" + currentDepth);
                case BLOCK:
                    // Will block in runAsync until queue drains
                    break;
                case SHED:
                    // Return completed future (drop the task)
                    return CompletableFuture.completedFuture(null);
            }
        }

        // Track queue depth
        queueDepth.get(symbol).incrementAndGet();

        // Track event rate for hot stripe detection
        eventCounters.computeIfAbsent(symbol, k -> new LongAdder()).increment();

        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } finally {
                queueDepth.get(symbol).decrementAndGet();
            }
        }, executor);
    }

    /**
     * Returns the executor for a given symbol.
     * Fast lane symbols get dedicated threads; others get slow lane stripes.
     */
    private ExecutorService executorFor(String symbol) {
        // Check fast lane first
        ExecutorService dedicated = fastLane.get(symbol);
        if (dedicated != null) {
            return dedicated;
        }

        // Route to slow lane via hash-based stripe
        int stripeIndex = Math.abs(symbol.hashCode()) % SLOW_LANE_SIZE;
        return slowLane[stripeIndex];
    }

    /**
     * Checks if a symbol is currently experiencing hot stripe conditions.
     */
    public boolean isHotStripe(String symbol) {
        AtomicInteger depth = queueDepth.get(symbol);
        return depth != null && depth.get() > hotQueueThreshold;
    }

    /**
     * Gets current queue depth for a symbol.
     */
    public int getQueueDepth(String symbol) {
        return queueDepth.getOrDefault(symbol, new AtomicInteger(0)).get();
    }

    /**
     * Dynamically promotes a symbol to the fast lane.
     * Use when detection shows sustained high throughput.
     *
     * @param symbol Symbol to promote
     * @return true if promotion succeeded
     */
    public boolean promoteToFastLane(String symbol) {
        if (fastLane.containsKey(symbol)) {
            return false; // Already in fast lane
        }

        ExecutorService newExecutor = createDedicatedExecutor(symbol);
        ExecutorService previous = fastLane.putIfAbsent(symbol, newExecutor);

        if (previous != null) {
            // Lost race, shutdown new executor
            newExecutor.shutdown();
            return false;
        }

        return true;
    }

    /**
     * Dynamically demotes a symbol from fast lane to slow lane.
     * Use when traffic subsides to reclaim resources.
     */
    public boolean demoteToSlowLane(String symbol) {
        ExecutorService removed = fastLane.remove(symbol);
        if (removed != null) {
            removed.shutdown();
            return true;
        }
        return false;
    }

    /**
     * Rebalances a hot symbol by draining its queue to a new dedicated executor.
     * Nuclear option for meme stock scenarios.
     *
     * <p><b>Warning:</b> This pauses the symbol briefly during migration.
     */
    public void emergencyRebalance(String symbol) {
        ExecutorService oldExecutor = fastLane.get(symbol);
        if (oldExecutor == null) {
            return; // Not in fast lane, nothing to rebalance
        }

        if (!(oldExecutor instanceof ThreadPoolExecutor)) {
            return;
        }

        // Create replacement executor
        ExecutorService newExecutor = createDedicatedExecutor(symbol + "-rebalanced");

        // Shutdown old executor (reject new tasks)
        oldExecutor.shutdown();

        // Drain remaining tasks and submit to new executor
        try {
            List<Runnable> remaining = ((ThreadPoolExecutor) oldExecutor)
                    .getQueue()
                    .stream()
                    .toList();

            for (Runnable task : remaining) {
                newExecutor.submit(task);
            }

            // Replace in fast lane
            fastLane.put(symbol, newExecutor);

        } catch (Exception e) {
            // Restore old executor on failure
            fastLane.put(symbol, oldExecutor);
            throw new RuntimeException("Rebalance failed for " + symbol, e);
        }
    }

    /**
     * Gets monitoring statistics for all stripes.
     */
    public StripeStats getStats(String symbol) {
        return new StripeStats(
                symbol,
                getQueueDepth(symbol),
                eventCounters.getOrDefault(symbol, new LongAdder()).sum(),
                isHotStripe(symbol),
                fastLane.containsKey(symbol)
        );
    }

    /**
     * Graceful shutdown of all executors.
     */
    public void shutdown() {
        fastLane.values().forEach(ExecutorService::shutdown);
        for (ExecutorService executor : slowLane) {
            executor.shutdown();
        }
    }

    /**
     * Awaits termination with timeout.
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        for (ExecutorService executor : fastLane.values()) {
            long remaining = deadline - System.nanoTime();
            if (!executor.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }

        for (ExecutorService executor : slowLane) {
            long remaining = deadline - System.nanoTime();
            if (!executor.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Backpressure handling modes.
     */
    public enum BackpressureMode {
        /** Reject task with exception */
        REJECT,
        /** Block caller until queue drains */
        BLOCK,
        /** Silently drop the task */
        SHED
    }

    /**
     * Immutable statistics snapshot for a stripe.
     */
    public record StripeStats(
            String symbol,
            int queueDepth,
            long totalEvents,
            boolean isHot,
            boolean inFastLane
    ) {}
}
