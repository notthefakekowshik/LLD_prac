package com.kowshik.threads;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ThreadInterruptionDemo — The Interruption Contract
 *
 * INTERVIEW PREP:
 * ==============
 * Q: What does Thread.interrupt() actually do?
 * A: It sets the thread's internal "interrupted" flag to true. If the thread is
 *    blocked in certain operations (sleep, wait, join, LockSupport.park,
 *    BlockingQueue.take, Future.get), those operations throw InterruptedException
 *    and clear the flag. If the thread is running (not blocked), the flag stays
 *    set — the thread must poll via isInterrupted().
 *
 * Q: What's the difference between isInterrupted() and Thread.interrupted()?
 * A: isInterrupted() — instance method, returns flag state, does NOT clear it.
 *    Thread.interrupted() — static method, returns + CLEARS the interrupted
 *    flag of the current thread. This is the source of countless bugs.
 *
 * Q: When should I restore the interrupt vs propagate it?
 * A: RESTORE: You caught InterruptedException in a Runnable/Callable where you
 *       can't throw checked exceptions. Do: Thread.currentThread().interrupt();
 *       This preserves the interrupted status so callers up the stack know.
 *    PROPAGATE: Your method declares `throws InterruptedException`. Just re-throw.
 *       The caller handles it.
 *    NEVER: catch (InterruptedException e) { } // silent swallow — VIOLATION.
 *
 * Q: Which blocking operations respond to interruption?
 * A: Thread.sleep(), Object.wait(), Thread.join(),
 *    LockSupport.park() (used internally by locks, conditions, thread pools),
 *    BlockingQueue.put/take, Future.get, CompletableFuture.get,
 *    ReentrantLock.lockInterruptibly(),
 *    CountDownLatch/CyclicBarrier/Semaphore.await (offer timed variant).
 *
 * Q: What happens if you interrupt a thread that's NOT blocked?
 * A: Nothing happens immediately. The interrupted flag is set. The thread must
 *    check isInterrupted() in its run loop and react. No exception is thrown.
 */
