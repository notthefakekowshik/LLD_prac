package com.leetcodeconcurrency.DesignPrintInOrder;

import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

class FooSemaphoreBetter {

    private Semaphore run2, run3;

    public FooSemaphoreBetter() {
        run2 = new Semaphore(0);
        run3 = new Semaphore(0);
    }

    public void first(Runnable printFirst) throws InterruptedException {
        // printFirst.run() outputs "first". Do not change or remove this line.
        printFirst.run();
        run2.release();
    }

    public void second(Runnable printSecond) throws InterruptedException {
        // printSecond.run() outputs "second". Do not change or remove this line.
        run2.acquire();
        printSecond.run();
        run3.release();
    }

    public void third(Runnable printThird) throws InterruptedException {
        // printThird.run() outputs "third". Do not change or remove this line.
        run3.acquire();
        printThird.run();
    }
}

class FooSemaphoreBruteForce {

    private Semaphore run1, run2, run3;

    public FooSemaphoreBruteForce() {
        run1 = new Semaphore(1);
        run2 = new Semaphore(0);
        run3 = new Semaphore(0);
    }

    public void first(Runnable printFirst) throws InterruptedException {
        // printFirst.run() outputs "first". Do not change or remove this line.
        run1.acquire(); // this has acquired the run1 semaphore, no one else can take this semaphore.
        printFirst.run();
        run2.release(); // this says the JVM to release the run2 semaphore.
    }

    public void second(Runnable printSecond) throws InterruptedException {
        // printSecond.run() outputs "second". Do not change or remove this line.
        run2.acquire(); // this has acquired the run2 semaphore, no one else can take this semaphore. No
                        // one else claims the run2 semaphore. That's how our code is written and that's
                        // how it should be written.
        printSecond.run();
        run3.release();
    }

    public void third(Runnable printThird) throws InterruptedException {
        // printThird.run() outputs "third". Do not change or remove this line.
        run3.acquire();
        printThird.run();
        run1.release();
    }
}

public class PrintOrderSemaphore {

    public static void main(String[] args) {
        Consumer<String> printCosumer = (message) -> System.out.println(message);

        // Threads can't be started again which are done executing, so, you need to
        // create them again.
        // That's the reason for putting loop around creation but not starting.
        int counter = 3;
        while (counter-- > 0) {
            FooSemaphoreBetter foo = new FooSemaphoreBetter();

            Thread threadA = new Thread(() -> {
                try {
                    foo.first(() -> printCosumer.accept("first"));
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

            threadC.start();
            threadB.start();
            threadA.start();

            // Wait for threads to finish before the next iteration
            try {
                threadA.join();  // MAIN thread waits for A to finish
                threadB.join();  // MAIN thread waits for B to finish
                threadC.join();  // MAIN thread waits for C to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
