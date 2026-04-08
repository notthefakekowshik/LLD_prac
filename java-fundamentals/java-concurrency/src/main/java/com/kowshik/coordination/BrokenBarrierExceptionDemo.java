package com.kowshik.coordination;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

/*

The Danger Zone: The Broken Barrier In a distributed system, things fail.
What happens if you have 3 threads waiting, and Thread 2 throws an exception or gets interrupted before reaching the barrier?

The Solution: 
Handling BrokenBarrierException CyclicBarrier has a built-in mechanism for this.
If any thread waiting on the barrier is interrupted or times out, the barrier enters a broken state.
It immediately wakes up all other waiting threads by throwing a BrokenBarrierException.

*/
public class BrokenBarrierExceptionDemo {

    public static void main(String[] args) throws InterruptedException {
        // 1. Create a barrier for 3 parties
        CyclicBarrier barrier = new CyclicBarrier(3,
                () -> System.out.println(">> Barrier Action (This will NEVER run in this example)"));

        // 2. Create and start the first two "Good" threads
        Thread t1 = new Thread(new Worker(barrier, "Thread-1"));
        Thread t2 = new Thread(new Worker(barrier, "Thread-2"));

        t1.start();
        t2.start();

        System.out.println("Sleeping for 5 secs");
        Thread.sleep(5000);
        System.out.println("Waking up after 5 secs and will interrupt Thread-2");
        t2.interrupt();

        // 3. Create the "Victim" thread
        Thread t3 = new Thread(new Worker(barrier, "Thread-3 (Victim)"));
        t3.start();

        // Give t3 a moment to hit the barrier
        Thread.sleep(5000);
    }

    static class Worker implements Runnable {
        private CyclicBarrier barrier;
        private String name;

        public Worker(CyclicBarrier barrier, String name) {
            this.barrier = barrier;
            this.name = name;
        }

        @Override
        public void run() {
            try {
                System.out.println(name + " is waiting at the barrier...");
                // The thread blocks here
                barrier.await();
                System.out.println(name + " crossed the barrier successfully!");

            } catch (InterruptedException e) {
                // This block runs for the thread that was explicitly interrupted
                System.out.println("❌ " + name + " was INTERRUPTED!");

            } catch (BrokenBarrierException e) {
                // This block runs for the threads that were 'innocent' but got kicked out
                // (Thread-1, Thread-2)
                System.out.println("⚠️ " + name + " caught BrokenBarrierException (Someone else failed!)");
            }
        }
    }
}
