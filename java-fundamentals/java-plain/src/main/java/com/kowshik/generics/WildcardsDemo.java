package com.kowshik.generics;

/**
 * Demonstrates usage of wildcards in Java generics.
 * Shows upper bounded, lower bounded, and unbounded wildcards.
 *
 * PECS Principle:
 * - **Producer Extends** (`? extends T`): Use when the generic
 * instance only produces values of type T (you read from it).
 * - **Consumer Super** (`? super T`): Use when the instance only
 * consumes values of type T (you write to it).
 *
 * In this demo:
 * - `printNumbers` is a *producer* of Numbers, so it uses `? extends Number`.
 * - `addIntegers` is a *consumer* of Integers, so it uses `? super Integer`.
 * - `printAnything` neither produces nor consumes a specific type,
 * thus it uses the unbounded wildcard `?`.
 */
public class WildcardsDemo {

    // 1. Upper bounded wildcard – accepts List of Number or its subclasses
    public static void printNumbers(java.util.List<? extends Number> list) {
        for (Number n : list) {
            System.out.print(n + " ");
        }
        System.out.println();
    }

    // 2. Lower bounded wildcard – accepts List of Integer or any of its supertypes
    public static void addIntegers(java.util.List<? super Integer> list) {
        list.add(1);
        list.add(2);
        list.add(3);
    }

    // 3. Unbounded wildcard – can be used when the type is irrelevant
    public static void printAnything(java.util.List<?> list) {
        for (Object o : list) {
            System.out.print(o + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        java.util.List<Integer> ints = new java.util.ArrayList<>();
        addIntegers(ints); // adds 1,2,3
        System.out.print("After addIntegers: ");
        printAnything(ints);

        java.util.List<Double> doubles = java.util.List.of(1.1, 2.2, 3.3);
        System.out.print("Printing numbers (upper bounded): ");
        printNumbers(doubles);
    }
}
