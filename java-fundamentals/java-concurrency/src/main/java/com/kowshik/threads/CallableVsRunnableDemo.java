package com.kowshik.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * CallableVsRunnableDemo — Key Differences Between Runnable and Callable
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What is the difference between Runnable and Callable?
 * A:
 *   | Feature              | Runnable              | Callable<V>              |
 *   |----------------------|-----------------------|--------------------------|
 *   | Method               | void run()            | V call() throws Exception |
 *   | Return value         | None (void)           | Yes (generic type V)     |
 *   | Checked exceptions   | Cannot throw          | Can throw checked ones   |
 *   | Used with            | Thread, ExecutorService| ExecutorService, FutureTask |
 *   | Introduced in        | Java 1.0              | Java 5 (java.util.concurrent) |
 *
 * Q: Can you use Callable directly with a Thread?
 * A: No. Thread only accepts Runnable. Wrap the Callable in a FutureTask
 *    (which implements Runnable) to bridge the two.
 *
 * Q: How do you get the result of a Callable?
 * A: Submit it to an ExecutorService — it returns a Future<V>.
 *    Call future.get() (blocking) to retrieve the result.
 *
 * Q: What happens when a Callable throws an exception?
 * A: The exception is wrapped inside ExecutionException and re-thrown
 *    when you call future.get(). Unwrap via e.getCause().
 */
public class CallableVsRunnableDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Plain Runnable — fire-and-forget, no return value
    // ─────────────────────────────────────────────────────────────────────────
    static class PrintTask implements Runnable {
        private final String message;

        PrintTask(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            System.out.println("[Runnable] " + Thread.currentThread().getName()
                    + " → " + message);
            // Cannot return a value. Cannot declare checked exceptions.
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Callable<V> — returns a result and can throw checked exceptions
    // ─────────────────────────────────────────────────────────────────────────
    static class SquareTask implements Callable<Integer> {
        private final int number;

        SquareTask(int number) {
            this.number = number;
        }

        @Override
        public Integer call() throws Exception {
            System.out.println("[Callable] " + Thread.currentThread().getName()
                    + " → computing square of " + number);
            Thread.sleep(100); // simulate work; checked exception is allowed here
            return number * number;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Callable that deliberately throws a checked exception
    // ─────────────────────────────────────────────────────────────────────────
    static class FailingTask implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println("[Callable-Failing] " + Thread.currentThread().getName()
                    + " → about to throw");
            throw new Exception("Simulated processing failure");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(3);

        System.out.println("=== 1. Runnable — no return value ===");
        executor.submit(new PrintTask("Hello from Runnable"));

        // Lambda equivalent of Runnable
        executor.submit(() -> System.out.println("[Runnable-Lambda] "
                + Thread.currentThread().getName() + " → Lambda Runnable"));

        System.out.println("\n=== 2. Callable — returns a result ===");
        Future<Integer> squareFuture = executor.submit(new SquareTask(7));
        try {
            Integer result = squareFuture.get(); // blocks until done
            System.out.println("[Main] Square result received: " + result);
        } catch (ExecutionException e) {
            System.err.println("[Main] Task failed: " + e.getCause().getMessage());
        }

        // Lambda equivalent of Callable
        Future<String> lambdaFuture = executor.submit(() -> {
            Thread.sleep(50);
            return "[Callable-Lambda] " + Thread.currentThread().getName() + " → done";
        });
        try {
            System.out.println(lambdaFuture.get());
        } catch (ExecutionException e) {
            System.err.println("[Main] Lambda task failed: " + e.getCause().getMessage());
        }

        System.out.println("\n=== 3. Callable exception wrapped in ExecutionException ===");
        Future<String> failFuture = executor.submit(new FailingTask());
        try {
            failFuture.get(); // triggers the wrapped exception
        } catch (ExecutionException e) {
            System.out.println("[Main] Caught ExecutionException, root cause: "
                    + e.getCause().getMessage());
        }

        System.out.println("\n=== 4. Callable wrapped in FutureTask to run via Thread ===");
        // FutureTask implements Runnable AND Future — bridges Callable to Thread
        FutureTask<Integer> futureTask = new FutureTask<>(new SquareTask(9));
        Thread thread = new Thread(futureTask, "FutureTask-Thread");
        thread.start();
        thread.join(); // wait for it to finish
        try {
            System.out.println("[Main] FutureTask result from raw Thread: " + futureTask.get());
        } catch (ExecutionException e) {
            System.err.println("[Main] FutureTask failed: " + e.getCause().getMessage());
        }

        executor.shutdown();
        System.out.println("\n=== Done ===");
    }
}
