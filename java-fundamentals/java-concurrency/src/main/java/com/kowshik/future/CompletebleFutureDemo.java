package com.kowshik.future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * CompletableFuture Demo - Asynchronous Programming Tutorial
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. What is CompletableFuture and why use it over Future?
 * - Non-blocking asynchronous programming
 * - Composable and chainable operations
 * - Exception handling with exceptionally/handle
 * - Combining multiple futures (thenCombine, allOf, anyOf)
 *
 * 2. Common Interview Questions:
 * - Difference between supplyAsync() and runAsync()
 * - When to provide custom executor vs using common pool?
 * - How to handle exceptions in CompletableFuture?
 * - Explain thenApply() vs thenCompose() vs thenCombine()
 * - What is the difference between thenAccept() and thenApply()?
 * - How does CompletableFuture avoid callback hell?
 *
 * 3. Real-world Use Cases:
 * - Microservices orchestration
 * - Parallel API calls
 * - Database + cache queries in parallel
 * - Timeout handling with orTimeout()
 *
 * 4. Common Pitfalls:
 * - Not shutting down custom executors
 * - Blocking operations in async chain
 * - ForkJoinPool.commonPool() starvation
 * - Not handling exceptions properly
 *
 * Key Methods to Know:
 * - supplyAsync/runAsync: Start async computation
 * - thenApply/thenAccept/thenRun: Transform/consume results
 * - thenCompose: Chain dependent futures
 * - thenCombine: Combine independent futures
 * - exceptionally/handle: Error handling
 * - allOf/anyOf: Multiple futures coordination
 */

public class CompletebleFutureDemo {

    private static final Logger log = LoggerFactory.getLogger(CompletebleFutureDemo.class);

    /**
     * Demonstrates the difference between using ForkJoinPool.commonPool()
     * vs providing a custom executor for CompletableFuture operations.
     *
     * @param args command line arguments
     * @throws InterruptedException if interrupted while waiting
     */
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Create our OWN dedicated thread pool
        ExecutorService myCustomExecutor = Executors.newFixedThreadPool(4);
        System.out.println("Created a custom executor with 4 threads.");

        // Version 0: The Old Way (Future)
        // Limitation: It blocks the main thread until the result is available.
        System.out.println("\n--- Future Demo ---");
        Future<String> future = myCustomExecutor.submit(() -> {
            try {
                Thread.sleep(5000); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Future Task running in thread: " + Thread.currentThread().getName();
        });

        System.out.println("Future submitted. Main thread doing other work...");
        // This line BLOCKS until the result is ready.
        // We cannot chain actions or handle exceptions gracefully without try-catch
        // here.
        String result = future.get();
        System.out.println("Future Result: " + result);
        System.out.println("--- End Future Demo ---\n");

        // Version 1: Using the default common pool
        CompletableFuture.supplyAsync(() -> {
            // This task will run in the ForkJoinPool.commonPool()
            return "Task running in thread: " + Thread.currentThread().getName();
        }).thenAccept(System.out::println);

        // Version 2: Providing OUR executor (The better way for serious apps)
        CompletableFuture.supplyAsync(() -> {
            // This task is guaranteed to run in OUR executor
            return "Task running in thread: " + Thread.currentThread().getName();
        }, myCustomExecutor).thenAccept(System.out::println); // Pass the executor here

        // Clean up our custom executor
        myCustomExecutor.shutdown();
        myCustomExecutor.awaitTermination(2, TimeUnit.SECONDS);
    }
}
