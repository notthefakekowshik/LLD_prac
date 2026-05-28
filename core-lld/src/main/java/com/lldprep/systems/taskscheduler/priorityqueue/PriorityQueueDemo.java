package com.lldprep.systems.taskscheduler.priorityqueue;

import com.lldprep.systems.taskscheduler.*;

import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the pain points of using PriorityQueue for scheduling.
 *
 * Key observations:
 * 1. Tasks fire slightly late because of POLL_INTERVAL_MS drift.
 * 2. CPU is consumed continuously even when no tasks are ready.
 * 3. Inserting a shorter-delay task after the dispatcher starts sleeping
 *    causes a missed wakeup — task still fires late.
 *
 * Compare output timestamps vs scheduled times to see timing drift.
 */
public class PriorityQueueDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=================================================");
        System.out.println("  PriorityQueue Scheduler — Pain Point Demo");
        System.out.println("=================================================\n");

        TaskScheduler scheduler = new PriorityQueueScheduler();

        demoPollDrift(scheduler);
        demoMissedWakeup(scheduler);
        demoONScanCancellation(scheduler);

        scheduler.shutdown(2, TimeUnit.SECONDS);
        System.out.println("\nShutdown complete.");
    }

    /**
     * Pain #1 — Poll drift: tasks fire up to POLL_INTERVAL_MS (10ms) late.
     */
    private static void demoPollDrift(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("--- Pain #1: Poll Drift ---");
        System.out.println("Scheduled 3 tasks at 200ms, 400ms, 600ms. Watch the actual fire times.\n");

        long base = System.currentTimeMillis();
        for (int i = 1; i <= 3; i++) {
            final int num = i;
            scheduler.scheduleOnce(
                "drift-task-" + i,
                () -> {
                    long drift = System.currentTimeMillis() - base - (num * 200L);
                    System.out.printf("  Task %d fired at ~%dms  (drift: +%dms)%n",
                        num, System.currentTimeMillis() - base, drift);
                },
                num * 200L, TimeUnit.MILLISECONDS
            );
        }

        Thread.sleep(900);
        System.out.println();
    }

    /**
     * Pain #2 — Missed wakeup: dispatcher sleeping, new shorter-delay task inserted.
     * With DelayQueue, inserting a new task with a shorter delay would immediately
     * signal the waiting thread. Here, the dispatcher keeps sleeping.
     */
    private static void demoMissedWakeup(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("--- Pain #2: Missed Wakeup ---");
        System.out.println("Scheduling a 2s task first, then a 50ms task immediately after.");
        System.out.println("If the dispatcher is sleeping for the 2s task, it misses the 50ms signal.\n");

        long start = System.currentTimeMillis();

        scheduler.scheduleOnce(
            "long-task",
            () -> System.out.printf("  long-task fired at ~%dms%n", System.currentTimeMillis() - start),
            2000, TimeUnit.MILLISECONDS
        );

        // Insert a much shorter task immediately — dispatcher may already be sleeping
        scheduler.scheduleOnce(
            "short-task",
            () -> System.out.printf("  short-task fired at ~%dms (should be ~50ms, not 2000ms)%n",
                System.currentTimeMillis() - start),
            50, TimeUnit.MILLISECONDS
        );

        Thread.sleep(2200);
        System.out.println();
    }

    /**
     * Pain #3 — O(n) cancellation: must scan the entire queue.
     */
    private static void demoONScanCancellation(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("--- Pain #3: O(n) Cancellation ---");
        System.out.println("Scheduling 5 tasks, then cancelling by ID (requires full queue scan).\n");

        ScheduledTask toCancel = null;
        for (int i = 1; i <= 5; i++) {
            final int num = i;
            ScheduledTask t = scheduler.scheduleOnce(
                "cancel-demo-" + i,
                () -> System.out.println("  Task " + num + " executed (this should NOT print for task 3)"),
                3000, TimeUnit.MILLISECONDS
            );
            if (i == 3) toCancel = t;
        }

        System.out.println("  Cancelling task 3 by ID — PQ must scan all 5 elements.");
        boolean cancelled = scheduler.cancel(toCancel.getId());
        System.out.println("  Cancelled: " + cancelled);
        System.out.println("  (With DelayQueue, this would be O(1) via internal HashMap)\n");

        scheduler.cancelAll();
    }
}
