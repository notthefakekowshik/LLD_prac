package com.leetcodeconcurrency.printinorder;

import java.util.concurrent.Semaphore;

/**
 * LeetCode 1114: Print in Order - Semaphore Approach
 *
 * Semaphore(0) starts with zero permits, causing acquire() to block.
 * - sem1: initially 0 permits, released by first() → signals first done
 * - sem2: initially 0 permits, released by second() → signals second done
 *
 * Pros: Cleaner API than CountDownLatch for this use case (no count management).
 * Cons: Slightly more overhead than CountDownLatch.
 */
public class PrintInOrderSemaphore {

    private final Semaphore sem1;
    private final Semaphore sem2;

    public PrintInOrderSemaphore() {
        this.sem1 = new Semaphore(0);
        this.sem2 = new Semaphore(0);
    }

    public void first(Runnable printFirst) throws InterruptedException {
        printFirst.run();
        sem1.release();
    }

    public void second(Runnable printSecond) throws InterruptedException {
        sem1.acquire();
        printSecond.run();
        sem2.release();
    }

    public void third(Runnable printThird) throws InterruptedException {
        sem2.acquire();
        printThird.run();
    }
}
