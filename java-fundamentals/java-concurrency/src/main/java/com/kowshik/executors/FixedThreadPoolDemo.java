package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FixedThreadPoolDemo — newFixedThreadPool(n)
 *
 * INTERVIEW PREP:
 * ==============
 * Q: When should you use a fixed thread pool?
 * A: When you know the max concurrency you want — CPU-bound tasks (N_CPU + 1),
 *    or when you want a hard cap on resource usage.
 *
 * Q: What is the danger of newFixedThreadPool in production?
 * A: It uses an UNBOUNDED LinkedBlockingQueue. Tasks pile up indefinitely
 *    if producers are faster than consumers → OOM.
 *
 * Q: Does maximumPoolSize matter in a fixed thread pool?
 * A: No. maximumPoolSize == corePoolSize, so the pool never grows beyond n.
 *    Extra tasks just queue up, never spawn more threads.
 *
 * Demos:
 *   1. Basic task submission and result collection via Future
 *   2. Queue backlog — submitting more tasks than threads
 *   3. Thread reuse proof — same threads handle multiple tasks
 *   4. Production-safe variant with bounded queue
 */
public class FixedThreadPoolDemo {

    private static final Logger log = LoggerFactory.getLogger(FixedThreadPoolDemo.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        basicSubmission();
        queueBacklog();
        threadReuseProof();
        productionSafeVariant();
    }

    // -------------------------------------------------------------------------
    // Demo 1: Basic task submission — submit Callable, collect Future results
    // -------------------------------------------------------------------------
    static void basicSubmission() throws InterruptedException, ExecutionException {
        log.info("=== Demo 1: Basic fixed thread pool — 3 threads, 5 tasks ===");
        ExecutorService pool = Executors.newFixedThreadPool(3);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            futures.add(pool.submit(() -> {
                log.info("[Task-{}] running on {}", taskId, Thread.currentThread().getName());
                Thread.sleep(200);
                return taskId * taskId;
            }));
        }

        for (int i = 0; i < futures.size(); i++) {
            log.info("[Result] Task-{} = {}", i + 1, futures.get(i).get());
        }

        shutdownAndAwait(pool);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 2: Queue backlog — 3 threads but 10 tasks
    // Tasks 4-10 queue up, never spawn a 4th thread
    // -------------------------------------------------------------------------
    static void queueBacklog() throws InterruptedException {
        log.info("=== Demo 2: Queue backlog — 3 threads, 10 tasks ===");
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 10; i++) {
            final int id = i;
            pool.submit(() -> {
                log.info("[Task-{}] started — active={}, queued={}",
                        id, pool.getActiveCount(), pool.getQueue().size());
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        Thread.sleep(100); // let tasks start
        log.info("[Monitor] Pool size={}, Active={}, Queue size={}",
                pool.getPoolSize(), pool.getActiveCount(), pool.getQueue().size());

        shutdownAndAwait(pool);
        log.info("[Done] Completed tasks: {}", pool.getCompletedTaskCount());
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 3: Thread reuse — same 3 threads handle 9 tasks (3 rounds of 3)
    // -------------------------------------------------------------------------
    static void threadReuseProof() throws InterruptedException {
        log.info("=== Demo 3: Thread reuse proof ===");
        ExecutorService pool = Executors.newFixedThreadPool(3);
        AtomicInteger round = new AtomicInteger(0);

        for (int batch = 0; batch < 3; batch++) {
            int r = round.incrementAndGet();
            List<Future<?>> batch_futures = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                final int taskId = i + 1;
                batch_futures.add(pool.submit(() -> {
                    log.info("[Round-{} Task-{}] thread={}", r, taskId, Thread.currentThread().getName());
                    Thread.sleep(100);
                    return null;
                }));
            }
            for (Future<?> f : batch_futures) {
                try { f.get(); } catch (ExecutionException e) { log.error("Task failed", e); }
            }
        }
        // Same 3 thread names appear across all 3 rounds — threads are reused
        shutdownAndAwait(pool);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 4: Production-safe variant — bounded queue, custom rejection handler
    // -------------------------------------------------------------------------
    static void productionSafeVariant() throws InterruptedException {
        log.info("=== Demo 4: Production-safe — bounded queue + rejection handler ===");

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                3, 3,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(5),          // bounded: max 5 queued tasks
                r -> new Thread(r, "fixed-worker"),
                (task, executor) ->                   // custom rejection
                        log.warn("[REJECTED] Queue full — task dropped. Active={}, Queue={}",
                                executor.getActiveCount(), executor.getQueue().size())
        );

        for (int i = 1; i <= 12; i++) {
            final int id = i;
            pool.execute(() -> {
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                log.info("[Task-{}] completed", id);
            });
        }
        // Tasks 9-12 will be rejected (3 threads running + 5 queued = 8 capacity)
        shutdownAndAwait(pool);
        log.info("");
    }

    static void shutdownAndAwait(ExecutorService pool) throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
    }
}
