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

| Interface | Method | Signature | Purpose | Demo |
|-----------|--------|-----------|---------|------|
| **Predicate<T>** | `test(T t)` | `T -> boolean` | Filters or checks a condition. | `PredicateDemo.java` |
| **Consumer<T>** | `accept(T t)` | `T -> void` | Performs an action with an input. | `ConsumerDemo.java` |
| **Function<T, R>** | `apply(T t)` | `T -> R` | Transforms an input to an output. | `FunctionDemo.java` |
| **Supplier<T>** | `get()` | `() -> T` | Produces an object (Lazy generation). | `SupplierDemo.java` |
| **BiFunction<T, U, R>** | `apply(T t, U u)` | `(T, U) -> R` | Transforms two inputs into one output. | `BiFunctionDemo.java` |
| **BiPredicate<T, U>** | `test(T t, U u)` | `(T, U) -> boolean` | Tests a condition on two inputs. | `BiFunctionDemo.java` |
| **BiConsumer<T, U>** | `accept(T t, U u)` | `(T, U) -> void` | Performs a side-effect on two inputs. | `BiFunctionDemo.java` |
| **UnaryOperator<T>** | `apply(T t)` | `T -> T` | `Function<T,T>`: same type in and out. | `OperatorDemo.java` |
| **BinaryOperator<T>** | `apply(T t, T u)` | `(T, T) -> T` | `BiFunction<T,T,T>`: merges two values of the same type. | `OperatorDemo.java` |

## Method References (`::`)
A compact syntax for lambdas that only call an existing method. See `MethodReferenceDemo.java`.

| Kind | Syntax | Equivalent Lambda |
|------|--------|-------------------|
| Static | `ClassName::staticMethod` | `x -> ClassName.staticMethod(x)` |
| Bound instance | `obj::method` | `x -> obj.method(x)` |
| Unbound instance | `ClassName::instanceMethod` | `x -> x.method()` |
| Constructor | `ClassName::new` | `x -> new ClassName(x)` |

## Variable Capture (Closures)
Lambdas can capture variables from their enclosing scope, but only if those variables are **effectively final** (never reassigned). See `VariableCaptureDemo.java`.

- **Local variables / parameters** — must be effectively final.
- **Instance fields** — no restriction (accessed via `this`, which is always final).
- **Static fields** — no restriction.
- **Workaround for counters** — use a single-element array `int[] count = {0}` or `AtomicInteger`.

## Custom Functional Interfaces
Define your own when built-ins don't fit. See `CustomFunctionalInterfaceDemo.java`.

Common reasons:
1. **Domain clarity** — `Validator<T>` reads better than `Predicate<T>` in business code.
2. **Checked exceptions** — built-in `Function` cannot throw checked exceptions; a custom `ThrowingFunction` wraps them.
3. **3+ parameters** — no built-in `TriFunction`; define your own.

## Why use them?
1. **Conciseness**: Removes boilerplate code (anonymous inner classes).
2. **Readability**: Intent is clearer (e.g., `filter(isHighValue)` vs. loops and if-statements).
3. **Lazy Execution**: Suppliers allow you to delay expensive computations until needed.
4. **Composition**: Interfaces like `Predicate` and `Function` provide methods like `and()`, `or()`, and `andThen()` to build complex logic from simple blocks.
