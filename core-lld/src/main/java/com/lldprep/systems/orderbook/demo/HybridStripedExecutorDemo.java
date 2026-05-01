package com.lldprep.systems.orderbook.demo;

import com.lldprep.systems.orderbook.service.HybridStripedExecutor;
import com.lldprep.systems.orderbook.service.HybridStripedExecutor.BackpressureMode;
import com.lldprep.systems.orderbook.service.HybridStripedExecutor.StripeStats;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the HybridStripedExecutor with hot stripe scenarios.
 *
 * <p><b>Scenarios:</b>
 * <ol>
 *   <li>Normal distribution: All symbols get equal load</li>
 *   <li>Hot stripe (80/20): Simulate NASDAQ volume concentration</li>
 *   <li>Backpressure: Queue overflow handling</li>
 *   <li>Dynamic promotion: Auto-promote viral symbol to fast lane</li>
 *   <li>Monitoring: Track queue depth and event rates</li>
 * </ol>
 */
public class HybridStripedExecutorDemo {

    private static final int HOT_SYMBOL_VOLUME = 100_000;
    private static final int COLD_SYMBOL_VOLUME = 1_000;

    public static void main(String[] args) throws Exception {
        System.out.println("=== HybridStripedExecutor Demo ===\n");

        // Create executor with low thresholds for demo visibility
        HybridStripedExecutor executor = new HybridStripedExecutor(100, 500);

        demoNormalDistribution(executor);
        demoHotStripeScenario(executor);
        demoBackpressure(executor);
        demoDynamicPromotion(executor);
        demoMonitoring(executor);

        System.out.println("\nShutting down...");
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("Done.");
    }

    /**
     * Scenario 1: Normal load distribution across many symbols.
     */
    private static void demoNormalDistribution(HybridStripedExecutor executor) throws Exception {
        System.out.println("\n--- Scenario 1: Normal Distribution ---");

        String[] symbols = {"SYM1", "SYM2", "SYM3", "SYM4", "SYM5"};
        CountDownLatch latch = new CountDownLatch(symbols.length * 100);

        for (String symbol : symbols) {
            for (int i = 0; i < 100; i++) {
                final int taskId = i;
                executor.submit(symbol, () -> {
                    simulateWork(1); // 1ms work
                    System.out.printf("  %s: task-%d completed%n", symbol, taskId);
                    latch.countDown();
                });
            }
        }

        latch.await(10, TimeUnit.SECONDS);
        System.out.println("Normal distribution complete.");
    }

    /**
     * Scenario 2: Simulate NASDAQ 80/20 volume concentration.
     */
    private static void demoHotStripeScenario(HybridStripedExecutor executor) throws Exception {
        System.out.println("\n--- Scenario 2: Hot Stripe (80/20 Rule) ---");
        System.out.println("Simulating: AAPL gets 80% volume, others get 20%");

        String hotSymbol = "AAPL";
        String[] coldSymbols = {"SMALL1", "SMALL2", "SMALL3", "SMALL4"};

        // AAPL: 80% of total load
        CountDownLatch hotLatch = new CountDownLatch(HOT_SYMBOL_VOLUME);
        for (int i = 0; i < HOT_SYMBOL_VOLUME; i++) {
            executor.submit(hotSymbol, () -> {
                simulateWork(0); // Minimal work, max throughput
                hotLatch.countDown();
            });
        }

        // Others: 20% distributed
        CountDownLatch coldLatch = new CountDownLatch(coldSymbols.length * COLD_SYMBOL_VOLUME);
        for (String symbol : coldSymbols) {
            for (int i = 0; i < COLD_SYMBOL_VOLUME; i++) {
                executor.submit(symbol, () -> {
                    simulateWork(0);
                    coldLatch.countDown();
                });
            }
        }

        // Show queue buildup
        Thread.sleep(100); // Let queues fill
        System.out.printf("AAPL queue depth: %d (hot stripe detected: %b)%n",
                executor.getQueueDepth(hotSymbol), executor.isHotStripe(hotSymbol));

        hotLatch.await(30, TimeUnit.SECONDS);
        coldLatch.await(30, TimeUnit.SECONDS);
        System.out.println("Hot stripe scenario complete.");
    }

    /**
     * Scenario 3: Backpressure when queue overflows.
     */
    private static void demoBackpressure(HybridStripedExecutor executor) {
        System.out.println("\n--- Scenario 3: Backpressure ---");

        String symbol = "BACKPRESSURE_TEST";

        // Flood the queue
        for (int i = 0; i < 600; i++) {
            try {
                executor.submitWithBackpressure(symbol, () -> {
                    simulateWork(10);
                }, BackpressureMode.REJECT);
            } catch (Exception e) {
                System.out.printf("  Task %d rejected: %s%n", i, e.getMessage());
                break; // Stop on first rejection
            }
        }

        System.out.println("Backpressure demo complete.");
    }

    /**
     * Scenario 4: Dynamic promotion of viral symbol to fast lane.
     */
    private static void demoDynamicPromotion(HybridStripedExecutor executor) throws Exception {
        System.out.println("\n--- Scenario 4: Dynamic Promotion ---");

        String viralSymbol = "MEME_STOCK";

        // Initially in slow lane
        System.out.printf("Before: %s in fast lane = %b%n",
                viralSymbol, isInFastLane(executor, viralSymbol));

        // Simulate viral spike
        CountDownLatch latch = new CountDownLatch(1000);
        for (int i = 0; i < 1000; i++) {
            executor.submit(viralSymbol, () -> {
                simulateWork(0);
                latch.countDown();
            });
        }

        Thread.sleep(50);
        System.out.printf("Queue depth during spike: %d%n", executor.getQueueDepth(viralSymbol));

        // Promote to fast lane
        boolean promoted = executor.promoteToFastLane(viralSymbol);
        System.out.printf("Promoted to fast lane: %b%n", promoted);

        latch.await(10, TimeUnit.SECONDS);
        System.out.printf("After: %s in fast lane = %b%n",
                viralSymbol, isInFastLane(executor, viralSymbol));
    }

    /**
     * Scenario 5: Monitoring and statistics.
     */
    private static void demoMonitoring(HybridStripedExecutor executor) throws Exception {
        System.out.println("\n--- Scenario 5: Monitoring ---");

        // Generate load on multiple symbols
        String[] symbols = {"AAPL", "MSFT", "TSLA", "SMALL1"};
        CountDownLatch latch = new CountDownLatch(symbols.length * 100);

        for (String symbol : symbols) {
            for (int i = 0; i < 100; i++) {
                executor.submit(symbol, () -> {
                    simulateWork(1);
                    latch.countDown();
                });
            }
        }

        Thread.sleep(50);

        // Print stats
        System.out.println("\nStripe Statistics:");
        for (String symbol : symbols) {
            StripeStats stats = executor.getStats(symbol);
            System.out.printf("  %s: depth=%d, events=%d, hot=%b, fastLane=%b%n",
                    stats.symbol(), stats.queueDepth(), stats.totalEvents(),
                    stats.isHot(), stats.inFastLane());
        }

        latch.await(10, TimeUnit.SECONDS);
    }

    private static void simulateWork(int millis) {
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean isInFastLane(HybridStripedExecutor executor, String symbol) {
        return executor.getStats(symbol).inFastLane();
    }
}
