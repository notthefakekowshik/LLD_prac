package com.lldprep.systems.threadpool;

import com.lldprep.systems.threadpool.factory.DefaultThreadFactory;
import com.lldprep.systems.threadpool.policy.AbortPolicy;

import java.util.concurrent.TimeUnit;

/**
 * Main class to demonstrate the refactored Custom Thread Pool implementation.
 * 
 * For comprehensive demos, see:
 * - demo.BasicThreadPoolDemo - Basic usage with metrics
 * - demo.RejectionPolicyDemo - Different rejection strategies
 * - demo.FutureDemo - Callable/Future support
 */
public class ThreadPoolMain {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Custom Thread Pool Demo ===");
        System.out.println("(Run demo classes for comprehensive examples)\n");
        
        CustomThreadPool threadPool = new FixedThreadPool.Builder()
            .poolSize(3)
            .queueCapacity(5)
            .rejectionPolicy(new AbortPolicy())
            .threadFactory(new DefaultThreadFactory("MainPool"))
            .build();
        
        System.out.println("Initial metrics: " + threadPool.getMetrics() + "\n");
        
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            threadPool.submit(() -> {
                System.out.println(Thread.currentThread().getName() + " is executing Task-" + taskId);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println(Thread.currentThread().getName() + " was interrupted while executing Task-" + taskId);
                }
                System.out.println(Thread.currentThread().getName() + " completed Task-" + taskId);
            });
        }
        
        System.out.println("\nAll tasks submitted.");
        System.out.println("Metrics after submission: " + threadPool.getMetrics() + "\n");
        
        Thread.sleep(1000);
        
        System.out.println("\nCalling shutdown()...");
        threadPool.shutdown();
        
        try {
            threadPool.submit(() -> System.out.println("This should not run"));
        } catch (Exception e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
        
        System.out.println("\nWaiting for termination...");
        boolean terminated = threadPool.awaitTermination(10, TimeUnit.SECONDS);
        
        if (terminated) {
            System.out.println("\nPool terminated successfully.");
            System.out.println("Final metrics: " + threadPool.getMetrics());
        }
    }
}
