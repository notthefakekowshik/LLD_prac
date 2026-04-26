package com.kowshik.advanced;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates how virtual threads remove the OS-thread bottleneck but NOT
 * external-resource bottlenecks (e.g. a 30-connection DB pool).
 *
 * Scenario: 1,000,000 concurrent virtual threads all want a DB connection.
 *           DB pool size = 30.
 *           Result: 999,970 threads block inside getConnection() →
 *           thundering-herd, timeouts, retry storms, DB death.
 *
 * Fixes shown:
 *   1. Semaphore (match concurrency to DB capacity)
 *   2. Caching (avoid DB hit entirely)
 *   3. Batching (amortise connection cost)
 *   4. Backpressure / rate limiting (reject instead of overwhelm)
 */
public class VirtualThreadsDbBottleneckDemo {

    // Simulated DB pool with only 30 connections
    private static final int DB_POOL_SIZE = 30;
    private static final Semaphore DB_PERMITS = new Semaphore(DB_POOL_SIZE);

    // Simulated cache
    private static final ConcurrentHashMap<Integer, String> CACHE = new ConcurrentHashMap<>();

    // Metrics
    private static final AtomicInteger dbCalls = new AtomicInteger(0);
    private static final AtomicInteger cacheHits = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Virtual Threads DB Bottleneck Demo ===");
        System.out.println("DB pool size: " + DB_POOL_SIZE);

        demoUnboundedConcurrency();          // BAD
        demoSemaphoreGuardedConcurrency();     // GOOD
        demoWithCaching();                   // BETTER
        demoWithBatching();                  // BEST for bulk workloads
        demoWithBackpressure();              // Defensive

        System.out.println("\nDone.");
    }

    // ------------------------------------------------------------------
    // 1. THE PROBLEM: 1_000 tasks, 30 DB connections → 970 threads block
    // ------------------------------------------------------------------
    static void demoUnboundedConcurrency() throws Exception {
        System.out.println("\n--- 1. Unbounded (BROKEN) ---");
        dbCalls.set(0);

        long start = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1_000; i++) {
                final int id = i;
                executor.submit(() -> {
                    // All 1,000 virtual threads race here
                    String result = queryDbBlocking(id); // 970 of them block inside DB pool
                    if (id % 200 == 0) System.out.println("Task " + id + " => " + result);
                });
            }
        } // waits for all 1,000

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Total DB calls: " + dbCalls.get() + " | Time: " + elapsed + "ms");
    }

    // ------------------------------------------------------------------
    // 2. FIX: Semaphore limits *in-flight* DB work to pool capacity
    // ------------------------------------------------------------------
    static void demoSemaphoreGuardedConcurrency() throws Exception {
        System.out.println("\n--- 2. Semaphore Guarded (GOOD) ---");
        dbCalls.set(0);

        long start = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1_000; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        DB_PERMITS.acquire();          // Only 30 pass through at once
                        String result = queryDbBlocking(id);
                        if (id % 200 == 0) System.out.println("Task " + id + " => " + result);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        DB_PERMITS.release();
                    }
                });
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Total DB calls: " + dbCalls.get() + " | Time: " + elapsed + "ms");
    }

    // ------------------------------------------------------------------
    // 3. FIX: Caching — avoids the DB entirely for hot keys
    // ------------------------------------------------------------------
    static void demoWithCaching() throws Exception {
        System.out.println("\n--- 3. With Caching (BETTER) ---");
        dbCalls.set(0);
        cacheHits.set(0);
        CACHE.clear();

        long start = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // 1,000 tasks but only 100 unique IDs → 90% cacheable
            for (int i = 0; i < 1_000; i++) {
                final int id = i % 100;
                int finalI = i;
                executor.submit(() -> {
                    String result = getWithCache(id);
                    if (finalI % 200 == 0) System.out.println("Task " + id + " => " + result);
                });
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("DB calls: " + dbCalls.get() +
                           " | Cache hits: " + cacheHits.get() +
                           " | Time: " + elapsed + "ms");
    }

    static String getWithCache(int id) {
        String cached = CACHE.get(id);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        // Only one thread per key pays the DB cost; rest see cached value
        return CACHE.computeIfAbsent(id, k -> {
            dbCalls.incrementAndGet();
            return queryDbBlocking(k);
        });
    }

    // ------------------------------------------------------------------
    // 4. FIX: Batching — amortise connection + round-trip cost
    // ------------------------------------------------------------------
    static void demoWithBatching() throws Exception {
        System.out.println("\n--- 4. With Batching (BEST for bulk) ---");
        dbCalls.set(0);

        long start = System.currentTimeMillis();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Instead of 1,000 single-row queries, batch in groups of 50
            int batchSize = 50;
            int total = 1_000;
            for (int i = 0; i < total; i += batchSize) {
                final int from = i;
                final int to = Math.min(i + batchSize, total);
                executor.submit(() -> queryDbBatch(from, to));
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        // 1,000 / 50 = 20 batch queries
        System.out.println("DB calls: " + dbCalls.get() + " | Time: " + elapsed + "ms");
    }

    // ------------------------------------------------------------------
    // 5. FIX: Backpressure — reject overload instead of drowning DB
    // ------------------------------------------------------------------
    static void demoWithBackpressure() throws Exception {
        System.out.println("\n--- 5. Backpressure (DEFENSIVE) ---");
        dbCalls.set(0);

        // Bounded queue + CallerRunsPolicy = apply backpressure naturally
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                30, 30,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(60),
                Thread.ofVirtual().factory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            final int id = i;
            executor.submit(() -> {
                // Still protected by semaphore inside task
                try {
                    DB_PERMITS.acquire();
                    String result = queryDbBlocking(id);
                    if (id % 50 == 0) System.out.println("Task " + id + " => " + result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    DB_PERMITS.release();
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("DB calls: " + dbCalls.get() + " | Time: " + elapsed + "ms");
    }

    // =================== Simulated DB ===================

    static String queryDbBlocking(int id) {
        dbCalls.incrementAndGet();
        try {
            Thread.sleep(5); // Simulate 5ms query latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Row-" + id;
    }

    static void queryDbBatch(int from, int to) {
        dbCalls.incrementAndGet();
        try {
            Thread.sleep(10); // 10ms for 50 rows → amortised 0.2ms/row
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
