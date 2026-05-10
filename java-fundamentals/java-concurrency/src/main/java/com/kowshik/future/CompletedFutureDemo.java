package com.kowshik.future;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Demo for {@link CompletableFuture#completedFuture(Object)}.
 *
 * <p><b>What it does:</b> Returns an already-completed CompletableFuture with the given value.
 * No async thread is spawned. The future is in the COMPLETED state immediately.</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Cache hits — value is already in memory, no need for async lookup.</li>
 *   <li>Early-return / validation failures — return a known default without blocking.</li>
 *   <li>Testing — mock dependencies that are supposed to be async.</li>
 *   <li>Composing pipelines — start a chain from a known seed value.</li>
 * </ul>
 */
public class CompletedFutureDemo {

    private static final Map<String, String> userCache = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture.completedFuture Demo ===\n");

        demo1_basicUsage();
        demo2_cacheHit();
        demo3_earlyReturnValidation();
        demo4_mockingInTests();
        demo5_pipelineFromKnownValue();

        System.out.println("\n=== All demos finished ===");
    }

    // --- Demo 1: Basic usage --------------------------------------------------
    private static void demo1_basicUsage() {
        System.out.println("--- Demo 1: Basic Usage ---");

        CompletableFuture<String> future = CompletableFuture.completedFuture("Hello, World!");

        System.out.println("Is done? " + future.isDone());          // true
        System.out.println("Result:  " + future.join());             // Hello, World!

        // Chaining still works — callbacks run immediately on the calling thread
        future.thenApply(String::toUpperCase)
              .thenAccept(result -> System.out.println("Upper:   " + result));

        System.out.println();
    }

    // --- Demo 2: Cache hit scenario ------------------------------------------
    private static void demo2_cacheHit() throws Exception {
        System.out.println("--- Demo 2: Cache Hit (no async thread needed) ---");

        userCache.put("user-42", "Alice");

        CompletableFuture<String> result = fetchUser("user-42");

        System.out.println("Fetching cached user...");
        System.out.println("Result: " + result.get(0, TimeUnit.SECONDS)); // instant, no waiting
        System.out.println("No thread was spawned for the cache hit.");
        System.out.println();
    }

    // Simulate a service that checks cache first, falls back to async DB call
    static CompletableFuture<String> fetchUser(String userId) {
        String cached = userCache.get(userId);
        if (cached != null) {
            // Cache hit: return already-completed future — no async overhead
            return CompletableFuture.completedFuture(cached);
        }
        // Cache miss: real async work
        return CompletableFuture.supplyAsync(() -> simulateDbLookup(userId));
    }

    // --- Demo 3: Early return / validation ------------------------------------
    private static void demo3_earlyReturnValidation() throws Exception {
        System.out.println("--- Demo 3: Early Return on Validation ---");

        CompletableFuture<String> emptyOrder = placeOrder("");
        CompletableFuture<String> validOrder = placeOrder("Burger");

        System.out.println("Empty order result: " + emptyOrder.join());
        System.out.println("Valid order result: " + validOrder.join());
        System.out.println();
    }

    static CompletableFuture<String> placeOrder(String item) {
        if (item == null || item.isBlank()) {
            // Fail fast with a known error — no thread pool used
            return CompletableFuture.completedFuture("REJECTED: Empty order");
        }
        return CompletableFuture.supplyAsync(() -> "CONFIRMED: " + item);
    }

    // --- Demo 4: Mocking async dependencies in tests -------------------------
    private static void demo4_mockingInTests() {
        System.out.println("--- Demo 4: Mocking Async Dependencies (Testing) ---");

        // In a real test, you don't want to spin up threads or hit real services.
        // completedFuture lets you mock an async dependency trivially.
        PaymentService mockService = new PaymentService();

        CompletableFuture<String> receipt = mockService.charge("mock-token-123");

        System.out.println("Mock payment receipt: " + receipt.join());
        System.out.println("Test ran synchronously — no ExecutorService needed.");
        System.out.println();
    }

    static class PaymentService {
        CompletableFuture<String> charge(String token) {
            // In production: supplyAsync(() -> callPaymentGateway(token))
            // In tests: return completedFuture("fake-receipt")
            return CompletableFuture.completedFuture("RECEIPT-" + token);
        }
    }

    // --- Demo 5: Pipeline from a known seed value ---------------------------
    private static void demo5_pipelineFromKnownValue() {
        System.out.println("--- Demo 5: Pipeline from Known Seed Value ---");

        // Sometimes you want to enter an async pipeline with a value you already have.
        int basePrice = 100;

        CompletableFuture.completedFuture(basePrice)
                .thenApply(price -> price * 0.9)          // apply discount
                .thenApply(discounted -> discounted + 5)   // add shipping
                .thenAccept(finalPrice ->
                        System.out.println("Final price: $" + String.format("%.2f", finalPrice)));

        System.out.println();
    }

    // --- Helpers --------------------------------------------------------------
    private static String simulateDbLookup(String userId) {
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return "DB-User-" + userId;
    }
}
