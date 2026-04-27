package com.leetcodeconcurrency.printinorder;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LeetCode 1114: Print in Order - ReentrantLock + Condition Approach
 *
 * Uses explicit locking with two condition variables for fine-grained waiting/signaling.
 * - lock: protects shared state (flags)
 * - cond1: signals first is done
 * - cond2: signals second is done
 * - firstDone, secondDone: boolean flags (prevents missed signals)
 *
 * Pros: Most flexible - allows selective wakeups, interrupt handling, timeouts.
 * Cons: Verbose - requires lock/unlock pairs and flag management.
 */
public class PrintInOrderCondition {

    private final ReentrantLock lock;
    private final Condition cond1;
    private final Condition cond2;
    private boolean firstDone;
    private boolean secondDone;

    public PrintInOrderCondition() {
        this.lock = new ReentrantLock();
        this.cond1 = lock.newCondition();
        this.cond2 = lock.newCondition();
        this.firstDone = false;
        this.secondDone = false;
    }

    public void first(Runnable printFirst) throws InterruptedException {
        lock.lock();
        try {
            printFirst.run();
            firstDone = true;
            cond1.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void second(Runnable printSecond) throws InterruptedException {
        lock.lock();
        try {
            while (!firstDone) {
                cond1.await();
            }
            printSecond.run();
            secondDone = true;
            cond2.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void third(Runnable printThird) throws InterruptedException {
        lock.lock();
        try {
            while (!secondDone) {
                cond2.await();
            }
            printThird.run();
        } finally {
            lock.unlock();
        }
    }
}
