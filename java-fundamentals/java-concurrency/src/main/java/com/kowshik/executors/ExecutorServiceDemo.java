package com.kowshik.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ExecutorService Demo - Thread Pool Management Tutorial
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. Why use ExecutorService instead of creating raw threads?
 * - Thread reuse and pool management
 * - Reduced overhead of thread creation/destruction
 * - Better resource management and control
 * - Queue-based task submission
 *
 * 2. Common Interview Questions:
 * - Difference between execute() and submit()?
 * - Types of thread pools: FixedThreadPool, CachedThreadPool,
 * SingleThreadExecutor, ScheduledThreadPool
 * - What happens when you submit more tasks than pool size?
 * - Explain shutdown() vs shutdownNow()
 * - What is the difference between Runnable and Callable?
 * - How to handle rejected execution?
 * - What is ThreadPoolExecutor and its core parameters?
 *
 *
 * 3. Key Differences:
 * execute(): From Executor interface, returns void, accepts only Runnable
 * submit(): From ExecutorService, returns Future<?>, accepts Runnable or
 * Callable
 *
 * 4. Thread Pool Types:
 * - newFixedThreadPool(n): Fixed number of threads, unbounded queue
 * - newCachedThreadPool(): Creates threads as needed, reuses idle threads
 * - newSingleThreadExecutor(): Single worker thread, sequential execution
 * - newScheduledThreadPool(n): For delayed/periodic task execution
 *
 * 5. Best Practices:
 * - Always shutdown executor service
 * - Use awaitTermination() to wait for tasks to complete
 * - Handle InterruptedException properly
 * - Consider custom ThreadPoolExecutor for fine-grained control
 *
 * 6. Common Pitfalls:
 * - Forgetting to shutdown executor (app won't terminate)
 * - Not handling exceptions in submitted tasks
 * - Using unbounded queues leading to OOM
 */
public class ExecutorServiceDemo {

    /**
     * Demonstrates the benefits of using ExecutorService with a thread pool
     * compared to creating individual threads for each task.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // 1. Create a thread pool with a fixed number of threads (e.g., 10)
        // The JVM doesn't have to create and destroy threads constantly.
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // 2. Submit 100 tasks to the executor service
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Executing task " + taskId + " in thread: " + Thread.currentThread().getName());
                try {
                    // Simulate work
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 3. Shut down the executor service gracefully
        // This is crucial! Otherwise, your application will not terminate.
        System.out.println("All tasks submitted. Shutting down executor.");
        executor.shutdown(); // Disables new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("All tasks completed.");
    }
}