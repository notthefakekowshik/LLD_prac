package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * SingleThreadExecutorDemo — newSingleThreadExecutor()
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What guarantees does newSingleThreadExecutor give?
 * A: Tasks are executed sequentially, one at a time, in submission order.
 *    If the worker thread dies, a replacement is created automatically.
 *
 * Q: How is it different from newFixedThreadPool(1)?
 * A: newSingleThreadExecutor wraps the pool in a proxy that prevents casting
 *    to ThreadPoolExecutor. You cannot accidentally call setCorePoolSize(4)
 *    and break the single-thread guarantee. newFixedThreadPool(1) allows that.
 *
 * Q: What is a good use case?
 * A: Serializing access to a non-thread-safe resource (DB connection, log file,
 *    in-memory state machine) without any synchronization in the task code.
 *
 * Demos:
 *   1. Sequential ordering guarantee
 *   2. Thread resurrection after death
 *   3. Proxy protection — cannot be cast to ThreadPoolExecutor
 *   4. Real-world: serialized log writer
 */
public class SingleThreadExecutorDemo {

    private static final Logger log = LoggerFactory.getLogger(SingleThreadExecutorDemo.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        sequentialOrderDemo();
        threadResurrectionDemo();
        proxyProtectionDemo();
        serializedLogWriterDemo();
    }

    // -------------------------------------------------------------------------
    // Demo 1: Sequential ordering — tasks always run in submission order
    // -------------------------------------------------------------------------
    static void sequentialOrderDemo() throws InterruptedException, ExecutionException {
        log.info("=== Demo 1: Sequential ordering guarantee ===");
        ExecutorService pool = Executors.newSingleThreadExecutor();

        Future<?>[] futures = new Future[5];
        for (int i = 1; i <= 5; i++) {
            final int id = i;
            futures[i - 1] = pool.submit(() -> {
                log.info("[Task-{}] running on thread={}", id, Thread.currentThread().getName());
                Thread.sleep(100);
                return id;
            });
        }

        // Tasks always complete in order 1 → 2 → 3 → 4 → 5
        for (Future<?> f : futures) f.get();
        log.info("[Done] Tasks always ran in submission order — no parallelism.");

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 2: Thread resurrection — if worker thread dies, pool creates a new one
    // -------------------------------------------------------------------------
    static void threadResurrectionDemo() throws InterruptedException {
        log.info("=== Demo 2: Thread resurrection after worker death ===");
        ExecutorService pool = Executors.newSingleThreadExecutor();

        // Task 1: runs fine
        pool.submit(() -> log.info("[Task-1] thread={}", Thread.currentThread().getName()));

        // Task 2: throws — kills the worker thread
        pool.submit(() -> {
            log.info("[Task-2] about to throw RuntimeException on thread={}",
                    Thread.currentThread().getName());
            throw new RuntimeException("Simulated crash");
        });

        Thread.sleep(200); // let the crash happen

        // Task 3: pool auto-creates a new worker thread and continues
        pool.submit(() -> log.info("[Task-3] thread={} (new thread after crash)",
                Thread.currentThread().getName()));

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("[Done] Pool recovered automatically from worker thread death.");
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 3: Proxy protection — newSingleThreadExecutor cannot be cast
    // -------------------------------------------------------------------------
    static void proxyProtectionDemo() {
        log.info("=== Demo 3: Proxy protection — cannot resize via cast ===");

        ExecutorService single = Executors.newSingleThreadExecutor();
        ExecutorService fixed1 = Executors.newFixedThreadPool(1);

        // newFixedThreadPool(1) — CAN be cast and reconfigured (dangerous!)
        try {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) fixed1;
            tpe.setCorePoolSize(4);  // silently breaks the "1 thread" guarantee
            log.warn("[Fixed(1)] Cast succeeded — pool was resized to {} threads (dangerous!)",
                    tpe.getCorePoolSize());
        } catch (ClassCastException e) {
            log.info("[Fixed(1)] Cast blocked: {}", e.getMessage());
        }

        // newSingleThreadExecutor() — proxy BLOCKS the cast
        try {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) single;
            tpe.setCorePoolSize(4);
            log.warn("[SingleThread] Cast succeeded — this SHOULD NOT happen");
        } catch (ClassCastException e) {
            log.info("[SingleThread] Cast blocked by proxy — single-thread guarantee enforced. ✓");
        }

        single.shutdown();
        fixed1.shutdown();
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 4: Real-world — serialized log writer (no synchronization needed)
    // -------------------------------------------------------------------------
    static void serializedLogWriterDemo() throws InterruptedException {
        log.info("=== Demo 4: Serialized log writer — thread-safe via single thread ===");

        // Non-thread-safe log buffer — safe because only ONE thread ever touches it
        StringBuilder logBuffer = new StringBuilder();
        ExecutorService logWriter = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "log-writer"));

        // Multiple caller threads submit log entries concurrently
        Thread[] callers = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int id = i + 1;
            callers[i] = new Thread(() -> {
                for (int j = 1; j <= 3; j++) {
                    final String entry = "[Caller-" + id + "] Log entry #" + j;
                    logWriter.submit(() -> {
                        logBuffer.append(entry).append("\n"); // safe: only log-writer thread runs this
                    });
                }
            }, "caller-" + id);
        }

        for (Thread t : callers) t.start();
        for (Thread t : callers) t.join();

        // Wait for all log tasks to flush
        logWriter.shutdown();
        logWriter.awaitTermination(5, TimeUnit.SECONDS);

        log.info("[Log buffer contents — {} entries written without any synchronization]",
                logBuffer.toString().split("\n").length);
        log.info("");
    }
}
