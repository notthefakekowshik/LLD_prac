package com.leetcodeconcurrency.DesignCompletableFuturePatterns;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * CompletableFuture Error Handling Patterns
 *
 * exceptionally   → recover from exception, return fallback value   (like catch)
 * handle          → always called (success OR failure), can transform both (like finally + catch)
 * whenComplete    → side-effect ONLY, does NOT change result         (like finally)
 *
 * Interview classic: "What's the difference between exceptionally and handle?"
 *   exceptionally: only called on failure, only takes Throwable, returns fallback VALUE
 *   handle:        called on BOTH success and failure, takes (result, throwable), returns VALUE
 *   whenComplete:  called on BOTH, takes (result, throwable), returns NOTHING (BiConsumer)
 */
public class ErrorHandlingPatterns {

    // ──────────────────────────────────────────────
    // 1. exceptionally — recover from failure
    // ──────────────────────────────────────────────
    public static void demoExceptionally() throws ExecutionException, InterruptedException {
        System.out.println("─── exceptionally (recovery) ───");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    if (true) throw new RuntimeException("DB connection refused");
                    return "success";
                })
                .exceptionally(ex -> {
                    System.out.println("  Recovered from: " + ex.getMessage());
                    return "FALLBACK_DATA";
                });

        System.out.println("  Result: " + future.get());
    }

    // ──────────────────────────────────────────────
    // 2. handle — always called, success or failure
    // ──────────────────────────────────────────────
    public static void demoHandle() throws ExecutionException, InterruptedException {
        System.out.println("\n─── handle (always invoked) ───");

        // Handle on SUCCESS
        CompletableFuture<String> successCase = CompletableFuture
                .supplyAsync(() -> "OK")
                .handle((result, ex) -> {
                    if (ex != null) return "Fallback from: " + ex.getMessage();
                    return result + " (processed by handle)";
                });
        System.out.println("  Success: " + successCase.get());

        // Handle on FAILURE
        CompletableFuture<String> failureCase = CompletableFuture
                .supplyAsync(() -> {
                    throw new RuntimeException("Boom!");
                })
                .handle((result, ex) -> {
                    if (ex != null) return "Recovered from: " + ex.getMessage();
                    return "Should not reach here";
                });
        System.out.println("  Failure: " + failureCase.get());
    }

    // ──────────────────────────────────────────────
    // 3. whenComplete — side-effect only (logging, cleanup)
    // ──────────────────────────────────────────────
    public static void demoWhenComplete() throws ExecutionException, InterruptedException {
        System.out.println("\n─── whenComplete (side-effect only) ───");

        // whenComplete is a BiConsumer — it cannot change the result
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> "data")
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        System.out.println("  [LOG] Successfully got: " + result);
                    } else {
                        System.out.println("  [LOG] Failed with: " + ex.getMessage());
                    }
                });

        System.out.println("  Result unchanged: " + future.get());
    }

    // ──────────────────────────────────────────────
    // 4. Chaining recovery — multiple fallback attempts
    // ──────────────────────────────────────────────
    public static void demoChainedFallback() {
        System.out.println("\n─── Chained fallback strategy ───");

        String result = fetchFromPrimary()
                .exceptionally(ex -> {
                    System.out.println("  [WARN] Primary failed: " + ex.getMessage());
                    return null; // signal to try next
                })
                .thenCompose(val -> {
                    if (val != null) return CompletableFuture.completedFuture(val);
                    return fetchFromSecondary();
                })
                .exceptionally(ex -> {
                    System.out.println("  [WARN] Secondary failed: " + ex.getMessage());
                    return null;
                })
                .thenCompose(val -> {
                    if (val != null) return CompletableFuture.completedFuture(val);
                    return fetchFromCacheOnly();
                })
                .join();

        System.out.println("  Final: " + result);
    }

    private static CompletableFuture<String> fetchFromPrimary() {
        return CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Primary DB timeout");
        });
    }

    private static CompletableFuture<String> fetchFromSecondary() {
        return CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Secondary DB timeout");
        });
    }

    private static CompletableFuture<String> fetchFromCacheOnly() {
        return CompletableFuture.completedFuture("CACHED_DATA");
    }

    // ──────────────────────────────────────────────
    // 5. CompletionException unwrapping
    //    When a stage throws, it's wrapped in CompletionException.
    //    get() unwraps it to ExecutionException with CompletionException as cause.
    // ──────────────────────────────────────────────
    public static void demoCompletionException() {
        System.out.println("\n─── CompletionException unwrapping ───");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    throw new RuntimeException("Stage-thrown");
                });

        try {
            future.get();
        } catch (ExecutionException e) {
            System.out.println("  ExecutionException cause: " + e.getCause().getClass().getSimpleName());

            // Walk the cause chain — depth varies by JDK version
            Throwable root = e.getCause();
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            System.out.println("  Root cause: " + root.getClass().getSimpleName() + " — " + root.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ──────────────────────────────────────────────
    // 6. Retry pattern with CompletableFuture
    // ──────────────────────────────────────────────
    public static void demoRetry() {
        System.out.println("\n─── Retry pattern ───");

        // Deterministic: fails first 2 attempts, succeeds on 3rd
        String result = retryWithBackoff(new Supplier<>() {
            int callCount = 0;
            public String get() {
                callCount++;
                if (callCount < 3) throw new RuntimeException("Transient failure (attempt " + callCount + ")");
                return "SUCCESS on attempt " + callCount;
            }
        }, 3).join();

        System.out.println("  Retry result: " + result);
    }

    private static CompletableFuture<String> retryWithBackoff(
            Supplier<String> task, int maxAttempts) {

        CompletableFuture<String> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> attempt(future, task, 1, maxAttempts));

        return future;
    }

    private static void attempt(CompletableFuture<String> result,
                                Supplier<String> task, int attempt, int maxAttempts) {
        try {
            String value = task.get();
            result.complete(value);
        } catch (Exception e) {
            if (attempt >= maxAttempts) {
                result.completeExceptionally(e);
            } else {
                long backoff = (long) Math.pow(2, attempt) * 100;
                System.out.println("  Attempt " + attempt + " failed, retrying in " + backoff + "ms...");
                sleep(backoff);
                attempt(result, task, attempt + 1, maxAttempts);
            }
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

    public static void main(String[] args) throws Exception {
        demoExceptionally();
        demoHandle();
        demoWhenComplete();
        demoChainedFallback();
        demoCompletionException();
        demoRetry();
    }
}
