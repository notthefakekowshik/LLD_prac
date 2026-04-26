package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CachedThreadPoolDemo — newCachedThreadPool()
 *
 * INTERVIEW PREP:
 * ==============
 * Q: How does newCachedThreadPool differ from newFixedThreadPool?
 * A: Fixed pool has a capped thread count and queues overflow tasks.
 *    Cached pool spawns a new thread for EVERY task that finds no idle thread —
 *    no queue buffering (SynchronousQueue). Threads expire after 60s idle.
 *
 * Q: Why is newCachedThreadPool dangerous under load?
 * A: maximumPoolSize = Integer.MAX_VALUE. A burst of 10,000 submissions
 *    creates 10,000 OS threads → native memory exhaustion.
 *
 * Q: When is it safe to use?
 * A: Short-lived tasks with self-limiting arrival rate — e.g., handling
 *    callbacks from a fixed-size upstream connection pool.
 *
 * Demos:
 *   1. Thread elasticity — pool grows under burst, shrinks after idle
 *   2. Thread reuse — idle threads are reused before creating new ones
 *   3. OOM risk simulation — show rapid thread count growth
 *   4. Safe bounded alternative with SynchronousQueue
 */
public class CachedThreadPoolDemo {

    private static final Logger log = LoggerFactory.getLogger(CachedThreadPoolDemo.class);

    public static void main(String[] args) throws InterruptedException {
        elasticityDemo();
        threadReuseDemo();
        rapidGrowthDemo();
        safeBoundedAlternative();
        threadCountDrainDemo();
    }

    // -------------------------------------------------------------------------
    // Demo 1: Elasticity — grows to match burst, idles back down
    // -------------------------------------------------------------------------
    static void elasticityDemo() throws InterruptedException {
        log.info("=== Demo 1: Elasticity — burst then idle ===");
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        log.info("[Before burst] Pool size: {}", pool.getPoolSize());

        // Submit 8 tasks simultaneously — 8 new threads are created
        for (int i = 1; i <= 8; i++) {
            final int id = i;
            pool.submit(() -> {
                log.info("[Task-{}] thread={}", id, Thread.currentThread().getName());
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        Thread.sleep(100);
        log.info("[During burst] Pool size: {} (grew to match tasks)", pool.getPoolSize());

        Thread.sleep(1000); // all tasks finish
        log.info("[After tasks] Pool size: {} (threads idle, waiting for 60s reap)", pool.getPoolSize());

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 2: Thread reuse — second wave reuses idle threads from first wave
    // -------------------------------------------------------------------------
    static void threadReuseDemo() throws InterruptedException {
        log.info("=== Demo 2: Thread reuse — idle threads picked up for next tasks ===");
        ExecutorService pool = Executors.newCachedThreadPool();

        log.info("[Wave 1] submitting 3 tasks");
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            pool.submit(() -> {
                log.info("[Wave1-Task-{}] thread={}", id, Thread.currentThread().getName());
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        Thread.sleep(400); // wave 1 finishes, 3 threads are idle

        log.info("[Wave 2] submitting 3 more tasks — should reuse the same threads");
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            pool.submit(() -> {
                log.info("[Wave2-Task-{}] thread={}", id, Thread.currentThread().getName());
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 3: Rapid growth risk — shows thread count exploding
    // (deliberately small to avoid actually OOM'ing)
    // -------------------------------------------------------------------------
    static void rapidGrowthDemo() throws InterruptedException {
        log.info("=== Demo 3: Rapid growth — simulating burst risk ===");
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        // Each task sleeps 2s — no idle threads available → new thread per task
        for (int i = 1; i <= 20; i++) {
            pool.submit(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        Thread.sleep(200);
        log.info("[Peak] Pool size = {} threads created for 20 tasks", pool.getPoolSize());
        log.info("[Risk] In production with 10,000 tasks → 10,000 OS threads → OOM");

        pool.shutdownNow();
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 4: Safe bounded alternative — CachedThreadPool behavior but capped
    // -------------------------------------------------------------------------
    static void safeBoundedAlternative() throws InterruptedException {
        log.info("=== Demo 4: Safe bounded alternative (SynchronousQueue + capped max) ===");

        // Same SynchronousQueue as newCachedThreadPool, but max threads is capped
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                0,                                    // core = 0 (like cached)
                50,                                   // hard cap at 50 threads
                60L, TimeUnit.SECONDS,                // idle thread reap
                new SynchronousQueue<>(),             // direct handoff (like cached)
                r -> new Thread(r, "cached-bounded"), // named threads
                (task, executor) ->
                        log.warn("[REJECTED] No threads available and cap reached")
        );

        for (int i = 1; i <= 10; i++) {
            final int id = i;
            pool.submit(() -> {
                log.info("[Task-{}] thread={}", id, Thread.currentThread().getName());
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        log.info("[Done] Bounded cached pool completed safely.");
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 5: Thread count drain — watch live as pool size drops while consumers
    // finish tasks. A background monitor prints pool stats every 500ms so you
    // can see the thread count decreasing in real time.
    //
    // Flow:
    //   - 20 tasks submitted at once → 20 threads created (peak)
    //   - Each task simulates work (sleeps for a staggered duration)
    //   - As tasks complete, those threads go idle
    //   - Monitor prints: poolSize / activeCount / completedTasks every 500ms
    //   - You'll see: pool=20 active=20 → pool=20 active=14 → ... → active=0
    // -------------------------------------------------------------------------
    static void threadCountDrainDemo() throws InterruptedException {
        log.info("=== Demo 5: Live thread count drain as consumers finish ===");

        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        AtomicBoolean monitorRunning = new AtomicBoolean(true);

        // Background monitor — prints pool stats every 500ms
        Thread monitor = new Thread(() -> {
            while (monitorRunning.get()) {
                log.info("[Monitor] poolSize={} | active={} | idle={} | completed={}",
                        pool.getPoolSize(),
                        pool.getActiveCount(),
                        pool.getPoolSize() - pool.getActiveCount(),
                        pool.getCompletedTaskCount());
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }, "pool-monitor");
        monitor.setDaemon(true);
        monitor.start();

        // Submit 20 tasks with staggered durations (500ms to 3500ms)
        // so you can watch the thread count drop in waves
        for (int i = 1; i <= 20; i++) {
            final int id = i;
            final long workMs = 500L + (i * 150L); // task-1=650ms, task-20=3500ms
            pool.submit(() -> {
                log.info("[Consumer-{}] started, will run for {}ms", id, workMs);
                try { Thread.sleep(workMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                log.info("[Consumer-{}] finished", id);
            });
        }

        Thread.sleep(200); // let all tasks start
        log.info("[Peak] All 20 tasks running — pool has {} threads", pool.getPoolSize());

        // Wait for all tasks to complete
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        monitorRunning.set(false);
        log.info("[Final] poolSize={} | active={} | completed={}",
                pool.getPoolSize(), pool.getActiveCount(), pool.getCompletedTaskCount());
        log.info("[Note] Threads stay alive (idle) for 60s before being reaped — poolSize stays > 0");
        log.info("");
    }
}
