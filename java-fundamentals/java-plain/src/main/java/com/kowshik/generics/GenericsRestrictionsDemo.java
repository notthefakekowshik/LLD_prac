package com.kowshik.generics;

/**
 * Demonstrates various restrictions on Java generics.
 * Shows what is not allowed with generic type parameters.
 */
public class GenericsRestrictionsDemo {

    // 1. Cannot instantiate a generic type with a primitive
    // This would be a compile error: Box<int> intBox = new Box<>(5);
    // Instead we must use the wrapper type Integer.

    // 2. Cannot create an array of a generic type
    // The following is illegal: List<String>[] array = new List<String>[10];
    // Workaround: use List<?>[] or cast with @SuppressWarnings.

    // 3. Cannot use instanceof with a parameterized type
    // if (obj instanceof List<String>) { ... } // compile error

    // 4. Cannot declare static fields of a generic type parameter
    // class Example<T> { static T value; } // illegal because static belongs to
    // class, not instance.

    // 5. Cannot create a generic exception class
    // class MyException<T> extends Exception { } // illegal, generic exceptions are
    // not allowed.

    // 6. Cannot overload methods where erasure makes signatures identical
    // void method(Set<String> s) {} // and void method(Set<Integer> s) {} //
    // compile error.

    public static void main(String[] args) {
        System.out.println("Generics restrictions demo – compile-time only, no runtime behavior.");
    }
}
