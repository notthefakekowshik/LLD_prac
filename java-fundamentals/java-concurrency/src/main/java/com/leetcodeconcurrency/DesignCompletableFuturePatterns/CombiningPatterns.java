package com.leetcodeconcurrency.DesignCompletableFuturePatterns;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CompletableFuture Combining Patterns
 *
 * allOf  → wait for ALL futures (returns Void → need .thenApply to extract results)
 * anyOf  → first to complete wins   (returns Object → need cast)
 * Fan-out/Fan-in → fire many, collect all
 *
 * Key interview distinction:
 *   allOf returns CompletableFuture<Void> — results are NOT directly accessible.
 *   You must use allOf(...).thenApply(v -> future1.join() + future2.join())
 *   Or: stream.map(CompletableFuture::join).collect(...)
 */
public class CombiningPatterns {

    // ──────────────────────────────────────────────
    // 1. allOf — wait for ALL, then aggregate
    // ──────────────────────────────────────────────
    public static void demoAllOf() throws ExecutionException, InterruptedException {
        System.out.println("─── allOf (wait for all) ───");

        CompletableFuture<String> db = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "DB: user-profile";
        });

        CompletableFuture<String> cache = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Cache: recent-orders";
        });

        CompletableFuture<String> api = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "API: recommendations";
        });

        // allOf returns CompletableFuture<Void> — must use thenApply to extract
        String aggregated = CompletableFuture.allOf(db, cache, api)
                .thenApply(v ->
                        db.join() + " | " + cache.join() + " | " + api.join()
                )
                .get();

        System.out.println("  " + aggregated);
    }

    // ──────────────────────────────────────────────
    // 2. allOf with stream — idiomatic
    // ──────────────────────────────────────────────
    public static void demoAllOfStream() throws ExecutionException, InterruptedException {
        System.out.println("\n─── allOf (stream variant) ───");

        List<CompletableFuture<String>> futures = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    sleep(i * 50L); // variable delays
                    return "Task-" + i;
                }))
                .collect(Collectors.toList());

        // Idiomatic: convert List<CF<T>> → CF<List<T>>
        CompletableFuture<List<String>> all = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

        System.out.println("  " + all.get());
    }

    // ──────────────────────────────────────────────
    // 3. anyOf — first to complete wins
    // ──────────────────────────────────────────────
    public static void demoAnyOf() throws ExecutionException, InterruptedException {
        System.out.println("\n─── anyOf (fastest wins) ───");

        CompletableFuture<String> service1 = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "SlowService";
        });

        CompletableFuture<String> service2 = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "FastService";
        });

        CompletableFuture<String> service3 = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return "SlowestService";
        });

        // anyOf returns CompletableFuture<Object> — cast is needed
        Object result = CompletableFuture.anyOf(service1, service2, service3).get();
        System.out.println("  Winner: " + result);
    }

    // ──────────────────────────────────────────────
    // 4. Fan-out / Fan-in — scatter-gather
    // ──────────────────────────────────────────────
    public static void demoFanOutFanIn() throws ExecutionException, InterruptedException {
        System.out.println("\n─── Fan-out / Fan-in (scatter-gather) ───");

        List<String> productIds = List.of("P1", "P2", "P3", "P4", "P5");

        // FAN-OUT: fire one async call per product
        List<CompletableFuture<String>> priceFutures = productIds.stream()
                .map(pid -> CompletableFuture.supplyAsync(() ->
                        "Price(" + pid + ")=" + fetchProductPrice(pid)))
                .collect(Collectors.toList());

        // FAN-IN: collect all results
        List<String> prices = priceFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        System.out.println("  " + prices);
    }

    // ──────────────────────────────────────────────
    // 5. Fan-out with partial failure handling
    // ──────────────────────────────────────────────
    public static void demoFanOutWithFallback() {
        System.out.println("\n─── Fan-out with fallback per task ───");

        List<CompletableFuture<String>> futures = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> CompletableFuture
                        .supplyAsync(() -> {
                            if (i == 3) throw new RuntimeException("Task-" + i + " failed");
                            sleep(i * 50L);
                            return "Result-" + i;
                        })
                        .exceptionally(ex -> "FALLBACK for " + ex.getMessage())
                        .thenApply(String::toUpperCase))
                .collect(Collectors.toList());

        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        System.out.println("  " + results);
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

    private static String fetchProductPrice(String productId) {
        sleep(100);
        return switch (productId) {
            case "P1" -> "₹100";
            case "P2" -> "₹200";
            case "P3" -> "₹150";
            case "P4" -> "₹300";
            case "P5" -> "₹250";
            default -> "N/A";
        };
    }

    public static void main(String[] args) throws Exception {
        demoAllOf();
        demoAllOfStream();
        demoAnyOf();
        demoFanOutFanIn();
        demoFanOutWithFallback();
    }
}
