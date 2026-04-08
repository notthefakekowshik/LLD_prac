package com.kowshik.generics;

/**
 * Demonstrates bounded type parameters in Java generics.
 * A bounded type parameter restricts the types that can be used as arguments.
 */
public class BoundedTypeDemo {

    // 1. Upper bounded type parameter – accepts Number or its subclasses
    public static <T extends Number> double sum(T a, T b) {
        return a.doubleValue() + b.doubleValue();
    }

    // 2. Multiple bounds – class must extend Comparable and implement Runnable
    // Note: class bound must come first if present.
    public static <T extends Comparable<T> & Runnable> void runAndCompare(T a, T b) {
        // Compare using compareTo (from Comparable)
        int cmp = a.compareTo(b);
        System.out.println("Comparison result: " + cmp);
        // Run both (from Runnable)
        a.run();
        b.run();
    }

    public static void main(String[] args) {
        // Upper bound demo with Integer and Double
        System.out.println("Sum of 3 and 5: " + sum(3, 5));
        System.out.println("Sum of 2.5 and 4.1: " + sum(2.5, 4.1));

        // Multiple bounds demo using an anonymous class that implements both interfaces
        RunnableComparable rc1 = new RunnableComparable() {
            @Override
            public void run() {
                System.out.println("Running rc1");
            }

            @Override
            public int compareTo(RunnableComparable o) {
                return 0;
            }
        };
        RunnableComparable rc2 = new RunnableComparable() {
            @Override
            public void run() {
                System.out.println("Running rc2");
            }

            @Override
            public int compareTo(RunnableComparable o) {
                return 1;
            }
        };
        runAndCompare(rc1, rc2);
    }

    // Helper interface combining Comparable and Runnable for the demo
    interface RunnableComparable extends Comparable<RunnableComparable>, Runnable {
    }
}
