package com.leetcodeconcurrency.printinorder;

/**
 * Comprehensive comparison of all 4 approaches to LeetCode 1114: Print in Order.
 *
 * Demonstrates that all approaches produce correct output regardless of thread start order.
 */
public class PrintInOrderComparisonDemo {

    public static void main(String[] args) {
        System.out.println("=== LeetCode 1114: Print in Order - All Approaches ===\n");

        testApproach("CountDownLatch", new CountDownLatchTest());
        testApproach("Semaphore", new SemaphoreTest());
        testApproach("ReentrantLock + Condition", new ConditionTest());
        testApproach("Synchronized + Wait/Notify", new SynchronizedTest());

        printComparisonTable();
    }

    private static void testApproach(String name, ApproachTest test) {
        System.out.println("--- " + name + " ---");

        int[][] scenarios = {
            {1, 2, 3},
            {3, 2, 1},
            {2, 3, 1},
            {3, 1, 2}
        };

        boolean allPassed = true;
        for (int[] order : scenarios) {
            String output = test.run(order);
            boolean passed = output.equals("firstsecondthird");
            allPassed = allPassed && passed;
        }

        System.out.println("All scenarios: " + (allPassed ? "✅ PASSED" : "❌ FAILED"));
        System.out.println();
    }

    private static void printComparisonTable() {
        System.out.println("=== Approach Comparison ===\n");
        System.out.println("| Approach | Simplicity | Flexibility | Best For |");
        System.out.println("|----------|-----------|-------------|----------|");
        System.out.println("| CountDownLatch | ★★★★★ | ★★☆☆☆ | One-shot signaling, simple happens-before |");
        System.out.println("| Semaphore | ★★★★☆ | ★★★☆☆ | Resource limiting, repeated signaling |");
        System.out.println("| ReentrantLock+Condition | ★★☆☆☆ | ★★★★★ | Complex coordination, timeouts, interrupts |");
        System.out.println("| Synchronized+Wait/Notify | ★★★☆☆ | ★★☆☆☆ | Legacy code, no external dependencies |");
        System.out.println();
        System.out.println("Key Insight: CountDownLatch is optimal for this problem - it's designed");
        System.out.println("specifically for one-shot multi-thread handoffs (count down + await).");
    }

    interface ApproachTest {
        String run(int[] order);
    }

    static class CountDownLatchTest implements ApproachTest {
        public String run(int[] order) {
            PrintInOrder controller = new PrintInOrder();
            return runTest(controller, order);
        }
    }

    static class SemaphoreTest implements ApproachTest {
        public String run(int[] order) {
            PrintInOrderSemaphore controller = new PrintInOrderSemaphore();
            return runTestSemaphore(controller, order);
        }
    }

    static class ConditionTest implements ApproachTest {
        public String run(int[] order) {
            PrintInOrderCondition controller = new PrintInOrderCondition();
            return runTestCondition(controller, order);
        }
    }

    static class SynchronizedTest implements ApproachTest {
        public String run(int[] order) {
            PrintInOrderSynchronized controller = new PrintInOrderSynchronized();
            return runTestSynchronized(controller, order);
        }
    }

    private static String runTest(PrintInOrder controller, int[] order) {
        StringBuilder output = new StringBuilder();

        Thread t1 = new Thread(() -> {
            try {
                controller.first(() -> output.append("first"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                controller.second(() -> output.append("second"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                controller.third(() -> output.append("third"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runThreads(order, t1, t2, t3);
        return output.toString();
    }

    private static String runTestSemaphore(PrintInOrderSemaphore controller, int[] order) {
        StringBuilder output = new StringBuilder();

        Thread t1 = new Thread(() -> {
            try {
                controller.first(() -> output.append("first"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                controller.second(() -> output.append("second"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                controller.third(() -> output.append("third"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runThreads(order, t1, t2, t3);
        return output.toString();
    }

    private static String runTestCondition(PrintInOrderCondition controller, int[] order) {
        StringBuilder output = new StringBuilder();

        Thread t1 = new Thread(() -> {
            try {
                controller.first(() -> output.append("first"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                controller.second(() -> output.append("second"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                controller.third(() -> output.append("third"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runThreads(order, t1, t2, t3);
        return output.toString();
    }

    private static String runTestSynchronized(PrintInOrderSynchronized controller, int[] order) {
        StringBuilder output = new StringBuilder();

        Thread t1 = new Thread(() -> {
            try {
                controller.first(() -> output.append("first"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                controller.second(() -> output.append("second"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t3 = new Thread(() -> {
            try {
                controller.third(() -> output.append("third"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        runThreads(order, t1, t2, t3);
        return output.toString();
    }

    private static void runThreads(int[] order, Thread t1, Thread t2, Thread t3) {
        Thread[] threads = {t1, t2, t3};
        for (int threadNum : order) {
            threads[threadNum - 1].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
