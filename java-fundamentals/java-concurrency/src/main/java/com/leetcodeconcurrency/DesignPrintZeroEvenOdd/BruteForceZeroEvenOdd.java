package com.leetcodeconcurrency.DesignPrintZeroEvenOdd;

public class BruteForceZeroEvenOdd {

    private int n;
    private int current = 0;

    public BruteForceZeroEvenOdd(int n) {
        this.n = n;
    }

    void zero(Runnable printNumber) {
        synchronized (this) {
            while (current <= n) {
                if (current == 0) {
                    printNumber.run();
                    current++;
                } else {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                notifyAll();
            }
        }
    }


    void even(Runnable printNumber) {
        synchronized (this) {
            while (current <= n) {
                if (current % 2 == 0) {
                    printNumber.run();
                    current++;
                } else {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                notifyAll();
            }
        }
    }

    void odd(Runnable printNumber) {
        synchronized (this) {
            while (current <= n) {
                if (current % 2 != 0) {
                    printNumber.run();
                    current++;
                } else {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                notifyAll();
            }
        }
    }

}

class Main {
    public static void main(String[] args) {
        BruteForceZeroEvenOdd bruteForceZeroEvenOdd = new BruteForceZeroEvenOdd(20);

        Thread zeroThread = new Thread(() -> bruteForceZeroEvenOdd.zero(() -> System.out.println("zero")));
        Thread evenThread = new Thread(() -> bruteForceZeroEvenOdd.even(() -> System.out.println("zero")));
        Thread oddThread = new Thread(() -> bruteForceZeroEvenOdd.odd(() -> System.out.println("zero")));


        zeroThread.start();
        evenThread.start();
        oddThread.start();

    }
}
