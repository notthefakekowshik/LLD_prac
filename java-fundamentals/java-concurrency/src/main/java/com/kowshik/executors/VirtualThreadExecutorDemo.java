package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VirtualThreadExecutorDemo — Executors.newVirtualThreadPerTaskExecutor() (Java 21)
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What is a virtual thread?
 * A: A lightweight thread managed by the JVM, not the OS. Thousands of virtual
 *    threads multiplex onto a small pool of OS "carrier" threads (backed by
 *    ForkJoinPool). Stack starts at a few hundred bytes vs ~512KB for OS threads.
 *
 * Q: When should you use newVirtualThreadPerTaskExecutor?
 * A: I/O-bound workloads with high concurrency — HTTP calls, DB queries, file I/O.
 *    Each task gets its own virtual thread; when it blocks on I/O, the carrier
 *    thread is freed to run another virtual thread. No thread-pool tuning needed.
 *
 * Q: When should you NOT use it?
 * A: CPU-bound tasks — virtual threads don't add parallelism beyond N_CPUs.
 *    Tasks that use synchronized blocks — may pin the carrier thread.
 *
 * Q: What is carrier pinning?
 * A: A virtual thread that blocks inside a synchronized block cannot be unmounted
 *    from its carrier thread — the carrier is pinned and unavailable to others.
 *    Fix: replace synchronized with ReentrantLock.
 *
 * Demos:
 *   1. Massive concurrency — 10,000 tasks, far beyond any fixed pool
 *   2. Throughput comparison: virtual vs fixed pool for I/O-bound work
 *   3. Virtual thread properties — isDaemon, name, isVirtual
 *   4. Carrier pinning hazard with synchronized vs ReentrantLock
 */
public class VirtualThreadExecutorDemo {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadExecutorDemo.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        massiveConcurrencyDemo();
        throughputComparison();
        virtualThreadProperties();
        carrierPinningHazard();
    }

    // -------------------------------------------------------------------------
    // Demo 1: Massive concurrency — 10,000 tasks with blocking I/O simulation
    // A fixed pool of 200 would queue 9,800 tasks; virtual threads handle all at once
    // -------------------------------------------------------------------------
    static void massiveConcurrencyDemo() throws InterruptedException {
        log.info("=== Demo 1: 10,000 concurrent I/O tasks ===");
        int taskCount = 10_000;
        AtomicInteger completed = new AtomicInteger();
        long start = System.currentTimeMillis();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(taskCount);
            for (int i = 0; i < taskCount; i++) {
                futures.add(pool.submit(() -> {
                    Thread.sleep(100); // simulate I/O wait
                    completed.incrementAndGet();
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try { f.get(); } catch (ExecutionException e) { log.error("Task failed", e); }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[Done] {} tasks completed in {}ms", completed.get(), elapsed);
        log.info("[Note] Fixed pool of 200 would have taken ~{}ms (10000/200 * 100ms)",
                taskCount / 200 * 100);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 2: Throughput comparison — virtual threads vs fixed thread pool
    // for simulated I/O-bound work (sleep = network latency)
    // -------------------------------------------------------------------------
    static void throughputComparison() throws InterruptedException {
        int taskCount = 500;
        int ioDelayMs = 50;
        log.info("=== Demo 2: Throughput — {} tasks with {}ms I/O delay each ===",
                taskCount, ioDelayMs);

        // Fixed pool — 50 threads
        long fixedStart = System.currentTimeMillis();
        ExecutorService fixed = Executors.newFixedThreadPool(50);
        List<Future<?>> fixedFutures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            fixedFutures.add(fixed.submit(() -> { Thread.sleep(ioDelayMs); return null; }));
        }
        for (Future<?> f : fixedFutures) {
            try { f.get(); } catch (ExecutionException e) { log.error("error", e); }
        }
        fixed.shutdown();
        long fixedTime = System.currentTimeMillis() - fixedStart;

        // Virtual threads
        long vtStart = System.currentTimeMillis();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> vtFutures = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                vtFutures.add(vt.submit(() -> { Thread.sleep(ioDelayMs); return null; }));
            }
            for (Future<?> f : vtFutures) {
                try { f.get(); } catch (ExecutionException e) { log.error("error", e); }
            }
        }
        long vtTime = System.currentTimeMillis() - vtStart;

        log.info("[Fixed pool 50 threads] {}ms", fixedTime);
        log.info("[Virtual threads]       {}ms", vtTime);
        log.info("[Virtual threads win for I/O-bound: all {} tasks run concurrently]", taskCount);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 3: Virtual thread properties
    // -------------------------------------------------------------------------
    static void virtualThreadProperties() throws InterruptedException, ExecutionException {
        log.info("=== Demo 3: Virtual thread properties ===");

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            pool.submit(() -> {
                Thread t = Thread.currentThread();
                log.info("[Virtual thread] name='{}', isVirtual={}, isDaemon={}",
                        t.getName(), t.isVirtual(), t.isDaemon());
                log.info("[Note] Virtual threads are always daemon threads");
                log.info("[Note] Each submit() creates a BRAND NEW virtual thread — no pooling");
            }).get();
        }

        // Compare with platform thread
        Thread platform = new Thread(() -> {
            Thread t = Thread.currentThread();
            log.info("[Platform thread] name='{}', isVirtual={}, isDaemon={}",
                    t.getName(), t.isVirtual(), t.isDaemon());
        }, "platform-thread");
        platform.start();
        platform.join();
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 4: Carrier pinning hazard
    // synchronized block pins the carrier; ReentrantLock does not
    // -------------------------------------------------------------------------
    static void carrierPinningHazard() throws InterruptedException {
        log.info("=== Demo 4: Carrier pinning — synchronized vs ReentrantLock ===");
        int cpus = Runtime.getRuntime().availableProcessors();
        log.info("[Info] {} carrier threads available (= availableProcessors)", cpus);

        // BAD: synchronized pins the carrier thread during sleep
        // With N carrier threads and N+1 virtual threads all sleeping in synchronized,
        // N carriers are pinned → the (N+1)th virtual thread cannot run
        Object monitor = new Object();
        AtomicInteger pinnedCompleted = new AtomicInteger();
        long pinnedStart = System.currentTimeMillis();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < cpus + 2; i++) {
                futures.add(pool.submit(() -> {
                    synchronized (monitor) {
                        Thread.sleep(200); // PINS carrier — can't unmount during sleep in synchronized
                    }
                    pinnedCompleted.incrementAndGet();
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try { f.get(); } catch (ExecutionException e) { log.warn("Task error", e); }
            }
        }
        long pinnedTime = System.currentTimeMillis() - pinnedStart;

        // GOOD: ReentrantLock allows virtual thread to unmount during await/sleep
        java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();
        AtomicInteger lockCompleted = new AtomicInteger();
        long lockStart = System.currentTimeMillis();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < cpus + 2; i++) {
                futures.add(pool.submit(() -> {
                    lock.lock();
                    try {
                        Thread.sleep(200); // virtual thread can unmount — carrier freed
                    } finally {
                        lock.unlock();
                    }
                    lockCompleted.incrementAndGet();
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try { f.get(); } catch (ExecutionException e) { log.warn("Task error", e); }
            }
        }
        long lockTime = System.currentTimeMillis() - lockStart;

        log.info("[synchronized — carrier pinned] {}ms to complete {} tasks", pinnedTime, pinnedCompleted.get());
        log.info("[ReentrantLock — no pinning]    {}ms to complete {} tasks", lockTime, lockCompleted.get());
        log.info("[Rule] Never use synchronized blocks in virtual thread tasks — use ReentrantLock");
        log.info("");
    }
}
