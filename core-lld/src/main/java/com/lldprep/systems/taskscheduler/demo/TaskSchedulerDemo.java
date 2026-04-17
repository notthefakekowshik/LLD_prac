package com.lldprep.taskscheduler.demo;

import com.lldprep.taskscheduler.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive demo of Task Scheduler capabilities.
 * 
 * Demonstrates:
 * - One-time delayed execution
 * - Absolute time scheduling
 * - Recurring tasks (fixed rate)
 * - Task cancellation
 * - Builder pattern usage
 * - Metrics collection
 */
public class TaskSchedulerDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("     Task Scheduler Demo");
        System.out.println("========================================\n");
        
        // Create scheduler with 4 worker threads
        TaskScheduler scheduler = new DelayQueueScheduler(4);
        
        demoOneTimeTasks(scheduler);
        demoAbsoluteTimeScheduling(scheduler);
        demoRecurringTasks(scheduler);
        demoBuilderPattern(scheduler);
        demoCancellation(scheduler);
        demoMetrics(scheduler);
        
        // Cleanup
        System.out.println("\n--- Shutting down scheduler ---");
        boolean terminated = scheduler.shutdown(5, TimeUnit.SECONDS);
        System.out.println("Shutdown " + (terminated ? "graceful" : "forced"));
    }
    
    private static void demoOneTimeTasks(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("--- Demo 1: One-time Delayed Tasks ---\n");
        
        // Task that runs after 1 second
        ScheduledTask task1 = scheduler.scheduleOnce(
            "quick-task", 
            () -> System.out.println("[1s] Quick task executed!"),
            1, TimeUnit.SECONDS
        );
        
        // Task that runs after 3 seconds
        ScheduledTask task2 = scheduler.scheduleOnce(
            "slow-task",
            () -> System.out.println("[3s] Slow task executed!"),
            3, TimeUnit.SECONDS
        );
        
        System.out.println("Scheduled: " + task1.getName() + " (in 1s)");
        System.out.println("Scheduled: " + task2.getName() + " (in 3s)");
        System.out.println("Waiting for tasks to complete...\n");
        
        Thread.sleep(4000); // Wait for both
    }
    
    private static void demoAbsoluteTimeScheduling(TaskScheduler scheduler) {
        System.out.println("\n--- Demo 2: Absolute Time Scheduling ---\n");
        
        // Schedule at specific time today (or tomorrow if passed)
        LocalTime futureTime = LocalTime.now().plusSeconds(2);
        ScheduledTask task = scheduler.scheduleAt(
            "lunch-reminder",
            () -> System.out.println("[Time-based] Reminder triggered at " + LocalTime.now()),
            futureTime
        );
        
        System.out.println("Scheduled lunch reminder at: " + futureTime);
        
        // Schedule at specific date-time
        LocalDateTime dateTime = LocalDateTime.now().plusSeconds(3);
        ScheduledTask task2 = scheduler.scheduleAt(
            "appointment",
            () -> System.out.println("[DateTime-based] Appointment notification!"),
            dateTime
        );
        
        System.out.println("Scheduled appointment at: " + dateTime);
        
        // Wait for these to complete
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void demoRecurringTasks(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("\n--- Demo 3: Recurring Tasks ---\n");
        
        // Fixed rate: execute every 500ms regardless of execution time
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
        
        // Let it run 5 times, then cancel
        Thread.sleep(3000);
        heartbeat.cancel();
        System.out.println("Heartbeat cancelled after " + heartbeat.getExecutionCount() + " beats\n");
        
        // Recurring with max executions using Duration
        final int[] count2 = {0};
        ScheduledTask limited = scheduler.scheduleRecurring(
            "limited-task",
            () -> {
                count2[0]++;
                System.out.println("[Limited #" + count2[0] + "] This will only run 3 times");
            },
            Duration.ofMillis(400)
        );
        
        // Manually limit to 3 executions by cancelling after
        Thread.sleep(1500);
        if (limited.getExecutionCount() >= 3) {
            limited.cancel();
            System.out.println("Limited task auto-cancelled at count: " + limited.getExecutionCount());
        }
    }
    
    private static void demoBuilderPattern(TaskScheduler scheduler) throws InterruptedException {
        System.out.println("\n--- Demo 4: Builder Pattern ---\n");
        
        // Fluent API for complex task configuration
        ScheduledTask complexTask = new TaskBuilder(scheduler, "data-sync", () -> {
            System.out.println("[Builder] Data sync running...");
        })
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
        
        // Cancel immediately
        boolean cancelled = cancelMe.cancel();
        System.out.println("Cancelled immediately: " + cancelled);
        System.out.println("Is cancelled: " + cancelMe.isCancelled());
        System.out.println("Is done: " + cancelMe.isDone());
        
        // Verify it doesn't run
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
