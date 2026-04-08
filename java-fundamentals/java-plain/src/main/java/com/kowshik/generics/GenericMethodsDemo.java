package com.kowshik.generics;

/**
 * Demonstrates generic methods in Java.
 * A generic method can declare its own type parameters independent of the
 * class's type parameters.
 */
public class GenericMethodsDemo {

    // 1. Simple generic method that returns the same object it receives
    public static <T> T identity(T value) {
        return value;
    }

    // 2. Generic method with multiple type parameters
    public static <K, V> boolean comparePairs(K key1, V value1, K key2, V value2) {
        return key1.equals(key2) && value1.equals(value2);
    }

    // 3. Generic method that works on arrays
    public static <T> void printArray(T[] array) {
        for (T element : array) {
            System.out.print(element + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        // Identity demo
        String str = identity("Hello Generic Method");
        System.out.println("Identity returned: " + str);

        // Compare pairs demo
        boolean same = comparePairs("key", 42, "key", 42);
        System.out.println("Pairs are same: " + same);

        // Print array demo
        Integer[] numbers = { 1, 2, 3, 4, 5 };
        printArray(numbers);
        String[] words = { "one", "two", "three" };
        printArray(words);
    }
}
