package com.kowshik.patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Production-like CyclicBarrier Demo: Parallel Data Processing Pipeline
 *
 * Simulates a real-world ETL (Extract, Transform, Load) pipeline where multiple
 * worker threads process batches of data through multiple stages. Workers must
 * synchronize at the end of each stage before proceeding to the next.
 *
 * Scenario: Processing customer orders in batches
 * - Stage 1: Extract and validate orders from source
 * - Stage 2: Transform and enrich order data
 * - Stage 3: Load processed orders to destination
 * - Stage 4: Generate reports and metrics
 *
 * Each worker processes a partition of the data, and all workers must complete
 * each stage before any worker can proceed to the next stage.
 */
public class DataPipelineBarrierDemo {

    private static final Logger logger = LoggerFactory.getLogger(DataPipelineBarrierDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== Production Data Pipeline with CyclicBarrier ===\n");

        // Simulate processing 1000 orders with 5 worker threads
        DataProcessingPipeline pipeline = new DataProcessingPipeline(1000, 5);
        pipeline.execute();

        Thread.sleep(1000);
        logger.info("\n=== Pipeline Execution Complete ===");
        pipeline.printStatistics();
    }

    /**
     * Represents a data processing pipeline with multiple synchronized stages
     */
    static class DataProcessingPipeline {
        private final int totalRecords;
        private final int numWorkers;
        private final CyclicBarrier barrier;
        private final ExecutorService executor;
        private final List<OrderBatch> orderBatches;
        private final AtomicInteger currentStage;
        private final PipelineMetrics metrics;

        public DataProcessingPipeline(int totalRecords, int numWorkers) {
            this.totalRecords = totalRecords;
            this.numWorkers = numWorkers;
            this.currentStage = new AtomicInteger(0);
            this.metrics = new PipelineMetrics();
            this.orderBatches = new ArrayList<>();
            this.executor = Executors.newFixedThreadPool(numWorkers);

            // Barrier action: executed when all workers complete a stage
            this.barrier = new CyclicBarrier(numWorkers, () -> {
                int stage = currentStage.incrementAndGet();
                String stageName = getStageName(stage);
                logger.info("╔════════════════════════════════════════════════════════╗");
                logger.info("║  STAGE {} COMPLETED: {} ", stage, stageName);
                logger.info("║  All {} workers synchronized - proceeding to next stage", numWorkers);
                logger.info("╚════════════════════════════════════════════════════════╝\n");
            });

            initializeBatches();
        }

        private void initializeBatches() {
            int recordsPerWorker = totalRecords / numWorkers;
            for (int i = 0; i < numWorkers; i++) {
                int startId = i * recordsPerWorker + 1;
                int endId = (i == numWorkers - 1) ? totalRecords : (i + 1) * recordsPerWorker;
                orderBatches.add(new OrderBatch(i + 1, startId, endId));
            }
        }

        public void execute() {
            logger.info("Starting pipeline with {} workers processing {} records\n", numWorkers, totalRecords);

            for (OrderBatch batch : orderBatches) {
                executor.submit(new PipelineWorker(batch));
            }

            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Pipeline execution interrupted", e);
            }
        }

        private String getStageName(int stage) {
            return switch (stage) {
                case 1 -> "EXTRACT & VALIDATE";
                case 2 -> "TRANSFORM & ENRICH";
                case 3 -> "LOAD TO DESTINATION";
                case 4 -> "GENERATE REPORTS";
                default -> "UNKNOWN";
            };
        }

        public void printStatistics() {
            logger.info("Pipeline Statistics:");
            logger.info("  Total Records Processed: {}", totalRecords);
            logger.info("  Workers Used: {}", numWorkers);
            logger.info("  Total Validation Errors: {}", metrics.validationErrors.get());
            logger.info("  Total Transformation Errors: {}", metrics.transformationErrors.get());
            logger.info("  Successfully Loaded: {}", metrics.successfulLoads.get());
        }

