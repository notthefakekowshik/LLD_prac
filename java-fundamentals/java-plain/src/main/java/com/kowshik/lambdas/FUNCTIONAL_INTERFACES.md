# Functional Interfaces in Java (SAM)

## What is a Functional Interface?
A **Functional Interface** is an interface that contains **exactly one abstract method**. They are also known as **SAM Interfaces** (Single Abstract Method).

While they can have only one abstract method, they can have:
- Multiple `default` methods.
- Multiple `static` methods.
- Methods overridden from the `Object` class (like `equals`, `toString`).

## Purpose
Functional interfaces provide the target types for **Lambda Expressions** and **Method References**. They enable functional programming patterns in Java, allowing you to treat code as data—passing behavior as parameters to methods.

## The `@FunctionalInterface` Annotation
This informative annotation (introduced in Java 8) tells the compiler to verify that the interface indeed has exactly one abstract method. If someone tries to add a second abstract method, the compiler will throw an error.

```java
@FunctionalInterface
public interface SimpleRule<T> {
    boolean evaluate(T input); // The Single Abstract Method
    
    default void log() {
        System.out.println("Executing rule...");
    }
}
```

## Core Built-in Functional Interfaces
Java provides a rich set of built-in functional interfaces in the `java.util.function` package:

| Interface | Method | Signature | Purpose |
|-----------|--------|-----------|---------|
| **Predicate<T>** | `test(T t)` | `T -> boolean` | Filters or checks a condition. |
| **Consumer<T>** | `accept(T t)` | `T -> void` | Performs an action with an input. |
| **Function<T, R>** | `apply(T t)` | `T -> R` | Transforms an input to an output. |
| **Supplier<T>** | `get()` | `() -> T` | Produces an object (Lazy generation). |
| **BiFunction<T, U, R>**| `apply(T t, U u)`| `(T, U) -> R` | Transforms two inputs into one output. |
| **UnaryOperator<T>** | `apply(T t)` | `T -> T` | Special Function where input and output types are the same. |

## Why use them?
1. **Conciseness**: Removes boilerplate code (anonymous inner classes).
2. **Readability**: Intent is clearer (e.g., `filter(isHighValue)` vs. loops and if-statements).
3. **Lazy Execution**: Suppliers allow you to delay expensive computations until needed.
4. **Composition**: Interfaces like `Predicate` and `Function` provide methods like `and()`, `or()`, and `andThen()` to build complex logic from simple blocks.
