package com.leetcodeconcurrency.DesignPrintZeroEvenOdd;

import java.util.concurrent.Semaphore;
import java.util.function.IntConsumer;

/**
 * Optimal solution using 3 Semaphores — O(1) directed signaling.
 *
 * Sequence: 0102030405... for n=5
 *
 * Design:
 *   zeroSem starts with 1 permit  → zero prints first
 *   oddSem  starts with 0 permits → odd waits
 *   evenSem starts with 0 permits → even waits
 *
 * Cycle per number i from 1..n:
 *   zero: acquire zeroSem → print 0 → release oddSem (if i odd) or evenSem (if i even)
 *   odd:  acquire oddSem  → print i → release zeroSem
 *   even: acquire evenSem → print i → release zeroSem
 *
 * Why 3 semaphores (not 2):
 *   zero needs a semaphore too — it must WAIT for odd/even to finish before printing
 *   the next zero. If zero had no semaphore, it would blast through all zeros without
 *   waiting, producing "00000..." instead of "010203...". The third semaphore
 *   (zeroSem) implements the handoff: odd/even → zero.
 *
 * Vs. brute force (synchronized + wait/notifyAll):
 *   Semaphore.release() wakes exactly ONE thread (fairness-dependent).
 *   notifyAll() wakes ALL threads — most re-check condition and go back to WAITING.
 *
 * LeetCode-compatible API: IntConsumer for 1..n, Runnable for zero.
 */
public class SemaphoreZeroEvenOdd {

    private final int n;
    private final Semaphore zeroSem = new Semaphore(1);
    private final Semaphore oddSem = new Semaphore(0);
    private final Semaphore evenSem = new Semaphore(0);

    public SemaphoreZeroEvenOdd(int n) {
        this.n = n;
    }

    public void zero(Runnable printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            zeroSem.acquire();
            printNumber.run();
            if (i % 2 == 1) {
                oddSem.release();
            } else {
                evenSem.release();
            }
        }
    }

    public void odd(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i += 2) {
            oddSem.acquire();
            printNumber.accept(i);
            zeroSem.release();
        }
    }

    public void even(IntConsumer printNumber) throws InterruptedException {
        for (int i = 2; i <= n; i += 2) {
            evenSem.acquire();
            printNumber.accept(i);
            zeroSem.release();
        }
    }
}