        /**
         * Worker thread that processes a batch of orders through all pipeline stages
         */
        class PipelineWorker implements Runnable {
            private final OrderBatch batch;

            public PipelineWorker(OrderBatch batch) {
                this.batch = batch;
            }

            @Override
            public void run() {
                try {
                    // Stage 1: Extract and Validate
                    extractAndValidate();
                    barrier.await();

                    // Stage 2: Transform and Enrich
                    transformAndEnrich();
                    barrier.await();

                    // Stage 3: Load to Destination
                    loadToDestination();
                    barrier.await();

                    // Stage 4: Generate Reports
                    generateReports();
                    barrier.await();

                    logger.info("Worker {} completed all stages for batch {}", batch.workerId, batch.workerId);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Worker {} interrupted", batch.workerId, e);
                } catch (BrokenBarrierException e) {
                    logger.error("Worker {} encountered broken barrier", batch.workerId, e);
                }
            }

            private void extractAndValidate() throws InterruptedException {
                logger.info("Worker {} [EXTRACT] Processing orders {} to {}", 
                    batch.workerId, batch.startId, batch.endId);
                
                // Simulate extraction and validation work
                Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
                
                int errors = ThreadLocalRandom.current().nextInt(0, 5);
                metrics.validationErrors.addAndGet(errors);
                batch.validRecords = batch.getRecordCount() - errors;
                
                logger.info("Worker {} [EXTRACT] Validated {} records ({} errors)", 
                    batch.workerId, batch.validRecords, errors);
            }

            private void transformAndEnrich() throws InterruptedException {
                logger.info("Worker {} [TRANSFORM] Enriching {} records", 
                    batch.workerId, batch.validRecords);
                
                // Simulate transformation work (e.g., currency conversion, address standardization)
                Thread.sleep(ThreadLocalRandom.current().nextInt(800, 2000));
                
                int errors = ThreadLocalRandom.current().nextInt(0, 3);
                metrics.transformationErrors.addAndGet(errors);
                batch.transformedRecords = batch.validRecords - errors;
                
                logger.info("Worker {} [TRANSFORM] Transformed {} records ({} errors)", 
                    batch.workerId, batch.transformedRecords, errors);
            }

            private void loadToDestination() throws InterruptedException {
                logger.info("Worker {} [LOAD] Writing {} records to database", 
                    batch.workerId, batch.transformedRecords);
                
                // Simulate database writes with potential retries
                Thread.sleep(ThreadLocalRandom.current().nextInt(600, 1800));
                
                metrics.successfulLoads.addAndGet(batch.transformedRecords);
                
                logger.info("Worker {} [LOAD] Successfully loaded {} records", 
                    batch.workerId, batch.transformedRecords);
            }

            private void generateReports() throws InterruptedException {
                logger.info("Worker {} [REPORT] Generating metrics for batch", batch.workerId);
                
                // Simulate report generation
                Thread.sleep(ThreadLocalRandom.current().nextInt(300, 800));
                
                double successRate = (batch.transformedRecords * 100.0) / batch.getRecordCount();
                logger.info("Worker {} [REPORT] Batch success rate: {:.2f}%", 
                    batch.workerId, successRate);
            }
        }
    }

    /**
     * Represents a batch of orders to be processed by a worker
     */
    static class OrderBatch {
        final int workerId;
        final int startId;
        final int endId;
        int validRecords;
        int transformedRecords;

        public OrderBatch(int workerId, int startId, int endId) {
            this.workerId = workerId;
            this.startId = startId;
            this.endId = endId;
        }

        public int getRecordCount() {
            return endId - startId + 1;
        }
    }

    /**
     * Tracks pipeline execution metrics
     */
    static class PipelineMetrics {
        final AtomicInteger validationErrors = new AtomicInteger(0);
        final AtomicInteger transformationErrors = new AtomicInteger(0);
        final AtomicInteger successfulLoads = new AtomicInteger(0);
    }
}