public class ThreadInterruptionDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          THREAD INTERRUPTION CONTRACT — DEEP DIVE            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        part1_isInterrupted_vs_interrupted();
        part2_SleepInterruption();
        part3_BlockingQueueInterruption();
        part4_NonBlockingInterruption();
        part5_Restore_vs_Propagate();
        part6_InterruptibleLock();
        part7_ShutdownPattern();
    }

    // ================================================================
    // PART 1: isInterrupted() vs Thread.interrupted() — THE trap
    // ================================================================
    static void part1_isInterrupted_vs_interrupted() {
        System.out.println("┌─ PART 1: isInterrupted() vs Thread.interrupted() ─────────────┐");
        System.out.println("  This is the #1 source of interruption bugs.\n");

        Thread.currentThread().interrupt();

        // isInterrupted() — does NOT clear the flag
        boolean flag1 = Thread.currentThread().isInterrupted();
        boolean flag2 = Thread.currentThread().isInterrupted();
        System.out.println("  After interrupt() → isInterrupted(): " + flag1 + " (flag intact)");
        System.out.println("  Second call: " + flag2 + " (still true — flag not cleared)");
        System.out.println();

        // Thread.interrupted() — RETURNS + CLEARS the flag
        boolean flag3 = Thread.interrupted();
        boolean flag4 = Thread.currentThread().isInterrupted();
        System.out.println("  Thread.interrupted():  " + flag3 + " (returns true, then CLEARS flag)");
        System.out.println("  isInterrupted() after: " + flag4 + " (flag is now cleared)");
        System.out.println();

        // Clear before leaving (cleanup)
        Thread.interrupted(); // already false, but harmless
        System.out.println("  Rule: isInterrupted() — check, don't clear.");
        System.out.println("        Thread.interrupted() — check AND clear in one call.");
        System.out.println();
    }

    // ================================================================
    // PART 2: Sleep() — the classic interruption scenario
    // ================================================================
    static void part2_SleepInterruption() throws InterruptedException {
        System.out.println("┌─ PART 2: Thread.sleep() Interruption ─────────────────────────┐");

        Thread sleeper = new Thread(() -> {
            try {
                System.out.println("  Sleeper: going to sleep for 10 seconds...");
                Thread.sleep(10_000);
                System.out.println("  Sleeper: woke up naturally (shouldn't happen)");
            } catch (InterruptedException e) {
                System.out.println("  Sleeper: interrupted! Flag before restore = " + Thread.currentThread().isInterrupted());
                // Restore the interrupted status
                Thread.currentThread().interrupt();
                System.out.println("  Sleeper: flag after restore = " + Thread.currentThread().isInterrupted());
            }
        });

        sleeper.start();
        Thread.sleep(200);  // Let sleeper enter sleep
        System.out.println("  Main: interrupting sleeper...");
        sleeper.interrupt();
        sleeper.join();

        System.out.println("  Sleep ALWAYS checks the interrupted flag on entry.");
        System.out.println("  If already interrupted → throws InterruptedException immediately.");
        System.out.println();
    }

    // ================================================================
    // PART 3: BlockingQueue — handling interruption in put/take
    // ================================================================
    static void part3_BlockingQueueInterruption() throws InterruptedException {
        System.out.println("┌─ PART 3: BlockingQueue — put/take Interruption ───────────────┐");

        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(1);
        queue.put(42); // Fill the queue — capacity 1

        Thread producer = new Thread(() -> {
            try {
                System.out.println("  Producer: trying to put into full queue...");
                queue.put(99); // Will block forever — queue is full, no consumer
                System.out.println("  Producer: put succeeded (shouldn't happen)");
            } catch (InterruptedException e) {
                System.out.println("  Producer: interrupted while blocked on put()");
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        Thread.sleep(200);
        System.out.println("  Main: interrupting producer (stuck on full queue)...");
        producer.interrupt();
        producer.join();

        System.out.println("  BlockingQueue.put() checks interruption and throws.");
        System.out.println("  Same for take(), poll(timeout), offer(timeout).");
        System.out.println();
    }

    // ================================================================
    // PART 4: Non-blocking thread — must poll isInterrupted()
    // ================================================================
    static void part4_NonBlockingInterruption() throws InterruptedException {
        System.out.println("┌─ PART 4: Non-Blocking Thread — Must Poll isInterrupted() ──────┐");

        Thread worker = new Thread(() -> {
            int iterations = 0;
            while (!Thread.currentThread().isInterrupted()) {
                // CPU-bound work — no blocking calls
                for (int i = 0; i < 1_000_000; i++) {
                    Math.sqrt(i);
                }
                iterations++;
            }
            System.out.println("  Worker: detected interrupt after " + iterations + " iterations");
            System.out.println("  Worker: flag = " + Thread.currentThread().isInterrupted());
        });

        worker.start();
        Thread.sleep(200);
        System.out.println("  Main: interrupting CPU-bound worker...");
        worker.interrupt();
        worker.join();

        System.out.println("  Without a blocking call, the thread MUST poll isInterrupted().");
        System.out.println("  Interrupting a running thread just sets a flag — no exception.");
        System.out.println();
    }

    // ================================================================
    // PART 5: Restore vs Propagate — the golden rule
    // ================================================================
    static void part5_Restore_vs_Propagate() {
        System.out.println("┌─ PART 5: Restore vs Propagate — The Golden Rule ┐");

        // Rule 1: If your method signature allows it → propagate (re-throw)
        System.out.println("  RULE 1 — PROPAGATE:");
        System.out.println("    If your method declares `throws InterruptedException`:");
        System.out.println("      catch → clean up (close resources) → throw InterruptedException");
        System.out.println("    Caller handles it. Don't swallow, don't restore — propagate.");
        System.out.println();

        // Rule 2: If you can't throw checked exceptions → restore
        System.out.println("  RULE 2 — RESTORE:");
        System.out.println("    If you're in a Runnable/Callable/handler with no throws clause:");
        System.out.println("      catch (InterruptedException e) {");
        System.out.println("          Thread.currentThread().interrupt();  // restore the flag");
        System.out.println("          return;                                // or handle cleanly");
        System.out.println("      }");
        System.out.println("    This tells the thread pool / caller that this thread was interrupted.");
        System.out.println();

        // Rule 3: NEVER swallow silently
        System.out.println("  RULE 3 — NEVER DO THIS:");
        System.out.println("    catch (InterruptedException e) { }  // SILENT SWALLOW");
        System.out.println("    This discards the interrupt signal. The thread pool will never");
        System.out.println("    know this thread was interrupted. Threads will never shut down.");
        System.out.println("    This is the #1 cause of hung JVM shutdowns.");
        System.out.println();
    }

    // ================================================================
    // PART 6: ReentrantLock.lockInterruptibly()
    // ================================================================
    static void part6_InterruptibleLock() throws InterruptedException {
        System.out.println("┌─ PART 6: ReentrantLock.lockInterruptibly() ───────────────────┐");

        ReentrantLock lock = new ReentrantLock();
        lock.lock(); // Main thread holds the lock

        Thread waiter = new Thread(() -> {
            try {
                System.out.println("  Waiter: trying lockInterruptibly() — will block...");
                lock.lockInterruptibly(); // Blocks, but interruptible
                System.out.println("  Waiter: got lock (shouldn't happen)");
            } catch (InterruptedException e) {
                System.out.println("  Waiter: interrupted while waiting for lock!");
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        Thread.sleep(200);
        System.out.println("  Main: interrupting waiter (blocked on lockInterruptibly)...");
        waiter.interrupt();
        waiter.join(1000);

        lock.unlock();

        System.out.println("  Contrast with lock(): does NOT respond to interrupt.");
        System.out.println("  Rule: Use lockInterruptibly() when shutdown/cancellation matters.");
        System.out.println();
    }

    // ================================================================
    // PART 7: The Thread Pool Shutdown Pattern
    // ================================================================
    static void part7_ShutdownPattern() throws InterruptedException {
        System.out.println("┌─ PART 7: Thread Pool Shutdown Pattern ─────────────────────────┐");

        // The correct shutdown sequence for ExecutorService
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Submit a long-running task
        Future<?> future = pool.submit(() -> {
            try {
                System.out.println("  Worker: doing long computation...");
                for (int i = 0; i < 100 && !Thread.currentThread().isInterrupted(); i++) {
                    Thread.sleep(100); // Simulate work
                }
            } catch (InterruptedException e) {
                System.out.println("  Worker: interrupted — cleaning up resources...");
                Thread.currentThread().interrupt(); // restore
            }
        });

        Thread.sleep(300);

        System.out.println("  Main: initiating shutdown...");
        pool.shutdown();              // Stop accepting new tasks

        if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
            System.out.println("  Main: tasks didn't finish — forcing shutdown...");
            pool.shutdownNow();       // Interrupts all running tasks
            // shutdownNow() interrupts threads, but they still need
            // to cooperate by checking isInterrupted() / catching InterruptedException
        }

        System.out.println();
        System.out.println("  Correct shutdown sequence:");
        System.out.println("    1. pool.shutdown()             — Graceful: stop accepting new tasks");
        System.out.println("    2. pool.awaitTermination(N,s)  — Wait for running tasks to finish");
        System.out.println("    3. if timed out → pool.shutdownNow()  — Interrupt running threads");
        System.out.println("    4. awaitTermination again      — Wait for interrupted tasks to exit");
        System.out.println("    Tasks MUST cooperate by checking interruption.");
        System.out.println();
    }
}
