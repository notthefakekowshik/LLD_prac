package com.kowshik.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ThreadLocalLeakDemo — ThreadLocal Memory Leak and Data Bleeding in Thread Pools
 *
 * INTERVIEW PREP:
 * ==============
 * Q: How can ThreadLocal cause a memory leak in a thread pool?
 * A: Thread pool threads are reused — they never die between requests. If a thread
 *    sets a ThreadLocal value and never calls remove(), that value stays in the
 *    thread's ThreadLocalMap forever. Over time:
 *      (1) MEMORY LEAK: The value object is reachable via the thread's ThreadLocalMap,
 *          so GC cannot collect it, even though no application code holds a reference.
 *      (2) DATA BLEEDING: The next request reusing that thread sees the stale value
 *          from a previous request — a serious correctness and security bug.
 *
 * Q: Why doesn't the WeakReference on the ThreadLocal key prevent the leak?
 * A: ThreadLocalMap entries use a WeakReference to the ThreadLocal KEY, not the VALUE.
 *    If the ThreadLocal object itself becomes unreachable (e.g., it's a local variable
 *    that goes out of scope), the key becomes null and the entry becomes "stale".
 *    BUT: the VALUE is still a strong reference and is NOT collected until the stale
 *    entry is cleaned up — which only happens lazily during the next get()/set()/remove()
 *    on that thread. In a long-lived thread pool thread, this cleanup may never happen.
 *
 * Q: What is the fix?
 * A: Always call threadLocal.remove() in a finally block at the end of the task,
 *    before the thread is returned to the pool. This clears both the key and value.
 *
 * Three scenarios in this demo:
 *   1. DATA BLEEDING — no remove(); next request sees previous request's data.
 *   2. MEMORY GROWTH — large objects stored in ThreadLocal without cleanup.
 *   3. CORRECT PATTERN — remove() in finally; each request starts clean.
 */
public class ThreadLocalLeakDemo {

    private static final Logger log = LoggerFactory.getLogger(ThreadLocalLeakDemo.class);

    // Simulates a per-request security context (user identity, roles, etc.)
    private static final ThreadLocal<RequestContext> REQUEST_CONTEXT = new ThreadLocal<>();

    // Simulates a large object stored per-thread (e.g., a buffer, session, connection)
    private static final ThreadLocal<byte[]> LARGE_BUFFER = new ThreadLocal<>();

    static class RequestContext {
        final String userId;
        final String role;

