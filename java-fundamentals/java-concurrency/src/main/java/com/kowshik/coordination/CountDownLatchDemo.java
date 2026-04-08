package com.kowshik.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CountDownLatch Demo - Synchronization Aid Tutorial
 *
 * CountDownLatch allows one or more threads to wait until a set of operations
 * being performed in other threads completes.
 *
 * Key Points:
 * - Initialized with a count
 * - countDown() decrements the count
 * - await() blocks until count reaches zero
 * - Cannot be reused (one-time use)
 *
 * Use Cases:
 * - Wait for multiple threads to complete initialization
 * - Start multiple threads simultaneously (like a starting gun)
 * - Coordinate dependent operations
 */
public class CountDownLatchDemo {

    private static final Logger logger = LoggerFactory.getLogger(CountDownLatchDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== CountDownLatch Demo ===\n");

        // Demo 1: Wait for workers to complete
        waitForWorkersDemo();

        Thread.sleep(2000);

        // Demo 2: Simultaneous start (starting gun pattern)
        simultaneousStartDemo();
    }

    /**
     * Demo 1: Main thread waits for all worker threads to complete
     */
    private static void waitForWorkersDemo() throws InterruptedException {
        logger.info("Demo 1: Waiting for workers to complete");

        int workerCount = 5;
        CountDownLatch latch = new CountDownLatch(workerCount);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);

        for (int i = 1; i <= workerCount; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    logger.info("Worker {} started", workerId);
                    Thread.sleep((long) (Math.random() * 2000));
                    logger.info("Worker {} completed", workerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // Decrement count
                    logger.info("Latch count after worker {}: {}", workerId, latch.getCount());
                }
            });
        }

        logger.info("Main thread waiting for all workers to complete...");
        latch.await(); // Block until count reaches 0
        logger.info("All workers completed! Proceeding with main thread.\n");

        executor.shutdown();
    }

    /**
     * Demo 2: Starting gun pattern - all threads start simultaneously
     */
    private static void simultaneousStartDemo() throws InterruptedException {
        logger.info("Demo 2: Simultaneous start pattern (starting gun)");

        int runnerCount = 4;
        CountDownLatch startSignal = new CountDownLatch(1); // Start signal
        CountDownLatch doneSignal = new CountDownLatch(runnerCount); // Completion tracking

        ExecutorService executor = Executors.newFixedThreadPool(runnerCount);

        for (int i = 1; i <= runnerCount; i++) {
            final int runnerId = i;
            executor.submit(() -> {
                try {
                    logger.info("Runner {} ready at starting line", runnerId);
                    startSignal.await(); // Wait for start signal

                    logger.info("Runner {} STARTED!", runnerId);
                    Thread.sleep((long) (Math.random() * 1500));
                    logger.info("Runner {} finished", runnerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        Thread.sleep(1000); // Let all runners get ready
        logger.info("*** FIRING STARTING GUN ***");
        startSignal.countDown(); // Release all runners

        doneSignal.await(); // Wait for all runners to finish
        logger.info("Race completed!");

        executor.shutdown();
    }
}
