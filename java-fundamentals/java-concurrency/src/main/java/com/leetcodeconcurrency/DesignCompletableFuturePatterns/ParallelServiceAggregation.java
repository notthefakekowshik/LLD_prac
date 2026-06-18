package com.leetcodeconcurrency.DesignCompletableFuturePatterns;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Real-world scenario: Parallel Service Aggregation
 *
 * Pattern: An API gateway that calls multiple downstream services in parallel,
 * aggregates their responses, and returns a combined result.
 *
 * This is THE most common SDE2+ concurrency interview scenario.
 */
public class ParallelServiceAggregation {

    // Custom executor to avoid blocking ForkJoinPool.commonPool() on I/O tasks
    private static final ExecutorService ioPool = Executors.newFixedThreadPool(10);

    // ──────────────────────────────────────────────
    // Simulated downstream services
    // ──────────────────────────────────────────────
    static class UserService {
        CompletableFuture<String> getUser(String userId) {
            return CompletableFuture.supplyAsync(() -> {
                sleep(150);
                return "{name: 'Alice', id: '" + userId + "'}";
            }, ioPool);
        }
    }

    static class OrderService {
        CompletableFuture<String> getOrders(String userId) {
            return CompletableFuture.supplyAsync(() -> {
                sleep(200);
                return "[order-101, order-102, order-103]";
            }, ioPool);
        }
    }

    static class RecommendationService {
        CompletableFuture<String> getRecommendations(String userId) {
            return CompletableFuture.supplyAsync(() -> {
                sleep(300);
                return "[product-A, product-B, product-C]";
            }, ioPool);
        }
    }

    static class AnalyticsService {
        CompletableFuture<Void> recordAccess(String userId) {
            return CompletableFuture.runAsync(() -> {
                sleep(50);
                System.out.println("  [Analytics] Recorded access for " + userId);
            }, ioPool);
        }
    }

    // ──────────────────────────────────────────────
    // 1. Classic "API Gateway" pattern — allOf + aggregation
    // ──────────────────────────────────────────────
    public static void demoApiGateway() throws Exception {
        System.out.println("─── API Gateway (allOf aggregation) ───");

        UserService userService = new UserService();
        OrderService orderService = new OrderService();
        RecommendationService recService = new RecommendationService();
        AnalyticsService analyticsService = new AnalyticsService();

        String userId = "U123";

        // Fire all calls in parallel
        CompletableFuture<String> userFuture = userService.getUser(userId);
        CompletableFuture<String> orderFuture = orderService.getOrders(userId);
        CompletableFuture<String> recFuture = recService.getRecommendations(userId);
        CompletableFuture<Void> analyticsFuture = analyticsService.recordAccess(userId); // fire-and-forget-ish

        // Aggregate: wait for all core services
        String response = CompletableFuture.allOf(userFuture, orderFuture, recFuture)
                .thenApply(v -> {
                    String user = userFuture.join();
                    String orders = orderFuture.join();
                    String recs = recFuture.join();
                    return String.format(
                            "{\n  user: %s,\n  orders: %s,\n  recommendations: %s\n}",
                            user, orders, recs);
                })
                .get(2, TimeUnit.SECONDS);

        System.out.println("  " + response);
        analyticsFuture.join(); // ensure analytics recorded
    }

    // ──────────────────────────────────────────────
    // 2. Time-bounded aggregation — OR between async and timeout
    // ──────────────────────────────────────────────
    public static void demoTimeBoundedAggregation() throws Exception {
        System.out.println("\n─── Time-bounded aggregation (500ms budget) ───");

        CompletableFuture<String> serviceA = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "A-OK";
        }, ioPool);

        CompletableFuture<String> serviceB = CompletableFuture.supplyAsync(() -> {
            sleep(800); // this is too slow
            return "B-OK";
        }, ioPool);

        CompletableFuture<String> serviceC = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "C-OK";
        }, ioPool);

        // Each service gets a fallback on timeout
        CompletableFuture<String> a = serviceA
                .completeOnTimeout("A-TIMEOUT", 500, TimeUnit.MILLISECONDS);
        CompletableFuture<String> b = serviceB
                .completeOnTimeout("B-TIMEOUT", 500, TimeUnit.MILLISECONDS);
        CompletableFuture<String> c = serviceC
                .completeOnTimeout("C-TIMEOUT", 500, TimeUnit.MILLISECONDS);

        String aggregated = CompletableFuture.allOf(a, b, c)
                .thenApply(v -> List.of(a.join(), b.join(), c.join()).toString())
                .get();

        System.out.println("  " + aggregated);
    }

    // ──────────────────────────────────────────────
    // 3. Circuit-breaker-ish — degrade gracefully on failure
    // ──────────────────────────────────────────────
    public static void demoGracefulDegradation() throws Exception {
        System.out.println("\n─── Graceful degradation ───");

        CompletableFuture<String> primary = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Primary service down");
        });

        CompletableFuture<String> secondary = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Secondary result";
        });

        String result = primary
                .exceptionally(ex -> {
                    System.out.println("  [DEGRADE] Primary failed → falling back to secondary");
                    return null;
                })
                .thenCompose(val ->
                        val != null
                                ? CompletableFuture.completedFuture(val)
                                : secondary
                )
                .get();

        System.out.println("  Final: " + result);
    }

    // ──────────────────────────────────────────────
    // 4. Batching with fan-out + fan-in
    // ──────────────────────────────────────────────
    public static void demoBatchProcessing() throws Exception {
        System.out.println("\n─── Batch processing (100 items, 10 threads) ───");

        List<String> itemIds = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> "item-" + i)
                .collect(Collectors.toList());

        long start = System.currentTimeMillis();

        List<String> enriched = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    sleep(50); // simulate per-item I/O
                    return id + ":enriched";
                }, ioPool))
                .collect(Collectors.toList())
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  Enriched " + enriched.size() + " items in " + elapsed + "ms");
    }

    // ──────────────────────────────────────────────
    // 5. Pipeline: fetch → transform → persist (reactive-ish)
    // ──────────────────────────────────────────────
    public static void demoPipeline() throws Exception {
        System.out.println("\n─── Data pipeline (fetch → transform → persist) ───");

        String result = CompletableFuture
                .supplyAsync(() -> {
                    sleep(100);
                    return "raw-data-from-db";
                }, ioPool)
                .thenApplyAsync(raw -> {
                    System.out.println("  [Transform] Thread: " + Thread.currentThread().getName());
                    return raw.toUpperCase().replace("-", "_");
                }, ioPool)
                .thenApplyAsync(transformed -> {
                    sleep(100);
                    return "persisted://" + transformed;
                }, ioPool)
                .get();

        System.out.println("  " + result);
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        demoApiGateway();
        demoTimeBoundedAggregation();
        demoGracefulDegradation();
        demoBatchProcessing();
        demoPipeline();
        ioPool.shutdown();
        ioPool.awaitTermination(2, TimeUnit.SECONDS);
    }
}
