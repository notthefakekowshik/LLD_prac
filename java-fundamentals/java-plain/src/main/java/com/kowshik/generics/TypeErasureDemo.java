package com.kowshik.generics;

/**
 * Demonstrates type erasure in Java generics.
 * Shows that generic type information is removed at runtime.
 */
public class TypeErasureDemo {

    // Generic class – type parameter T is erased to Object at runtime
    static class Box<T> {
        private T value;

        public Box(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }
    }

    public static void main(String[] args) {
        Box<Integer> intBox = new Box<>(123);
        Box<String> strBox = new Box<>("abc");

        // At runtime both boxes are just Box objects; type info is erased
        System.out.println("intBox class: " + intBox.getClass());
        System.out.println("strBox class: " + strBox.getClass());
        System.out.println("Same class? " + (intBox.getClass() == strBox.getClass()));

        // Demonstrate that casts are inserted by compiler
        Object obj = intBox; // raw type assignment
        // Need cast to retrieve value
        Integer i = (Integer) ((Box) obj).get();
        System.out.println("Retrieved integer: " + i);
    }
}
