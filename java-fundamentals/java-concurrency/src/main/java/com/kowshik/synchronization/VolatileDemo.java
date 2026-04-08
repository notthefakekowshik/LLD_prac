package com.kowshik.synchronization;

public class VolatileDemo {

    private static volatile int count = 0;

    public static void volatileCounterDemo() throws InterruptedException {
        Thread[] threads = new Thread[100];

        // Create 10 threads
        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    // THE BREAKING POINT
                    // This looks like one line, but it is 3 instructions.
                    count++;
                }
            });
            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }

        System.out.println("Expected Count: 1000000");
        System.out.println("Actual Count:   " + count);
    }

    public static void main(String[] args) throws InterruptedException {
//        volatileCounterDemo();
        fixForVolatile();
    }

    public static void fixForVolatile() throws InterruptedException {
        int []lock_count = new int[1];
        Thread[] lock_threads = new Thread[100];

        // Create 10 threads
        for (int i = 0; i < 100; i++) {
            lock_threads[i] = new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    synchronized (lock_count) {
                        lock_count[0]++;
                    }
                }
            });
            lock_threads[i].start();

        }

        // Wait for all threads to finish
        for (Thread t : lock_threads) {
            t.join();
        }

        System.out.println("Expected Count: 1000000");
        System.out.println("Actual Count:   " + lock_count[0]);

    }
}

/*
The command count++ is not magic. The CPU executes it in three steps:

LOAD: Read count from Main Memory into a CPU Register.

INCREMENT: Add 1 to the value in the Register (ALU operation).

STORE: Write the new value from the Register back to Main Memory.

volatile forces Step 1 and Step 3 to interact with Main Memory immediately (skipping cache). But it does not pause other threads during Step 2.
 */