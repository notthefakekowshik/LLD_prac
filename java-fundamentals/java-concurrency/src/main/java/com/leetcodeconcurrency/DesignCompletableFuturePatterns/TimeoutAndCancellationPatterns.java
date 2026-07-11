package com.leetcodeconcurrency.DesignCompletableFuturePatterns;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CompletableFuture Timeout & Cancellation Patterns
 *
 * orTimeout()       → completes exceptionally with TimeoutException after duration
 * completeOnTimeout() → completes with default value after duration
 * cancel()          → attempts to cancel the future (mayInterruptIfRunning flag)
 * Future.get(timeout) → blocking get with timeout
 *
 * Java 9+: orTimeout / completeOnTimeout
 * Pre-Java 9: accepEither + CompletableFuture.delayedExecutor fallback
 */
public class TimeoutAndCancellationPatterns {

    public static void main(String[] args) throws Exception {
        demoOrTimeout();
        demoCompleteOnTimeout();
        demoLegacyTimeout();
        demoCancel();
        demoCancellingSlowRunners();
        demoStructuredShutdown();
    }

    // ──────────────────────────────────────────────
    // 1. orTimeout — fail fast if operation takes too long (Java 9+)
    // ──────────────────────────────────────────────
    public static void demoOrTimeout() {
        System.out.println("─── orTimeout (Java 9+) ───");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    sleep(5000); // Simulate very slow operation
                    return "Too late";
                })
                .orTimeout(50000, TimeUnit.MILLISECONDS);

        try {
            String returnedString = future.get();
            System.out.println("  Result: " + returnedString);
        } catch (Exception e) {
            System.out.println("  Exception: " + e.getCause().getClass().getSimpleName()
                    + " — " + e.getCause().getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 2. completeOnTimeout — provide default on timeout (Java 9+)
    // ──────────────────────────────────────────────
    public static void demoCompleteOnTimeout() {
        System.out.println("\n─── completeOnTimeout (Java 9+) ───");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    sleep(5000);
                    return "Primary data";
                })
                .completeOnTimeout("DEFAULT_DATA", 500, TimeUnit.MILLISECONDS);

        String result = future.join();
        System.out.println("  Result: " + result);
    }

    // ──────────────────────────────────────────────
    // 3. Pre-Java 9: timeout via acceptEither + delayedExecutor
    // ──────────────────────────────────────────────
    public static void demoLegacyTimeout() {
        System.out.println("\n─── Legacy timeout (acceptEither trick) ───");

        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            sleep(5000);
            return "Primary";
        });

        CompletableFuture<String> timeout = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            throw new RuntimeException("Operation timed out");
        });

        // acceptEither: whichever completes first, runs the consumer
        CompletableFuture<Void> result = task.acceptEither(timeout, val ->
                System.out.println("  Got: " + val)
        );

        try {
            result.get();
        } catch (Exception e) {
            System.out.println("  Caught: " + e.getCause().getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 4. cancel() — attempt to cancel running future
    // ──────────────────────────────────────────────
    public static void demoCancel() {
        System.out.println("\n─── cancel() ───");

        CompletableFuture<String> future = new CompletableFuture<>();

        Thread worker = new Thread(() -> {
            try {
                // Simulate work
                Thread.sleep(2000);
                future.complete("Done!");
            } catch (InterruptedException e) {
                // Honoring interruption
                System.out.println("  Worker interrupted, cancelling...");
                future.completeExceptionally(e);
            }
        });
        worker.start();

        sleep(200);
        boolean cancelled = future.cancel(true); // true = mayInterruptIfRunning

        System.out.println("  cancelled: " + cancelled);
        System.out.println("  isCancelled: " + future.isCancelled());
        System.out.println("  isCompletedExceptionally: " + future.isCompletedExceptionally());

        try {
            future.get();
        } catch (Exception e) {
            System.out.println("  get() threw: " + e.getClass().getSimpleName());
        }
    }

    // ──────────────────────────────────────────────
    // 5. Cancelling one of many — race to cancel slow runners
    // ──────────────────────────────────────────────
    public static void demoCancellingSlowRunners() {
        System.out.println("\n─── Cancelling slow runners ───");

        CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "FAST";
        });

        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(5000);
            return "SLOW";
        });

        // When fast completes, cancel slow
        fast.thenAccept(result -> {
            System.out.println("  Fast completed: " + result);
            boolean cancelled = slow.cancel(true);
            System.out.println("  Slow cancelled: " + cancelled);
        });

        fast.join();
        sleep(500); // Give cancel some time
        System.out.println("  Slow isCancelled: " + slow.isCancelled());
    }

    // ──────────────────────────────────────────────
    // 6. Structured shutdown — completion with timeout
    // ──────────────────────────────────────────────
    public static void demoStructuredShutdown() throws Exception {
        System.out.println("\n─── Structured shutdown ───");

        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < 100; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("  Task detected interrupt, cleaning up...");
                    return "CLEANED_UP";
                }
                sleep(100);
            }
            return "COMPLETED";
        });

        sleep(500);
        boolean cancelled = task.cancel(true);
        System.out.println("  cancel() returned: " + cancelled);

        try {
            String result = task.get();
            System.out.println("  Result: " + result);
        } catch (Exception e) {
            System.out.println("  Caught: " + e.getCause().getClass().getSimpleName());
        }
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

}
