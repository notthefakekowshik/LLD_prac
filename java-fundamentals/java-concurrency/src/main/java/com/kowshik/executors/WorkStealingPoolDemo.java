package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WorkStealingPoolDemo — newWorkStealingPool()
 *
 * INTERVIEW PREP:
 * ==============
 * Q: How does work-stealing work?
 * A: Each worker thread has its own double-ended queue (deque). It takes tasks
 *    from its own deque's HEAD. When idle, it steals tasks from the TAIL of
 *    another thread's deque. This minimises contention vs a shared queue.
 *
 * Q: Why are the threads daemon threads?
 * A: Work-stealing pools are backed by ForkJoinPool, designed for background
 *    computation. The JVM exits without waiting for them — no explicit shutdown needed.
 *
 * Q: When NOT to use work-stealing pool?
 * A: Tasks needing ordered execution, tasks holding locks (stealing can cause
 *    priority inversion), or tasks that must not run concurrently.
 *
 * Demos:
 *   1. Parallelism vs FixedThreadPool throughput comparison
 *   2. Divide-and-conquer with recursive subtasks
 *   3. Daemon thread behaviour — JVM exits without shutdown()
 *   4. Work stealing in action — uneven task distribution
 */
public class WorkStealingPoolDemo {

    private static final Logger log = LoggerFactory.getLogger(WorkStealingPoolDemo.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        throughputComparison();
        divideAndConquer();
        daemonBehaviourNote();
        unevenDistributionDemo();
    }

    // -------------------------------------------------------------------------
    // Demo 1: Throughput — work-stealing vs fixed pool on CPU-bound tasks
    // -------------------------------------------------------------------------
    static void throughputComparison() throws InterruptedException {
        int cpus = Runtime.getRuntime().availableProcessors();
        int taskCount = cpus * 4;
        log.info("=== Demo 1: Throughput — {} tasks on {} CPUs ===", taskCount, cpus);

        // Fixed thread pool
        long fixedStart = System.currentTimeMillis();
        ExecutorService fixed = Executors.newFixedThreadPool(cpus);
        List<Future<Long>> fixedFutures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            fixedFutures.add(fixed.submit(WorkStealingPoolDemo::cpuWork));
        }
        for (Future<Long> f : fixedFutures) {
            try { f.get(); } catch (ExecutionException e) { log.error("Task failed", e); }
        }
        fixed.shutdown();
        fixed.awaitTermination(30, TimeUnit.SECONDS);
        long fixedTime = System.currentTimeMillis() - fixedStart;

        // Work-stealing pool
        long wsStart = System.currentTimeMillis();
        ExecutorService ws = Executors.newWorkStealingPool(cpus);
        List<Future<Long>> wsFutures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            wsFutures.add(ws.submit(WorkStealingPoolDemo::cpuWork));
        }
        for (Future<Long> f : wsFutures) {
            try { f.get(); } catch (ExecutionException e) { log.error("Task failed", e); }
        }
        ws.shutdown();
        ws.awaitTermination(30, TimeUnit.SECONDS);
        long wsTime = System.currentTimeMillis() - wsStart;

        log.info("[Fixed pool]        {}ms", fixedTime);
        log.info("[Work-stealing pool] {}ms", wsTime);
        log.info("[Work-stealing advantage: less lock contention on shared queue]\n");
    }

    static long cpuWork() {
        long sum = 0;
        for (long i = 0; i < 5_000_000L; i++) sum += i;
        return sum;
    }

    // -------------------------------------------------------------------------
    // Demo 2: Divide-and-conquer — parallel sum using recursive subtasks
    // -------------------------------------------------------------------------
    static void divideAndConquer() throws InterruptedException, ExecutionException {
        log.info("=== Demo 2: Divide-and-conquer — parallel array sum ===");
        int[] data = new int[1_000_000];
        for (int i = 0; i < data.length; i++) data[i] = i + 1;

        ForkJoinPool pool = (ForkJoinPool) Executors.newWorkStealingPool();
        long result = pool.submit(() -> parallelSum(data, 0, data.length)).get();

        long expected = (long) data.length * (data.length + 1) / 2;
        log.info("[Parallel sum] result={}, expected={}, match={}", result, expected, result == expected);
        pool.shutdown();
        log.info("");
    }

    static long parallelSum(int[] data, int from, int to) throws InterruptedException, ExecutionException {
        if (to - from <= 50_000) {
            long sum = 0;
            for (int i = from; i < to; i++) sum += data[i];
            return sum;
        }
        int mid = (from + to) / 2;
        ForkJoinPool pool = ForkJoinTask.getPool() instanceof ForkJoinPool
                ? (ForkJoinPool) ForkJoinTask.getPool()
                : ForkJoinPool.commonPool();

        Future<Long> left = pool.submit(() -> parallelSum(data, from, mid));
        long right = parallelSum(data, mid, to);
        return left.get() + right;
    }

    // -------------------------------------------------------------------------
    // Demo 3: Daemon thread note — work-stealing threads don't block JVM exit
    // -------------------------------------------------------------------------
    static void daemonBehaviourNote() {
        log.info("=== Demo 3: Daemon threads — work-stealing pool won't block JVM exit ===");
        ExecutorService ws = Executors.newWorkStealingPool();

        ws.submit(() -> {
            Thread t = Thread.currentThread();
            log.info("[Worker] name={}, isDaemon={}", t.getName(), t.isDaemon());
        });

        // NOTE: No shutdown() call needed — JVM will exit when main thread finishes
        // For comparison: newFixedThreadPool threads are NOT daemon — they keep JVM alive
        log.info("[Note] Work-stealing pool threads are daemon=true");
        log.info("[Note] newFixedThreadPool threads are daemon=false — JVM waits for them\n");
    }

    // -------------------------------------------------------------------------
    // Demo 4: Uneven task distribution — idle workers steal from busy ones
    // -------------------------------------------------------------------------
    static void unevenDistributionDemo() throws InterruptedException {
        log.info("=== Demo 4: Uneven distribution — stealing from busy threads ===");
        int cpus = Runtime.getRuntime().availableProcessors();
        ExecutorService ws = Executors.newWorkStealingPool(cpus);
        AtomicLong totalWork = new AtomicLong();

        // Submit tasks with wildly different sizes — work stealing balances the load
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < cpus * 2; i++) {
            final long iterations = (i % 2 == 0) ? 10_000_000L : 100_000L; // alternating heavy/light
            futures.add(ws.submit(() -> {
                long sum = 0;
                for (long j = 0; j < iterations; j++) sum += j;
                totalWork.addAndGet(sum);
                log.info("[Worker] {} iterations done on thread={}",
                        iterations, Thread.currentThread().getName());
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException e) { log.error("Task failed", e); }
        }

        log.info("[Done] Total work units: {}", totalWork.get());
        log.info("[Work-stealing balanced light and heavy tasks across all threads]\n");
        ws.shutdown();
    }
}
