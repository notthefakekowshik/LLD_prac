package com.lldprep.systems.taskscheduler.delayqueue;

import com.lldprep.systems.taskscheduler.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive demo of the DelayQueue-based Task Scheduler.
 *
 * Demonstrates:
 * - One-time delayed execution
 * - Absolute time scheduling
 * - Recurring tasks (fixed rate)
 * - Task cancellation
 * - Builder pattern usage
 * - Metrics collection
 */
public class DelayQueueDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("     DelayQueue Task Scheduler Demo");
        System.out.println("========================================\n");

        TaskScheduler scheduler = new DelayQueueScheduler(4);

        demoOneTimeTasks(scheduler);
        demoAbsoluteTimeScheduling(scheduler);
        demoRecurringTasks(scheduler);
        demoBuilderPattern(scheduler);
        demoCancellation(scheduler);
        demoMetrics(scheduler);

        System.out.println("\n--- Shutting down scheduler ---");
        boolean terminated = scheduler.shutdown(5, TimeUnit.SECONDS);
        System.out.println("Shutdown " + (terminated ? "graceful" : "forced"));
    }

    private static void demoOneTimeTasks(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("--- Demo 1: One-time Delayed Tasks ---\n");

        ScheduledTask task1 = scheduler.scheduleOnce(
            "quick-task",
            () -> System.out.println("[1s] Quick task executed!"),
            1, TimeUnit.SECONDS
        );

        ScheduledTask task2 = scheduler.scheduleOnce(
            "slow-task",
            () -> System.out.println("[3s] Slow task executed!"),
            3, TimeUnit.SECONDS
        );

        System.out.println("Scheduled: " + task1.getName() + " (in 1s)");
        System.out.println("Scheduled: " + task2.getName() + " (in 3s)");
        System.out.println("Waiting for tasks to complete...\n");

        Thread.sleep(4000);
    }

    private static void demoAbsoluteTimeScheduling(TaskScheduler scheduler) {
        System.out.println("\n--- Demo 2: Absolute Time Scheduling ---\n");

        LocalTime futureTime = LocalTime.now().plusSeconds(2);
        scheduler.scheduleAt(
            "lunch-reminder",
            () -> System.out.println("[Time-based] Reminder triggered at " + LocalTime.now()),
            futureTime
        );
        System.out.println("Scheduled lunch reminder at: " + futureTime);

        LocalDateTime dateTime = LocalDateTime.now().plusSeconds(3);
        scheduler.scheduleAt(
            "appointment",
            () -> System.out.println("[DateTime-based] Appointment notification!"),
            dateTime
        );
        System.out.println("Scheduled appointment at: " + dateTime);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void demoRecurringTasks(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("\n--- Demo 3: Recurring Tasks ---\n");

        final int[] count1 = {0};
        ScheduledTask heartbeat = scheduler.scheduleAtFixedRate(
            "heartbeat",
            () -> {
                count1[0]++;
                System.out.println("[Heartbeat #" + count1[0] + "] thump-thump at " +
                    LocalTime.now().toSecondOfDay() % 60 + "s");
            },
            0, 500, TimeUnit.MILLISECONDS
        );

        Thread.sleep(3000);
        heartbeat.cancel();
        System.out.println("Heartbeat cancelled after " + heartbeat.getExecutionCount() + " beats\n");

        final int[] count2 = {0};
        ScheduledTask limited = scheduler.scheduleRecurring(
            "limited-task",
            () -> {
                count2[0]++;
                System.out.println("[Limited #" + count2[0] + "] This will only run 3 times");
            },
            Duration.ofMillis(400)
        );

        Thread.sleep(1500);
        if (limited.getExecutionCount() >= 3) {
            limited.cancel();
            System.out.println("Limited task auto-cancelled at count: " + limited.getExecutionCount());
        }
    }

    private static void demoBuilderPattern(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("\n--- Demo 4: Builder Pattern ---\n");

        ScheduledTask complexTask = new TaskBuilder(scheduler, "data-sync", () ->
            System.out.println("[Builder] Data sync running..."))
            .runEvery(1, TimeUnit.SECONDS)
            .startingIn(500, TimeUnit.MILLISECONDS)
            .maxTimes(3)
            .schedule();

        System.out.println("Scheduled via builder: " + complexTask.getName());
        System.out.println("Config: run every 1s, start in 500ms, max 3 times");

        Thread.sleep(4000);
        System.out.println("Builder task executed " + complexTask.getExecutionCount() + " times");
    }

    private static void demoCancellation(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("\n--- Demo 5: Task Cancellation ---\n");

        ScheduledTask cancelMe = scheduler.scheduleOnce(
            "cancel-me",
            () -> System.out.println("This should NOT print!"),
            5, TimeUnit.SECONDS
        );

        System.out.println("Scheduled task to run in 5s: " + cancelMe.getName());
        System.out.println("Task scheduled time: " + cancelMe.getScheduledTime());

        boolean cancelled = cancelMe.cancel();
        System.out.println("Cancelled immediately: " + cancelled);
        System.out.println("Is cancelled: " + cancelMe.isCancelled());
        System.out.println("Is done: " + cancelMe.isDone());

        Thread.sleep(2000);
        System.out.println("After 2 seconds, task execution count: " + cancelMe.getExecutionCount());
    }

    private static void demoMetrics(TaskScheduler scheduler) {
        System.out.println("\n--- Demo 6: Metrics ---\n");

        SchedulerMetrics metrics = scheduler.getMetrics();
        System.out.println("Final Scheduler Metrics:");
        System.out.println("  - Tasks Submitted:  " + metrics.tasksSubmitted());
        System.out.println("  - Tasks Executed:   " + metrics.tasksExecuted());
        System.out.println("  - Tasks Cancelled:  " + metrics.tasksCancelled());
        System.out.println("  - Pending Tasks:    " + metrics.pendingTasks());
        System.out.println("  - Worker Pool Size: " + metrics.workerPoolSize());
        System.out.println("  - In Flight:        " + metrics.tasksInFlight());
        System.out.println("  - Success Rate:     " + String.format("%.1f%%", metrics.successRate() * 100));
        System.out.println("\nRaw: " + metrics);
    }
}
