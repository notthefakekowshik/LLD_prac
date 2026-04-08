package com.kowshik.generics;

/**
 * Demonstrates how generics interact with inheritance and subtyping.
 * Shows that List<Integer> is not a subtype of List<Number> and how to use
 * wildcards.
 */
public class GenericsInheritanceDemo {

    // 1. Inheritance with generics – subclass specifying concrete type
    static class Animal {
    }

    static class Dog extends Animal {
    }

    // 2. Generic class with type parameter
    static class Container<T> {
        private T value;

        public Container(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }
    }

    public static void main(String[] args) {
        // Direct generic inheritance – not allowed without wildcard
        // java.util.List<Integer> intList = new java.util.ArrayList<>();
        // java.util.List<Number> numList = intList; // Compile error

        // Using wildcard to allow subtypes
        java.util.List<? extends Number> numbers = java.util.List.of(1, 2, 3);
        System.out.println("Numbers via wildcard: " + numbers);

        // Container with Dog (subtype of Animal)
        Container<Dog> dogContainer = new Container<>(new Dog());
        // Container<Animal> animalContainer = dogContainer; // Compile error
        // Use wildcard to read as Animal
        Container<? extends Animal> animalContainer = dogContainer;
        System.out.println("Animal container holds: " + animalContainer.get().getClass().getSimpleName());
    }
}
