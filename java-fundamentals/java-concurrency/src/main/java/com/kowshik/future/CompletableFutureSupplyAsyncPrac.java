package com.kowshik.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CompletableFutureSupplyAsyncPrac {

    public static void main(String[] args) {
        // 1. Create the custom I/O pool
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        System.out.println("1. Submitting tasks...");

        // ==========================================
        // SCENARIO 1: Wrapping a Synchronous Call
        // ==========================================
        // We use supplyAsync because the method itself is blocking.
        // We push that blocking behavior onto the executorService.
        CompletableFuture<String> userNameSyncCF = CompletableFuture.supplyAsync(() -> {
            return getUserNameSync(1);
        }, executorService);

        userNameSyncCF.thenAccept(user -> {
            System.out.println("Sync Scenario Result: User name is " + user);
        });

        // ==========================================
        // SCENARIO 2: Calling an Asynchronous Call
        // ==========================================
        // We DO NOT use supplyAsync here!
        // The method already returns a CompletableFuture and handles its own async
        // execution.
        CompletableFuture<String> userNameAsyncCF = getUserNameAsync(2, executorService);

        userNameAsyncCF.thenAccept(user -> {
            System.out.println("Async Scenario Result: User name is " + user);
        });

        System.out.println("2. Main thread is free and moving on!");

        // 3. Graceful Shutdown & Wait
        // We must shut down the executor so the JVM can exit cleanly.
        executorService.shutdown();

        try {
            // Block the main thread just to keep the program alive long enough
            // to see the print statements from the background threads.
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted while waiting.");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("3. Program finished gracefully.");
    }

    /**
     * A purely synchronous, blocking method.
     */
    private static String getUserNameSync(int id) {
        try {
            Thread.sleep(3000); // Simulating 3-second DB call
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state
            throw new RuntimeException("DB call interrupted", e);
        }
        return "kowshik_sync";
    }

    /**
     * A true asynchronous method.
     * It returns IMMEDIATELY. The heavy lifting happens inside the Future.
     */
    private static CompletableFuture<String> getUserNameAsync(int id, ExecutorService pool) {
        // We push the blocking DB call INSIDE the supplyAsync lambda
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000); // Simulating 3-second DB call
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("DB call interrupted", e);
            }
            return "kowshik_async";
        }, pool);
    }
}

/*
 * =============================================================================
 * ==========
 * ARCHITECTURAL SUMMARY: WHICH APPROACH FITS YOUR CODE?
 * =============================================================================
 * ==========
 * * 1. Using getUserNameSync (The Bulkhead Pattern)
 * - Best for: Legacy, blocking operations (e.g., standard JDBC, Thread.sleep).
 * - How to use: Wrap it via `CompletableFuture.supplyAsync(..., customPool)`.
 * - Why: Pushes the blocking behavior onto a dedicated background thread,
 * protecting the main application (e.g., Tomcat) from thread starvation.
 * * 2. Using getUserNameAsync (Native Async)
 * - Best for: Modern reactive drivers (e.g., R2DBC, Java 11+ HttpClient).
 * - How to use: Call it directly! DO NOT wrap it in `supplyAsync`.
 * Example: `getUserNameAsync(1).thenAccept(...)`
 * - The Trap: Never wrap an already-async method in `supplyAsync` just to call
 * `.join()` on it. This wastes a thread context entirely. If you need to chain
 * multiple async operations, use `.thenCompose()` to flatten them naturally.
 * * 3. Lifecycle Management
 * - Always shut down your custom `ExecutorService` (preferably in a finally
 * block
 * or via Spring/framework lifecycle hooks) to prevent JVM memory leaks.
 * =============================================================================
 * ==========
 */