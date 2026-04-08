package com.leetcodeconcurrency.DesignPrintInOrder;

import java.util.concurrent.CountDownLatch;

/**
 * https://leetcode.com/problems/print-in-order/description/
 */


class Foo {

    private CountDownLatch latch1;
    private CountDownLatch latch2;

    public Foo() {
        latch1 = new CountDownLatch(1);
        latch2 = new CountDownLatch(1);
    }

    public void first(Runnable printFirst) throws InterruptedException {

        // printFirst.run() outputs "first". Do not change or remove this line.
        printFirst.run();
        latch1.countDown();
    }

    public void second(Runnable printSecond) throws InterruptedException {

        // printSecond.run() outputs "second". Do not change or remove this line.
        latch1.await();
        printSecond.run();
        latch2.countDown();
    }

    public void third(Runnable printThird) throws InterruptedException {

        // printThird.run() outputs "third". Do not change or remove this line.
        latch2.await();
        printThird.run();
    }
}


public class PrintOrder {

    public static void main(String[] args) {
        Foo foo = new Foo();

        Thread threadA = new Thread(() -> {
            try {
                foo.first(() -> System.out.println("first"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread threadB = new Thread(() -> {
            try {
                foo.second(() -> System.out.println("second"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread threadC = new Thread(() -> {
            try {
                foo.third(() -> System.out.println("third"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Start threads out of order to test the solution
        threadC.start();
        threadB.start();
        threadA.start();
    }
}
