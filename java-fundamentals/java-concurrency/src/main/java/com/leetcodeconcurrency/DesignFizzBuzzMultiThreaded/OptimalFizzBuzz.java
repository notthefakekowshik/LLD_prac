package com.leetcodeconcurrency.DesignFizzBuzzMultiThreaded;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntConsumer;

public class OptimalFizzBuzz {

    private int n;
    private int current = 1;
    private Lock lock = new ReentrantLock();
    private Condition condNum = lock.newCondition();
    private Condition condFizz = lock.newCondition();
    private Condition condBuzz = lock.newCondition();
    private Condition condFizzBuzz = lock.newCondition();

    public OptimalFizzBuzz(int n) {
        this.n = n;
    }

    // printFizz.run() outputs "fizz".
    public void fizz(Runnable printFizz) throws InterruptedException {
        lock.lock();
        try {
            while (current <= n) {
                if (current % 3 == 0 && current % 5 != 0) {
                    printFizz.run();
                    current++;
                    signalNext();
                } else {
                    condFizz.await();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // printBuzz.run() outputs "buzz".
    public void buzz(Runnable printBuzz) throws InterruptedException {
        lock.lock();
        try {
            while (current <= n) {
                if (current % 5 == 0 && current % 3 != 0) {
                    printBuzz.run();
                    current++;
                    signalNext();
                } else {
                    condBuzz.await();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // printFizzBuzz.run() outputs "fizzbuzz".
    public void fizzbuzz(Runnable printFizzBuzz) throws InterruptedException {
        lock.lock();
        try {
            while (current <= n) {
                if (current % 15 == 0) {
                    printFizzBuzz.run();
                    current++;
                    signalNext();
                } else {
                    condFizzBuzz.await();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // printNumber.accept(x) outputs "x", where x is an integer.
    public void number(IntConsumer printNumber) throws InterruptedException {
        lock.lock();
        try {
            while (current <= n) {
                if (current % 3 != 0 && current % 5 != 0) {
                    printNumber.accept(current);
                    current++;
                    signalNext();
                } else {
                    condNum.await();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void signalNext() {
        if (current > n) {
            // Signal all to exit
            condNum.signalAll();
            condFizz.signalAll();
            condBuzz.signalAll();
            condFizzBuzz.signalAll();
            return;
        }

        if (current % 15 == 0) {
            condFizzBuzz.signal();
        } else if (current % 3 == 0) {
            condFizz.signal();
        } else if (current % 5 == 0) {
            condBuzz.signal();
        } else {
            condNum.signal();
        }
    }
}

class MainOptimal {
    public static void main(String[] args) {
        OptimalFizzBuzz fizzBuzz = new OptimalFizzBuzz(15);

        Thread threadA = new Thread(() -> {
            try {
                fizzBuzz.fizz(() -> System.out.println("fizz"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread threadB = new Thread(() -> {
            try {
                fizzBuzz.buzz(() -> System.out.println("buzz"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread threadC = new Thread(() -> {
            try {
                fizzBuzz.fizzbuzz(() -> System.out.println("fizzbuzz"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread threadD = new Thread(() -> {
            try {
                fizzBuzz.number(System.out::println);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        threadA.start();
        threadB.start();
        threadC.start();
        threadD.start();
    }
}
