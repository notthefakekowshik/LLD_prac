package com.leetcodeconcurrency.DesignPrintZeroEvenOdd;

public class DemoRunner {

    public static void main(String[] args) throws InterruptedException {
        int n = 5;
        SemaphoreZeroEvenOdd zeo = new SemaphoreZeroEvenOdd(n);

        Thread t0 = new Thread(new ZeroTask(zeo));
        Thread t1 = new Thread(new OddTask(zeo));
        Thread t2 = new Thread(new EvenTask(zeo));

        t0.start();
        t1.start();
        t2.start();

        t0.join();
        t1.join();
        t2.join();

        System.out.println("\nExpected: 0102030405");
    }

    static class ZeroTask implements Runnable {
        private final SemaphoreZeroEvenOdd zeo;

        ZeroTask(SemaphoreZeroEvenOdd zeo) {
            this.zeo = zeo;
        }

        @Override
        public void run() {
            try {
                zeo.zero(() -> System.out.print("0"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class OddTask implements Runnable {
        private final SemaphoreZeroEvenOdd zeo;

        OddTask(SemaphoreZeroEvenOdd zeo) {
            this.zeo = zeo;
        }

        @Override
        public void run() {
            try {
                zeo.odd(System.out::print);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class EvenTask implements Runnable {
        private final SemaphoreZeroEvenOdd zeo;

        EvenTask(SemaphoreZeroEvenOdd zeo) {
            this.zeo = zeo;
        }

        @Override
        public void run() {
            try {
                zeo.even(System.out::print);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
