# Java Generics Theory

Generics were introduced in Java 5 to provide **stronger type checks at compile time** and to support **generic programming**. They allow you to define classes, interfaces, and methods where the type of data they operate on is specified as a parameter.

## Why Use Generics?

1. **Stronger type checks at compile time**: A Java compiler applies strong type checking to generic code and issues errors if the code violates type safety. Fixing compile-time errors is easier than fixing runtime errors.
2. **Elimination of casts**: Before generics, every object retrieved from a collection had to be cast to its specific type. With generics, the compiler knows the type and can automatically cast it.
3. **Enabling programmers to implement generic algorithms**: By using generics, programmers can implement generic algorithms that work on collections of different types, can be customized, and are type-safe and easier to read.

---

## Generic Types: Classes and Interfaces

A generic type is a generic class or interface that is parameterized over types. It uses angle brackets `< >` to specify type parameters.

```java
// Generic Class Example
public class Box<T> {
    private T t;

    public void set(T t) { this.t = t; }
    public T get() { return t; }
}

// Usage
Box<Integer> integerBox = new Box<>();
integerBox.set(10);
// integerBox.set("String"); // Compile-time error
```

### Type Parameter Naming Conventions

- `E` - Element (used extensively by the Java Collections Framework)
- `K` - Key
- `N` - Number
- `T` - Type
- `V` - Value
- `S`, `U`, `V` etc. - 2nd, 3rd, 4th types

---

## Generic Methods

Generic methods are methods that introduce their own type parameters. This is similar to declaring a generic type, but the type parameter's scope is limited to the method where it is declared. Static and non-static generic methods are allowed, as well as generic class constructors.

```java
public class Util {
    public static <K, V> boolean compare(Pair<K, V> p1, Pair<K, V> p2) {
        return p1.getKey().equals(p2.getKey()) &&
               p1.getValue().equals(p2.getValue());
    }
}
```

---

## Bounded Type Parameters

Sometimes you want to restrict the types that can be used as type arguments in a parameterized type. For example, a method that operates on numbers might only want to accept instances of `Number` or its subclasses.

To declare a bounded type parameter, list the type parameter's name, followed by the `extends` keyword, followed by its upper bound.

```java
public <T extends Number> void inspect(T t) {
    System.out.println("T is a subtype of Number");
}
```

You can specify multiple bounds using the `&` symbol. If one of the bounds is a class, it must be specified first.

```java
class A { /* ... */ }
interface B { /* ... */ }
interface C { /* ... */ }

// Multiple bounds
class D <T extends A & B & C> { /* ... */ }
```

---

## Generics, Inheritance, and Subtypes

As you know, you can assign an object of one type to an object of another type provided that the types are compatible (like `Object someObject = new String("Foo")`).
However, this is not the case for generic types.

```java
public void someMethod(Number n) { /* ... */ }

someMethod(new Integer(10)); // OK
someMethod(new Double(10.1)); // OK

public void boxTest(Box<Number> n) { /* ... */ }

boxTest(new Box<Integer>()); // Compile-time error! Box<Integer> is NOT a subtype of Box<Number>
```

Given two concrete types `A` and `B` (for example, `Number` and `Integer`), `MyClass<A>` has no relationship to `MyClass<B>`, regardless of whether or not `A` and `B` are related. The common parent of `MyClass<A>` and `MyClass<B>` is `Object`.

---

## Wildcards

In generic code, the question mark (`?`), called the wildcard, represents an unknown type.

### 1. Upper Bounded Wildcards (`<? extends T>`)

Used when you want to relax the restrictions on a variable. You can use an upper bounded wildcard to define a variable that accepts `T` or *any of its subclasses*.

```java
public static void process(List<? extends Number> list) {
    for (Number elem : list) {
        // ...
    }
}
```

**Important:** You generally cannot *add* to a collection with an upper bounded wildcard because the compiler doesn't know the concrete type being added.

### 2. Unbounded Wildcards (`<?>`)

Useful when you are writing a method that can be implemented using functionality provided in the `Object` class, or when the code is using methods in the generic class that don't depend on the type parameter (like `List.size` or `List.clear`).

```java
public static void printList(List<?> list) {
    for (Object elem: list)
        System.out.print(elem + " ");
}
```

### 3. Lower Bounded Wildcards (`<? super T>`)

A lower bounded wildcard restricts the unknown type to be a specific type or a *supertype* of that type. This is useful when you want to write to a collection.

```java
public static void addNumbers(List<? super Integer> list) {
    for (int i = 1; i <= 10; i++) {
        list.add(i); // Safe to add Integer or any of its subtypes
    }
}
```

### Wildcard Guidelines: The PECS Principle

**PECS** stands for **P**roducer **E**xtends, **C**onsumer **S**uper.

- Use `? extends T` when a collection is a **producer** of values (you want to read from the collection).
- Use `? super T` when a collection is a **consumer** of values (you want to write to the collection).
- If you need both read and write access without wildcards constraints, avoid wildcards and use the exact type `T`.
- ? extends T: Use for Read-only access. You know the "ceiling" is T.
- ? super T: Use for Write-only access. You know the "floor" is T

---

## Type Erasure

Generics were introduced to Java to provide tighter type checks at compile time. To implement generics, the Java compiler applies type erasure to:

1. Replace all type parameters in generic types with their bounds or `Object` if the type parameters are unbounded. The produced bytecode, therefore, contains only ordinary classes, interfaces, and methods.
2. Insert type casts if necessary to preserve type safety.
3. Generate bridge methods to preserve polymorphism in extended generic types.

Type erasure ensures that no new classes are created for parameterized types; consequently, generics incur no runtime overhead.

**Example of Erasure:**

```java
// At Compile Time
public class Node<T> {
    private T data;
    private Node<T> next;
    // ...
}

// At Runtime (After Type Erasure)
public class Node {
    private Object data;
    private Node next;
    // ...
}
```

---

## Restrictions on Generics

There are several restrictions on what you can do with generics:

1. **Cannot Instantiate Generic Types with Primitive Types**:
   `Box<int> box = new Box<>(); // Error! Use Box<Integer>`
2. **Cannot Create Instances of Type Parameters**:
   `public <E> void append(List<E> list) { E elem = new E(); // Error! }`
3. **Cannot Declare Static Fields Whose Types are Type Parameters**:
   `public class MobileDevice<T> { private static T os; // Error! }`
   Because a static variable is shared among all instances, the type would be ambiguous.
4. **Cannot Use Casts or `instanceof` With Parameterized Types**:
   `if (obj instanceof ArrayList<Integer>) { ... } // Error! Type is erased at runtime.`
5. **Cannot Create Arrays of Parameterized Types**:
   `List<Integer>[] arrayOfLists = new List<Integer>[2]; // Error!`
6. **Exceptions Restrictions**:
   - Cannot create, catch, or throw objects of parameterized types.
   - You cannot declare a generic class that extends `Throwable`.
   - Cannot catch an instance of a type parameter.
7. **Cannot Overload a Method Where the Formal Parameter Types of Each Overload Erase to the Same Raw Type**:

   ```java
   public void print(Set<String> strSet) { }
   public void print(Set<Integer> intSet) { } // Compile-time error due to erasure to print(Set)
   ```
