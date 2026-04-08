package com.lldprep.threadpool.demo;

import com.lldprep.threadpool.CustomThreadPool;
import com.lldprep.threadpool.FixedThreadPool;
import com.lldprep.threadpool.exception.RejectedExecutionException;
import com.lldprep.threadpool.factory.DefaultThreadFactory;
import com.lldprep.threadpool.policy.AbortPolicy;
import com.lldprep.threadpool.policy.CallerRunsPolicy;
import com.lldprep.threadpool.policy.DiscardPolicy;

import java.util.concurrent.TimeUnit;

/**
 * Demonstrates different rejection policies when queue is full.
 * 
 * Scenarios:
 * 1. AbortPolicy - Throws exception (fail-fast)
 * 2. CallerRunsPolicy - Runs in caller's thread (backpressure)
 * 3. DiscardPolicy - Silently drops task
 */
public class RejectionPolicyDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Rejection Policy Demo ===\n");
        
        demonstrateAbortPolicy();
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        demonstrateCallerRunsPolicy();
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        demonstrateDiscardPolicy();
    }
    
    private static void demonstrateAbortPolicy() throws InterruptedException {
        System.out.println("1. AbortPolicy - Throws exception when queue is full\n");
        
        CustomThreadPool pool = new FixedThreadPool.Builder()
            .poolSize(2)
            .queueCapacity(3)
            .rejectionPolicy(new AbortPolicy())
            .threadFactory(new DefaultThreadFactory("AbortPool"))
            .build();
        
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            try {
                pool.submit(() -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] Task-" + taskId);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                System.out.println("Task-" + taskId + " submitted successfully");
            } catch (RejectedExecutionException e) {
                System.out.println("Task-" + taskId + " REJECTED: " + e.getMessage());
            }
        }
        
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
    
    private static void demonstrateCallerRunsPolicy() throws InterruptedException {
        System.out.println("2. CallerRunsPolicy - Runs rejected task in caller's thread\n");
        
        CustomThreadPool pool = new FixedThreadPool.Builder()
            .poolSize(2)
            .queueCapacity(3)
            .rejectionPolicy(new CallerRunsPolicy())
            .threadFactory(new DefaultThreadFactory("CallerRunsPool"))
            .build();
        
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "] Executing Task-" + taskId);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
    
    private static void demonstrateDiscardPolicy() throws InterruptedException {
        System.out.println("3. DiscardPolicy - Silently discards rejected tasks\n");
        
        CustomThreadPool pool = new FixedThreadPool.Builder()
            .poolSize(2)
            .queueCapacity(3)
            .rejectionPolicy(new DiscardPolicy())
            .threadFactory(new DefaultThreadFactory("DiscardPool"))
            .build();
        
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "] Task-" + taskId);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            System.out.println("Submitted Task-" + taskId);
        }
        
        System.out.println("\nNote: Some tasks were silently discarded (no exception thrown)");
        
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("Completed tasks: " + pool.getCompletedTaskCount());
    }
}
