package com.kowshik.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CyclicBarrier Demo - Reusable Synchronization Barrier Tutorial
 *
 * CyclicBarrier allows a set of threads to wait for each other to reach
 * a common barrier point. Unlike CountDownLatch, it can be reused.
 *
 * Key Points:
 * - Threads wait at barrier until all parties arrive
 * - Can execute a barrier action when all threads arrive
 * - Reusable - can be reset and used again
 * - await() blocks until all threads arrive
 *
 * Use Cases:
 * - Multi-phase parallel algorithms
 * - Iterative computations where threads must synchronize
 * - Coordinating parallel matrix operations
 * - Game turn-based systems
 */
public class CyclicBarrierDemo {

    private static final Logger logger = LoggerFactory.getLogger(CyclicBarrierDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== CyclicBarrier Demo ===\n");

        // Demo 1: Basic barrier usage
        basicBarrierDemo();

        Thread.sleep(2000);

        // Demo 2: Multi-phase computation with reuse
        multiPhaseComputationDemo();
    }

    /**
     * Demo 1: Basic barrier with action
     */
    private static void basicBarrierDemo() throws InterruptedException {
        logger.info("Demo 1: Basic CyclicBarrier with barrier action");

        int parties = 3;

        /*
         * you need not use this variable r, you can directly have it as an anonymous
         * function (lambda, you did this for fun!)
         */
        Runnable r = () -> {
            logger.info("*** ALL THREADS ARRIVED AT BARRIER - Barrier action executing ***");
        };
        CyclicBarrier barrier = new CyclicBarrier(parties, r);

        ExecutorService executor = Executors.newFixedThreadPool(parties);

        for (int i = 1; i <= parties; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    logger.info("Thread {} doing work before barrier", threadId);
                    Thread.sleep((long) (Math.random() * 2000));

                    logger.info("Thread {} waiting at barrier", threadId);
                    barrier.await(); // Wait for others

                    logger.info("Thread {} passed barrier, continuing work", threadId);
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Thread {} interrupted", threadId, e);
                }
            });
        }

        executor.shutdown();
        Thread.sleep(5000);
        logger.info("");
    }

    /**
     * Demo 2: Multi-phase computation showing reusability
     */
    private static void multiPhaseComputationDemo() throws InterruptedException {
        logger.info("Demo 2: Multi-phase computation (barrier reuse)");

        int workers = 4;
        int phases = 3;
        int[] current_phase = new int[] { 0 };
        CyclicBarrier barrier = new CyclicBarrier(workers, () -> {
            logger.info("==> Phase {} completed - all workers synchronized", ++current_phase[0]);
        });

        ExecutorService executor = Executors.newFixedThreadPool(workers);

        for (int i = 1; i <= workers; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    for (int phase = 1; phase <= phases; phase++) {
                        logger.info("Worker {} executing phase {}", workerId, phase);
                        Thread.sleep((long) (Math.random() * 1500));

                        logger.info("Worker {} waiting at barrier (phase {})", workerId, phase);
                        barrier.await(); // Wait for all workers to complete this phase

                        logger.info("Worker {} proceeding to next phase {}", workerId, phase + 1);
                    }
                    logger.info("Worker {} completed all phases", workerId);
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Worker {} error", workerId, e);
                }
            });
        }

        executor.shutdown();
    }

    /**
     * Example: Matrix multiplication with parallel computation
     * Each thread computes part of the result and waits for others before next
     * phase
     */
    static class ParallelMatrixComputation {
        private final CyclicBarrier barrier;
        private final int[][] matrix;
        private final int workers;

        public ParallelMatrixComputation(int size, int workers) {
            this.workers = workers;
            this.matrix = new int[size][size];

            this.barrier = new CyclicBarrier(workers, () -> {
                logger.info("Phase completed - matrix state synchronized");
            });
        }

        public void compute() {
            ExecutorService executor = Executors.newFixedThreadPool(workers);

            for (int i = 0; i < workers; i++) {
                final int workerId = i;
                executor.submit(() -> {
                    try {
                        // Phase 1: Initialize
                        initializePartition(workerId);
                        barrier.await();

                        // Phase 2: Compute
                        computePartition(workerId);
                        barrier.await();

                        // Phase 3: Finalize
                        finalizePartition(workerId);
                        barrier.await();

                    } catch (InterruptedException | BrokenBarrierException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            executor.shutdown();
        }

        private void initializePartition(int workerId) {
            logger.info("Worker {} initializing partition", workerId);
        }

        private void computePartition(int workerId) {
            logger.info("Worker {} computing partition", workerId);
        }

        private void finalizePartition(int workerId) {
            logger.info("Worker {} finalizing partition", workerId);
        }
    }
}
