package com.leetcodeconcurrency.printinorder;

import java.util.concurrent.CountDownLatch;

/**
 * LeetCode 1114: Print in Order
 *
 * Design a class to ensure three methods are executed in order by three different threads:
 * - first() prints "first"
 * - second() prints "second" (must wait for first)
 * - third() prints "third" (must wait for second)
 *
 * Approach: CountDownLatch
 * - latch1: signals first() is complete (releases second())
 * - latch2: signals second() is complete (releases third())
 */
public class PrintInOrder {

    private final CountDownLatch latch1;
    private final CountDownLatch latch2;

    public PrintInOrder() {
        this.latch1 = new CountDownLatch(1);
        this.latch2 = new CountDownLatch(1);
    }

    public void first(Runnable printFirst) throws InterruptedException {
        printFirst.run();
        latch1.countDown();
    }

    public void second(Runnable printSecond) throws InterruptedException {
        latch1.await();
        printSecond.run();
        latch2.countDown();
    }

    public void third(Runnable printThird) throws InterruptedException {
        latch2.await();
        printThird.run();
    }
}
