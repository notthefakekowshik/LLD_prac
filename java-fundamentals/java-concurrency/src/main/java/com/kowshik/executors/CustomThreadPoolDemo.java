package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Thread Pool Demo - Advanced ThreadPoolExecutor Configuration
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. ThreadPoolExecutor Parameters:
 * - corePoolSize: Minimum number of threads to keep alive
 * - maximumPoolSize: Maximum number of threads allowed
 * - keepAliveTime: Time idle threads wait before termination
 * - workQueue: Queue to hold tasks before execution
 * - threadFactory: Factory for creating new threads
 * - rejectedExecutionHandler: Handler for rejected tasks
 *
 * 2. Common Interview Questions:
 * - Explain ThreadPoolExecutor parameters
 * - When does pool create new threads beyond core size?
 * - Different types of blocking queues?
 * - Rejection policies and when each is used?
 * - How to monitor thread pool health?
 * - What is thread pool saturation?
 *
 * 3. Rejection Policies:
 * - AbortPolicy: Throws RejectedExecutionException (default)
 * - CallerRunsPolicy: Runs task in caller thread (backpressure)
 * - DiscardPolicy: Silently discards task
 * - DiscardOldestPolicy: Discards oldest task in queue
 *
 * 4. Monitoring Metrics:
 * - Active thread count
 * - Queue size
 * - Completed task count
 * - Pool size
 *
 * 5. Best Practices:
 * - Choose appropriate queue type and size
 * - Monitor pool statistics
 * - Set meaningful thread names
 * - Handle rejected tasks appropriately
 * - Shutdown gracefully
 *
 * Demonstrates implementation of a custom thread pool with:
 * - Custom thread factory with naming
 * - Bounded blocking queue
 * - Custom rejection policy
 * - Thread pool monitoring
 * - Graceful shutdown
 */
public class CustomThreadPoolDemo {
    private static final Logger logger = LoggerFactory.getLogger(CustomThreadPoolDemo.class);

    static class MonitoredThreadPool extends ThreadPoolExecutor {
        private final AtomicInteger totalTasksSubmitted = new AtomicInteger(0);
        private final AtomicInteger totalTasksCompleted = new AtomicInteger(0);
        private final ConcurrentHashMap<String, Long> taskExecutionTimes = new ConcurrentHashMap<>();

        public MonitoredThreadPool(int corePoolSize,
                int maximumPoolSize,
                long keepAliveTime,
                TimeUnit unit,
                BlockingQueue<Runnable> workQueue,
                ThreadFactory threadFactory,
                RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            taskExecutionTimes.put(r.toString(), System.nanoTime());
            totalTasksSubmitted.incrementAndGet();
            logger.info("Task starting: {} on thread: {}", r, t.getName());
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            try {
                long startTime = taskExecutionTimes.remove(r.toString());
                long duration = System.nanoTime() - startTime;
                totalTasksCompleted.incrementAndGet();
                logger.info("Task completed: {}, Duration: {} ms", r, TimeUnit.NANOSECONDS.toMillis(duration));

                if (t != null) {
                    logger.error("Task failed with exception: {}", t.getMessage());
                }
            } finally {
                super.afterExecute(r, t);
            }
        }

        /**
         * Prints current thread pool statistics.
         */
        public void printStatistics() {
            logger.info("Pool Statistics - Submitted: {}, Completed: {}, Active: {}, Queue Size: {}",
                    totalTasksSubmitted.get(),
                    totalTasksCompleted.get(),
                    getActiveCount(),
                    getQueue().size());
        }
    }

    static class CustomThreadFactory implements ThreadFactory {
        private final String poolName;
        private final AtomicInteger threadCount = new AtomicInteger(1);

        public CustomThreadFactory(String poolName) {
            this.poolName = poolName;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, poolName + "-worker-" + threadCount.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    /**
     * Custom rejection handler with logging and fallback strategy.
     */
    static class CustomRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.warn("Task rejected: {}. Queue size: {}, Active threads: {}",
                    r, executor.getQueue().size(), executor.getActiveCount());

            // Attempt to run the task in the caller's thread as a fallback
            if (!executor.isShutdown()) {
                try {
                    r.run();
                } catch (Exception e) {
                    logger.error("Error executing rejected task: {}", e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int corePoolSize = 2;
        int maxPoolSize = 4;
        long keepAliveTime = 60L;
        int queueCapacity = 100;

        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(queueCapacity);
        CustomThreadFactory threadFactory = new CustomThreadFactory("CustomPool");
        CustomRejectionHandler rejectionHandler = new CustomRejectionHandler();

        MonitoredThreadPool executor = new MonitoredThreadPool(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                rejectionHandler);

        // Submit some example tasks
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Simulate varying task durations
                    TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(100, 1000));
                    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                        throw new RuntimeException("Simulated failure in task " + taskId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Monitor the pool
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.scheduleAtFixedRate(executor::printStatistics, 1, 1, TimeUnit.SECONDS);

        // Wait for tasks to complete
        TimeUnit.SECONDS.sleep(5);

        // Shutdown the pool gracefully
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        monitor.shutdownNow();
    }
}
