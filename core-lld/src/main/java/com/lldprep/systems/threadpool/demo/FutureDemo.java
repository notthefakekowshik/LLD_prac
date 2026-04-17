package com.lldprep.systems.threadpool.demo;

import com.lldprep.systems.threadpool.CustomThreadPool;
import com.lldprep.systems.threadpool.FixedThreadPool;
import com.lldprep.systems.threadpool.factory.DefaultThreadFactory;
import com.lldprep.systems.threadpool.future.CustomFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrates Callable/Future support for retrieving task results.
 * 
 * Features:
 * - Submit Callable tasks
 * - Retrieve results via Future.get()
 * - Handle task exceptions
 * - Timeout on get()
 * - Task cancellation
 */
public class FutureDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Future/Callable Demo ===\n");
        
        CustomThreadPool pool = new FixedThreadPool.Builder()
            .poolSize(3)
            .threadFactory(new DefaultThreadFactory("FuturePool"))
            .build();
        
        demonstrateBasicFuture(pool);
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        demonstrateExceptionHandling(pool);
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        demonstrateTimeout(pool);
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        demonstrateCancellation(pool);
        
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    private static void demonstrateBasicFuture(CustomThreadPool pool) {
        System.out.println("1. Basic Future - Computing factorial\n");
        
        List<CustomFuture<Integer>> futures = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            final int n = i;
            CustomFuture<Integer> future = pool.submit(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "] Computing factorial(" + n + ")");
                Thread.sleep(500);
                return factorial(n);
            });
            futures.add(future);
        }
        
        for (int i = 0; i < futures.size(); i++) {
            try {
                Integer result = futures.get(i).get();
                System.out.println("factorial(" + (i + 1) + ") = " + result);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error getting result: " + e.getMessage());
            }
        }
    }
    
    private static void demonstrateExceptionHandling(CustomThreadPool pool) {
        System.out.println("2. Exception Handling - Task throws exception\n");
        
        CustomFuture<Integer> future = pool.submit(() -> {
            System.out.println("[" + Thread.currentThread().getName() + "] About to throw exception");
            Thread.sleep(200);
            throw new RuntimeException("Simulated task failure");
        });
        
        try {
            Integer result = future.get();
            System.out.println("Result: " + result);
        } catch (ExecutionException e) {
            System.out.println("Caught ExecutionException: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted: " + e.getMessage());
        }
    }
    
    private static void demonstrateTimeout(CustomThreadPool pool) {
        System.out.println("3. Timeout - get() with timeout\n");
        
        CustomFuture<String> future = pool.submit(() -> {
            System.out.println("[" + Thread.currentThread().getName() + "] Long running task...");
            Thread.sleep(5000);
            return "Task completed";
        });
        
        try {
            String result = future.get(1, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
        } catch (TimeoutException e) {
            System.out.println("Task timed out after 1 second");
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void demonstrateCancellation(CustomThreadPool pool) throws InterruptedException {
        System.out.println("4. Cancellation - Cancel a running task\n");
        
        CustomFuture<String> future = pool.submit(() -> {
            System.out.println("[" + Thread.currentThread().getName() + "] Task started");
            for (int i = 0; i < 10; i++) {
                if (Thread.interrupted()) {
                    System.out.println("[" + Thread.currentThread().getName() + "] Task was interrupted");
                    throw new InterruptedException("Task cancelled");
                }
                Thread.sleep(500);
            }
            return "Task completed";
        });
        
        Thread.sleep(1000);
        
        boolean cancelled = future.cancel(true);
        System.out.println("Task cancelled: " + cancelled);
        System.out.println("Is cancelled: " + future.isCancelled());
        
        try {
            future.get();
        } catch (ExecutionException e) {
            System.out.println("Task was cancelled: " + e.getCause().getMessage());
        }
    }
    
    private static int factorial(int n) {
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }
}