        RequestContext(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        public String toString() {
            return "RequestContext{userId='" + userId + "', role='" + role + "'}";
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 1: DATA BLEEDING — no remove()
    //
    // Request A sets context. Request B (same thread) never sets it —
    // reads A's stale context. Security bug: B operates as user A.
    // -------------------------------------------------------------------------
    static void dataBleeding() throws InterruptedException {
        log.info("=== Scenario 1: DATA BLEEDING (no remove) ===");

        // Single-thread pool: both tasks run on the SAME thread
        ExecutorService pool = Executors.newFixedThreadPool(1);

        // Task 1 (Request A): sets context, does NOT remove
        pool.submit(() -> {
            REQUEST_CONTEXT.set(new RequestContext("alice", "ADMIN"));
            log.info("[Request-A] Set context: {}", REQUEST_CONTEXT.get());
            log.info("[Request-A] Processing as {}", REQUEST_CONTEXT.get().userId);
            // BUG: no REQUEST_CONTEXT.remove() — value stays in thread's map
        });

        // Give Task 1 time to complete so Task 2 reuses the same thread
        Thread.sleep(200);

        // Task 2 (Request B): never sets context — reads A's stale value
        pool.submit(() -> {
            RequestContext ctx = REQUEST_CONTEXT.get(); // should be null — but it isn't!
            if (ctx != null) {
                log.warn("[Request-B] *** DATA BLEED *** sees context from a PREVIOUS request: {}", ctx);
                log.warn("[Request-B] Request-B is now operating as user '{}' with role '{}'!",
                        ctx.userId, ctx.role);
            } else {
                log.info("[Request-B] Context is null (correct)");
            }
        });

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Scenario 2: MEMORY GROWTH — large object never collected
    //
    // Each task allocates a 1 MB buffer into ThreadLocal and never removes it.
    // After 5 tasks on the same thread, 5 MB is stuck in the thread's map.
    // GC cannot collect it because the thread is still alive (in the pool).
    // -------------------------------------------------------------------------
    static void memoryGrowth() throws InterruptedException {
        log.info("=== Scenario 2: MEMORY GROWTH (large object not removed) ===");

        ExecutorService pool = Executors.newFixedThreadPool(1);

        for (int i = 1; i <= 5; i++) {
            final int requestId = i;
            pool.submit(() -> {
                // Each request stores a 1 MB buffer — old one is replaced but never GC'd
                // until the thread dies (which never happens in a pool)
                LARGE_BUFFER.set(new byte[1024 * 1024]); // 1 MB
                log.info("[Request-{}] Stored 1MB buffer in ThreadLocal. No remove().", requestId);
                // BUG: LARGE_BUFFER.remove() never called
                // The previous buffer is overwritten in the map, but the JVM may not
                // immediately reclaim it. Worse: if you use a unique ThreadLocal per request,
                // every old value stays reachable via the thread's map until the entry is
                // lazily expunged.
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("After 5 requests: 5 x 1MB buffers may still be referenced by the pool thread.");
        log.info("In production, a pool thread lives for the app lifetime — memory never freed.\n");
    }

    // -------------------------------------------------------------------------
    // Scenario 3: CORRECT PATTERN — remove() in finally
    //
    // Every request sets context and removes it in a finally block.
    // The next request on the same thread always starts with a clean slate.
    // -------------------------------------------------------------------------
    static void correctPattern() throws InterruptedException {
        log.info("=== Scenario 3: CORRECT PATTERN (remove in finally) ===");

        ExecutorService pool = Executors.newFixedThreadPool(1);

        // Task 1: sets context, removes in finally
        pool.submit(() -> {
            REQUEST_CONTEXT.set(new RequestContext("alice", "ADMIN"));
            try {
                log.info("[Request-A] Set context: {}", REQUEST_CONTEXT.get());
                log.info("[Request-A] Processing as {}", REQUEST_CONTEXT.get().userId);
            } finally {
                REQUEST_CONTEXT.remove(); // always runs — even if an exception is thrown
                log.info("[Request-A] Context removed from ThreadLocal.");
            }
        });

        Thread.sleep(200);

        // Task 2: reads context — should be null (clean thread)
        pool.submit(() -> {
            RequestContext ctx = REQUEST_CONTEXT.get();
            if (ctx == null) {
                log.info("[Request-B] Context is null — thread is clean. ✓");
            } else {
                log.warn("[Request-B] *** BUG *** still sees: {}", ctx);
            }
        });

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Scenario 4: UNIQUE ThreadLocal PER REQUEST (catastrophic leak pattern)
    //
    // Some codebases create a new ThreadLocal instance per request (inside a method).
    // The ThreadLocal instance becomes unreachable (weak key → stale), BUT the value
    // is still strongly referenced by the thread's map until the stale entry is
    // lazily cleaned up — which requires another get/set/remove on that thread.
    // In a large pool under low load, stale entries accumulate indefinitely.
    // -------------------------------------------------------------------------
    static void uniqueThreadLocalPerRequest() throws InterruptedException {
        log.info("=== Scenario 4: NEW ThreadLocal PER REQUEST (worst pattern) ===");

        ExecutorService pool = Executors.newFixedThreadPool(1);

        for (int i = 1; i <= 3; i++) {
            final int id = i;
            pool.submit(() -> {
                // Anti-pattern: creating a NEW ThreadLocal instance inside the task
                ThreadLocal<byte[]> localBuffer = new ThreadLocal<>();
                localBuffer.set(new byte[512 * 1024]); // 512 KB

                log.info("[Request-{}] Created NEW ThreadLocal instance + stored 512KB value.", id);
                // localBuffer goes out of scope → its key in the ThreadLocalMap becomes
                // weakly reachable → stale entry. The 512KB VALUE is still strongly held
                // by the thread's ThreadLocalMap until the stale entry is expunged.
                // No localBuffer.remove() is possible after localBuffer itself is gone.
            });
            Thread.sleep(100);
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("3 stale entries with 512KB values may still exist in the pool thread's map.");
        log.info("Fix: Always use STATIC final ThreadLocal — never create one per request.\n");
    }

    public static void main(String[] args) throws InterruptedException {
        dataBleeding();
        memoryGrowth();
        correctPattern();
        uniqueThreadLocalPerRequest();

        log.info("=== Summary ===");
        log.info("Rule 1: Always call threadLocal.remove() in a finally block after each task.");
        log.info("Rule 2: Declare ThreadLocal as 'private static final' — never per-request.");
        log.info("Rule 3: In Java 21+, prefer ScopedValue over ThreadLocal for request-scoped data.");
    }
}
