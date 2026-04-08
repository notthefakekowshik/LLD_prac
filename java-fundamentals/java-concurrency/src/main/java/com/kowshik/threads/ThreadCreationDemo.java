package com.kowshik.threads;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ThreadCreationDemo — Four Ways to Create a Thread
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What are the ways to create a thread in Java?
 * A: 4 approaches — extend Thread, implement Runnable, use Callable+Future, use lambda.
 *
 * Q: Which approach is preferred and why?
 * A: Runnable/lambda — Java has no multiple inheritance, so extending Thread wastes your
 *    one inheritance slot. Runnable separates the task (what to do) from the thread
 *    (how to run it).
 *
 * Q: When do you use Callable over Runnable?
 * A: When the task needs to return a result OR throw a checked exception.
 *    Runnable.run() is void and cannot declare checked exceptions.
 *
 * Q: What happens if you call run() instead of start()?
 * A: run() executes on the current thread — no new thread is spawned.
 *    It's a regular method call. start() creates a new OS thread and calls run() on it.
 */
public class ThreadCreationDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Approach 1: Extend Thread
    // AVOID: wastes your single inheritance slot
    // ─────────────────────────────────────────────────────────────────────────
    static class MyThread extends Thread {
        private final String taskName;

        MyThread(String taskName) {
            super(taskName); // sets thread name
            this.taskName = taskName;
        }

        @Override
        public void run() {
            System.out.printf("[%s] Approach 1 (extend Thread): running%n",
                    Thread.currentThread().getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Approach 2: Implement Runnable
    // PREFERRED for no-return tasks — class can still extend something else
    // ─────────────────────────────────────────────────────────────────────────
    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            System.out.printf("[%s] Approach 2 (implement Runnable): running%n",
                    Thread.currentThread().getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Approach 3: Callable + Future (via ExecutorService)
    // Use when: task produces a result OR throws a checked exception
    // ─────────────────────────────────────────────────────────────────────────
    static class MyCallable implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.printf("[%s] Approach 3 (Callable): computing...%n",
                    Thread.currentThread().getName());
            Thread.sleep(100);
            return 42;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Daemon vs User threads
    // ─────────────────────────────────────────────────────────────────────────
    static void daemonDemo() throws InterruptedException {
        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    System.out.println("[daemon] heartbeat");
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        daemon.setDaemon(true);   // MUST be set BEFORE start()
        daemon.setName("heartbeat-daemon");
        daemon.start();

        Thread.sleep(500);        // JVM exits here — daemon thread stops automatically
        System.out.println("[main] exiting — daemon will be killed by JVM");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Thread Creation Demo ===\n");

        // Approach 1: extend Thread
        Thread t1 = new MyThread("thread-extend");
        t1.start();
        t1.join(); // Wait for t1 to finish

        // Approach 2a: implement Runnable
        Thread t2 = new Thread(new MyRunnable(), "thread-runnable");
        t2.start();
        t2.join();

        // Approach 2b: lambda (most common in practice)
        Thread t3 = new Thread(
                () -> System.out.printf("[%s] Approach 2b (lambda): running%n",
                        Thread.currentThread().getName()),
                "thread-lambda"
        );
        t3.start();
        t3.join();

        // Approach 3: Callable + ExecutorService
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new MyCallable());
        System.out.println("[main] Callable result: " + future.get()); // blocks until done
        executor.shutdown();

        // Approach 4: Virtual thread (Java 21+)
        Thread vt = Thread.ofVirtual().name("virtual-worker").start(
                () -> System.out.printf("[%s] Approach 4 (virtual thread): running%n",
                        Thread.currentThread().getName())
        );
        vt.join();

        System.out.println("\n--- Named thread, daemon demo ---");
        Thread named = new Thread(() -> System.out.println("[named-thread] running!"));
        named.setName("my-worker-1");
        named.start();
        named.join();

        System.out.println("\n--- Daemon thread demo (JVM kills it when main exits) ---");
        daemonDemo();

        System.out.println("\n=== Done ===");
    }
}
