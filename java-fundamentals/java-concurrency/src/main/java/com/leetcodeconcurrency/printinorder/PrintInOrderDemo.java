package com.leetcodeconcurrency.printinorder;

/**
 * Demo for LeetCode 1114: Print in Order
 *
 * Demonstrates that regardless of thread start order, output is always "firstsecondthird".
 */
public class PrintInOrderDemo {

    public static void main(String[] args) {
        System.out.println("=== LeetCode 1114: Print in Order ===\n");

        testScenario("Threads started in order: first, second, third", new int[]{1, 2, 3});
        testScenario("Threads started in reverse: third, second, first", new int[]{3, 2, 1});
        testScenario("Threads started mixed: second, third, first", new int[]{2, 3, 1});
        testScenario("Threads started mixed: third, first, second", new int[]{3, 1, 2});
    }

    private static void testScenario(String description, int[] order) {
        System.out.println("Scenario: " + description);
        System.out.print("Output: ");

        PrintInOrder controller = new PrintInOrder();
        StringBuilder output = new StringBuilder();

        Thread t1 = new Thread(() -> {
            try {
                controller.first(() -> output.append("first"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "FirstThread");

        Thread t2 = new Thread(() -> {
            try {
                controller.second(() -> output.append("second"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "SecondThread");

        Thread t3 = new Thread(() -> {
            try {
                controller.third(() -> output.append("third"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ThirdThread");

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

        System.out.println(output.toString());
        boolean passed = output.toString().equals("firstsecondthird");
        System.out.println("Result: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        System.out.println();
    }
}
