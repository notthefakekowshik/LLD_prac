package com.kowshik.generics;

/**
 * Demonstrates how to create a generic class.
 * A generic class allows you to specify a type parameter (T) that is replaced
 * with a concrete type when an instance is created.
 */
public class GenericClassDemo {

    // 1. Defining a Generic Class with a Type Parameter T
    // T stands for "Type". This could be Integer, String, or any custom Object.
    public static class Box<T> {
        private T content;

        // Constructor accepting the type T
        public Box(T content) {
            this.content = content;
        }

        // Method returning the type T
        public T getContent() {
            return content;
        }

        // Method accepting the type T
        public void setContent(T content) {
            this.content = content;
        }
    }

    // 2. Defining a class with Multiple Type Parameters (K, V)
    // K represents Key, V represents Value
    public static class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    public static void main(String[] args) {
        // We instantiate the Box by substituting 'T' with 'String'.
        Box<String> stringBox = new Box<>("Hello Generics");
        System.out.println("String Box contains: " + stringBox.getContent());

        // We can reuse the same Box class for an Integer!
        Box<Integer> integerBox = new Box<>(100);
        System.out.println("Integer Box contains: " + integerBox.getContent());

        // Working with Multiple Type Parameters
        Pair<String, Integer> studentScore = new Pair<>("Alice", 95);
        System.out.println("Student: " + studentScore.getKey() + " -> Score: " + studentScore.getValue());
    }
}
