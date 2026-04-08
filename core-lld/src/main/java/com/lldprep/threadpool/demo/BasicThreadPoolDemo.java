package com.lldprep.threadpool.demo;

import com.lldprep.threadpool.CustomThreadPool;
import com.lldprep.threadpool.FixedThreadPool;
import com.lldprep.threadpool.factory.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;

/**
 * Basic demonstration of FixedThreadPool with:
 * - Task submission
 * - Graceful shutdown
 * - awaitTermination
 * - Metrics monitoring
 */
public class BasicThreadPoolDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Basic Thread Pool Demo ===\n");
        
        CustomThreadPool pool = new FixedThreadPool.Builder()
            .poolSize(3)
            .queueCapacity(10)
            .threadFactory(new DefaultThreadFactory("BasicPool"))
            .build();
        
        System.out.println("Initial metrics: " + pool.getMetrics());
        System.out.println();
        
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("[" + Thread.currentThread().getName() + "] Executing Task-" + taskId);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("[" + Thread.currentThread().getName() + "] Task-" + taskId + " interrupted");
                }
                System.out.println("[" + Thread.currentThread().getName() + "] Completed Task-" + taskId);
            });
        }
        
        System.out.println("\nAll tasks submitted.");
        System.out.println("Metrics after submission: " + pool.getMetrics());
        System.out.println();
        
        Thread.sleep(1000);
        
        System.out.println("\nMetrics during execution: " + pool.getMetrics());
        System.out.println();
        
        System.out.println("Calling shutdown()...");
        pool.shutdown();
        
        try {
            pool.submit(() -> System.out.println("This should not run"));
        } catch (Exception e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
        
        System.out.println("\nWaiting for termination (timeout: 10 seconds)...");
        boolean terminated = pool.awaitTermination(10, TimeUnit.SECONDS);
        
        if (terminated) {
            System.out.println("Pool terminated successfully.");
            System.out.println("Final metrics: " + pool.getMetrics());
        } else {
            System.out.println("Pool did not terminate within timeout.");
        }
    }
}
