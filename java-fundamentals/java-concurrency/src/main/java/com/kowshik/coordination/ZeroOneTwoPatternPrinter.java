package com.kowshik.coordination;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

class ZeroEvenOddPrinterSemaphore {
    private int n;
    // zeroSem starts with a permit, as we must print 0 first.
    private Semaphore zeroSem = new Semaphore(1);
    // oddSem and evenSem start with no permits, forcing them to wait.
    private Semaphore oddSem = new Semaphore(0);
    private Semaphore evenSem = new Semaphore(0);

    public ZeroEvenOddPrinterSemaphore(int n) {
        this.n = n;
    }

    // printNumber.accept(x) is just a way to print x
    public void zero(Consumer<Integer> printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            zeroSem.acquire();
            printNumber.accept(0);
            oddSem.release();
        }
    }

    public void even(Consumer<Integer> printNumber) throws InterruptedException {
        for (int i = 2; i <= n; i += 2) {
            evenSem.acquire();
            printNumber.accept(i);
            zeroSem.release();
        }
    }

    public void odd(Consumer<Integer> printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i += 2) {
            oddSem.acquire();
            printNumber.accept(i);
            evenSem.release();
        }
    }
}

public class ZeroOneTwoPatternPrinter {
    public static void main(String[] args) {
        ZeroEvenOddPrinterSemaphore printer = new ZeroEvenOddPrinterSemaphore(6);
        Consumer<Integer> consumer = (val) -> System.out.println(val);

//        Thread zeroThread = new Thread(() -> {
//            try {
//                printer.zero(consumer);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        });
//
//        Thread evenThread = new Thread(() -> {
//            try {
//                printer.even(consumer);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        });
//
//        Thread oddThread = new Thread(() -> {
//            try {
//                printer.odd(consumer);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        });

        try(ExecutorService executorService = Executors.newFixedThreadPool(3)) {
            executorService.submit(() -> {
                try {
                    printer.odd(consumer);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            executorService.submit(() -> {
                try {
                    printer.zero(consumer);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            executorService.submit(() -> {
                try {
                    printer.even(consumer);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }


//
//
//        zeroThread.start();
//        oddThread.start();
//        evenThread.start();
    }
}
