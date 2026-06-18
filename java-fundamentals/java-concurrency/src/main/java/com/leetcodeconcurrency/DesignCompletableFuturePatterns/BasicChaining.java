package com.leetcodeconcurrency.DesignCompletableFuturePatterns;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * CompletableFuture Basic Chaining Patterns
 *
 * Core interview distinction:
 *   thenApply  → synchronous map     (T -> U, same stage)
 *   thenCompose → async flatMap       (T -> CompletableFuture<U>, flattens nested CF)
 *   thenAccept → terminal consumer    (T -> void)
 *   thenRun    → terminal runnable    (() -> void)
 *
 * Also covers their async variants:
 *   thenApplyAsync / thenComposeAsync / thenAcceptAsync / thenRunAsync
 *   These run on ForkJoinPool.commonPool() by default.
 */
public class BasicChaining {

    // ──────────────────────────────────────────────
    // 1. thenApply — synchronous transformation (map)
    // ──────────────────────────────────────────────
    public static void demoThenApply() throws ExecutionException, InterruptedException {
        System.out.println("─── thenApply (synchronous map) ───");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("  [supplyAsync] Thread: " + Thread.currentThread().getName());
                    return "hello";
                })
                .thenApply(word -> {
                    System.out.println("  [thenApply-1] Thread: " + Thread.currentThread().getName());
                    return word.toUpperCase();
                })
                .thenApply(word -> {
                    System.out.println("  [thenApply-2] Thread: " + Thread.currentThread().getName());
                    return word + " WORLD";
                });

        System.out.println("  Result: " + future.get());
    }

    // ──────────────────────────────────────────────
    // 2. thenCompose — async flatMap (flatten nested CF)
    // ──────────────────────────────────────────────
    public static void demoThenCompose() throws ExecutionException, InterruptedException {
        System.out.println("\n─── thenCompose (async flatMap) ───");

        CompletableFuture<String> future = fetchUserId("alice")
                .thenCompose(userId -> fetchUserEmail(userId))
                .thenCompose(email -> sendWelcomeEmail(email));

        System.out.println("  Result: " + future.get());
    }

    // Why thenCompose vs thenApply — classic interview question:
    // thenApply returns CompletableFuture<CompletableFuture<String>> (nested!)
    // thenCompose flattens: CompletableFuture<String>
    // Think: map vs flatMap in Streams

    private static CompletableFuture<String> fetchUserId(String username) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "user-42";
        });
    }

    private static CompletableFuture<String> fetchUserEmail(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "alice@example.com";
        });
    }

    private static CompletableFuture<String> sendWelcomeEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "Welcome email sent to " + email;
        });
    }

    // ──────────────────────────────────────────────
    // 3. thenAccept — terminal consumer (end of chain)
    // ──────────────────────────────────────────────
    public static void demoThenAccept() throws ExecutionException, InterruptedException {
        System.out.println("\n─── thenAccept (terminal consumer) ───");

        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> {
                    sleep(100);
                    return "processed-data";
                })
                .thenAccept(result -> {
                    System.out.println("  Consumer received: " + result);
                    // Side effect: log, persist, publish event, etc.
                });

        future.get(); // Wait for completion (returns Void)
        System.out.println("  Chain completed.");
    }

    // ──────────────────────────────────────────────
    // 4. thenRun — terminal runnable (no input)
    // ──────────────────────────────────────────────
    public static void demoThenRun() throws ExecutionException, InterruptedException {
        System.out.println("\n─── thenRun (terminal runnable) ───");

        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> {
                    sleep(100);
                    return "some-result";
                })
                .thenRun(() -> {
                    System.out.println("  Cleanup / notification — no access to result.");
                });

        future.get();
    }

    // ──────────────────────────────────────────────
    // 5. Async variants — run on ForkJoinPool.commonPool()
    // ──────────────────────────────────────────────
    public static void demoAsyncVariants() throws ExecutionException, InterruptedException {
        System.out.println("\n─── Async Variants ───");

        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("  [supplyAsync]    Thread: " + Thread.currentThread().getName());
                    return "data";
                })
                .thenApplyAsync(data -> {
                    // thenApplyAsync runs on common pool, unlike thenApply which may
                    // run on the same thread that completed the previous stage.
                    System.out.println("  [thenApplyAsync] Thread: " + Thread.currentThread().getName());
                    return data.toUpperCase();
                })
                .thenAcceptAsync(upper -> {
                    System.out.println("  [thenAcceptAsync] Thread: " + Thread.currentThread().getName());
                    System.out.println("  Final: " + upper);
                });

        future.get();
    }

    // ──────────────────────────────────────────────
    // 6. thenCombine — combine results of TWO independent futures
    // ──────────────────────────────────────────────
    public static void demoThenCombine() throws ExecutionException, InterruptedException {
        System.out.println("\n─── thenCombine (combine two independent futures) ───");

        CompletableFuture<String> priceFuture = CompletableFuture.supplyAsync(() -> {
            sleep(150);
            return "₹1,200";
        });

        CompletableFuture<String> etaFuture = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "15 min";
        });

        CompletableFuture<String> combined = priceFuture.thenCombine(etaFuture,
                (price, eta) -> String.format("Uber: %s, arriving in %s", price, eta));

        System.out.println("  " + combined.get());
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
        demoThenApply();
        demoThenCompose();
        demoThenAccept();
        demoThenRun();
        demoAsyncVariants();
        demoThenCombine();
    }
}
