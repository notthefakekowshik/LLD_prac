package com.leetcodeconcurrency.printinorder;

/**
 * LeetCode 1114: Print in Order - Synchronized + Wait/Notify Approach
 *
 * Uses intrinsic locks (synchronized) and Object.wait()/notifyAll().
 * - state: 0 = first pending, 1 = second pending, 2 = third pending
 *
 * Pros: No external classes needed - pure Java language primitives.
 * Cons: Less flexible (no timeouts, no interrupt on wait), spurious wakeup handling needed.
 */
public class PrintInOrderSynchronized {

    private int state;

    public PrintInOrderSynchronized() {
        this.state = 0;
    }

    public void first(Runnable printFirst) throws InterruptedException {
        synchronized (this) {
            printFirst.run();
            state = 1;
            notifyAll();
        }
    }

    public void second(Runnable printSecond) throws InterruptedException {
        synchronized (this) {
            while (state != 1) {
                wait();
            }
            printSecond.run();
            state = 2;
            notifyAll();
        }
    }

    public void third(Runnable printThird) throws InterruptedException {
        synchronized (this) {
            while (state != 2) {
                wait();
            }
            printThird.run();
        }
    }
}
