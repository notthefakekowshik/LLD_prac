package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ScheduledThreadPoolDemo — newScheduledThreadPool(n)
 *
 * INTERVIEW PREP:
 * ==============
 * Q: Difference between scheduleAtFixedRate and scheduleWithFixedDelay?
 * A: AtFixedRate — period measured from START of last execution.
 *    WithFixedDelay — delay measured from END of last execution.
 *    If task takes longer than the period, AtFixedRate catches up (runs
 *    immediately after completion); WithFixedDelay always waits the full delay.
 *
 * Q: What happens if a task throws in scheduleAtFixedRate?
 * A: Subsequent executions are SILENTLY SUPPRESSED. No exception propagated.
 *    ScheduledFuture.get() will rethrow it. Always wrap in try-catch.
 *
 * Q: How does it differ from a Timer?
 * A: Timer uses a single thread (one slow task blocks all others).
 *    ScheduledThreadPool uses multiple threads and handles exceptions gracefully.
 *    Timer is also not thread-safe.
 *
 * Demos:
 *   1. schedule() — one-shot delayed task
 *   2. scheduleAtFixedRate — fixed wall-clock intervals
 *   3. scheduleWithFixedDelay — fixed gap after completion
 *   4. Slow task: AtFixedRate vs WithFixedDelay side-by-side
 *   5. Silent suppression on exception — and the fix
 */
public class ScheduledThreadPoolDemo {

    private static final Logger log = LoggerFactory.getLogger(ScheduledThreadPoolDemo.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        scheduleOnce();
        fixedRateDemo();
        fixedDelayDemo();
        slowTaskComparison();
        silentSuppressionDemo();
    }

    // -------------------------------------------------------------------------
    // Demo 1: schedule() — run once after a delay
    // -------------------------------------------------------------------------
    static void scheduleOnce() throws InterruptedException, ExecutionException {
        log.info("=== Demo 1: schedule() — one-shot after 500ms ===");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        long start = System.currentTimeMillis();
        ScheduledFuture<String> future = scheduler.schedule(
                () -> "Hello from delayed task!",
                500, TimeUnit.MILLISECONDS
        );

        String result = future.get(); // blocks until executed
        log.info("[Result] '{}' — executed after {}ms", result, System.currentTimeMillis() - start);

        scheduler.shutdown();
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 2: scheduleAtFixedRate — tick every 500ms on the wall clock
    // -------------------------------------------------------------------------
    static void fixedRateDemo() throws InterruptedException {
        log.info("=== Demo 2: scheduleAtFixedRate — every 500ms ===");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        AtomicInteger count = new AtomicInteger();

        ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(() -> {
            int n = count.incrementAndGet();
            log.info("[FixedRate tick #{}] at {}", n, LocalTime.now());
        }, 0, 500, TimeUnit.MILLISECONDS);

        Thread.sleep(2200); // let ~4 ticks fire
        handle.cancel(false);
        scheduler.shutdown();
        scheduler.awaitTermination(2, TimeUnit.SECONDS);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 3: scheduleWithFixedDelay — wait 500ms AFTER each completion
    // -------------------------------------------------------------------------
    static void fixedDelayDemo() throws InterruptedException {
        log.info("=== Demo 3: scheduleWithFixedDelay — 500ms gap after each completion ===");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        AtomicInteger count = new AtomicInteger();

        ScheduledFuture<?> handle = scheduler.scheduleWithFixedDelay(() -> {
            int n = count.incrementAndGet();
            log.info("[FixedDelay tick #{}] started at {}", n, LocalTime.now());
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            log.info("[FixedDelay tick #{}] finished — next runs 500ms after THIS line", n);
        }, 0, 500, TimeUnit.MILLISECONDS);

        Thread.sleep(2500);
        handle.cancel(false);
        scheduler.shutdown();
        scheduler.awaitTermination(2, TimeUnit.SECONDS);
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 4: Slow task — AtFixedRate catches up, WithFixedDelay always waits
    // Task takes 800ms but period/delay is 500ms
    // -------------------------------------------------------------------------
    static void slowTaskComparison() throws InterruptedException {
        log.info("=== Demo 4: Slow task (800ms) with period/delay of 500ms ===");

        ScheduledExecutorService s1 = Executors.newScheduledThreadPool(1);
        ScheduledExecutorService s2 = Executors.newScheduledThreadPool(1);
        AtomicInteger rateCount = new AtomicInteger();
        AtomicInteger delayCount = new AtomicInteger();

        // AtFixedRate: next run scheduled at start+500ms — if task took 800ms,
        // next run fires IMMEDIATELY after completion (runs back-to-back)
        s1.scheduleAtFixedRate(() -> {
            int n = rateCount.incrementAndGet();
            log.info("[AtFixedRate #{}] start={}", n, LocalTime.now());
            try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, 0, 500, TimeUnit.MILLISECONDS);

        // WithFixedDelay: next run always starts 500ms AFTER task finishes
        s2.scheduleWithFixedDelay(() -> {
            int n = delayCount.incrementAndGet();
            log.info("[WithDelay  #{}] start={}", n, LocalTime.now());
            try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, 0, 500, TimeUnit.MILLISECONDS);

        Thread.sleep(4000);

        s1.shutdownNow();
        s2.shutdownNow();
        log.info("[Summary] AtFixedRate fired {} times, WithFixedDelay fired {} times in 4s",
                rateCount.get(), delayCount.get());
        log.info("[AtFixedRate runs more often — it catches up after slow executions]");
        log.info("");
    }

    // -------------------------------------------------------------------------
    // Demo 5: Silent suppression on uncaught exception — and the fix
    // -------------------------------------------------------------------------
    static void silentSuppressionDemo() throws InterruptedException {
        log.info("=== Demo 5: Silent suppression — exception stops ALL future runs ===");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger count = new AtomicInteger();

        // BUG: exception on tick 2 silently stops all subsequent ticks
        ScheduledFuture<?> buggy = scheduler.scheduleAtFixedRate(() -> {
            int n = count.incrementAndGet();
            log.info("[Buggy tick #{}]", n);
            if (n == 2) throw new RuntimeException("Crash on tick 2!");
            // tick 3, 4, 5... never happen — silently suppressed
        }, 0, 300, TimeUnit.MILLISECONDS);

        Thread.sleep(1200);
        log.info("[After 1.2s] buggy task fired {} time(s) — stopped after the exception", count.get());

        // FIX: wrap body in try-catch
        AtomicInteger fixedCount = new AtomicInteger();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int n = fixedCount.incrementAndGet();
                log.info("[Fixed tick #{}]", n);
                if (n == 2) throw new RuntimeException("Crash on tick 2 (handled)");
            } catch (Exception e) {
                log.warn("[Fixed tick] caught exception: {} — continuing", e.getMessage());
            }
        }, 0, 300, TimeUnit.MILLISECONDS);

        Thread.sleep(1200);
        log.info("[After 1.2s] fixed task fired {} times — survived the exception", fixedCount.get());

        scheduler.shutdownNow();
        log.info("");
    }
}
