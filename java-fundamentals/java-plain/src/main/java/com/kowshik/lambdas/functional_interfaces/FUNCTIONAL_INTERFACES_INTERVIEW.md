# Functional Interfaces — Interview Theory

---

## 0. The Foundation — What and Why

### The problem Java had before Java 8

Java has always been object-oriented — everything is a class or an interface. But sometimes
you just want to **pass a small piece of behaviour** to a method. Before Java 8 the only way
to do that was an **anonymous inner class**:

```java
// Sort a list — pre Java 8
Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.length() - b.length();
    }
});
```

That's 5 lines to express one idea: *compare by length*. The ceremony hides the intent.

---

### What Java 8 introduced: lambdas

A **lambda** is a shorthand for an anonymous inner class that has exactly one method:

```java
// Same sort — Java 8+
Collections.sort(names, (a, b) -> a.length() - b.length());
```

One line. The behaviour is the value. But the Java type system still needs a **type** for that
lambda. You can't just have floating code — the compiler needs to know what interface it satisfies.

---

### That type is a Functional Interface

A **Functional Interface (FI)** is any interface with **exactly one abstract method (SAM —
Single Abstract Method)**. A lambda is just syntactic sugar for implementing that one method.

```java
@FunctionalInterface
interface Comparator<T> {
    int compare(T a, T b);   // ← the SAM. The lambda fills this in.
}
```

The `@FunctionalInterface` annotation is **optional** — it's a compiler guard that prevents
someone from accidentally adding a second abstract method and silently breaking all lambdas
that target it.

---

### The key insight: behaviour as a value

Before Java 8, to pass behaviour you passed an *object* (anonymous inner class).
With lambdas + FIs, you pass *behaviour directly* — the FI is just the type wrapper the
compiler needs.

```java
// Three equivalent ways to express the same behaviour:

// 1. Anonymous inner class (pre Java 8)
Runnable r1 = new Runnable() {
    public void run() { System.out.println("hello"); }
};

// 2. Lambda (Java 8+)
Runnable r2 = () -> System.out.println("hello");

// 3. Method reference (Java 8+, when a method already does the job)
Runnable r3 = FunctionalInterfacesInterviewDemo::someStaticMethod;
```

`Runnable` is a functional interface. It has one abstract method: `run()`.
The lambda `() -> System.out.println("hello")` is the implementation of `run()`.

---

### Why the JDK ships 9+ built-in FIs

Writing your own FI for every use case would be repetitive. The JDK ships a family of
**generic, reusable FIs** in `java.util.function` that cover the most common shapes:
*produce something, consume something, test something, transform something*.

You only need a custom FI when none of the built-ins fit (checked exceptions, 3+ params,
or domain-specific naming).

---

### How it all connects

```
Lambda expression
    └── is an implementation of a Functional Interface (SAM)
            └── the built-in ones live in java.util.function
                    └── picked by the SHAPE of the lambda (in → out)
                            └── can be composed with andThen / and / or etc.
```

---

## 1. The Mental Model — Pick the Right FI in 5 Seconds

Think only about the **shape**: what goes in, what comes out.

```
Nothing  →  T          Supplier<T>          get()
T        →  void       Consumer<T>          accept(T)
T        →  boolean    Predicate<T>         test(T)
T        →  R          Function<T,R>        apply(T)
T        →  T          UnaryOperator<T>     apply(T)      ← special Function where T=R
(T,U)    →  R          BiFunction<T,U,R>    apply(T,U)
(T,T)    →  T          BinaryOperator<T>    apply(T,T)    ← special BiFunction where T=U=R
(T,U)    →  boolean    BiPredicate<T,U>     test(T,U)
(T,U)    →  void       BiConsumer<T,U>      accept(T,U)
```

**Interview tip**: If asked "which FI would you use for X?", draw the arrow mentally first, then name it.

---

## 2. Composition Methods — What Each FI Supports

| FI | Composition methods |
|----|-------------------|
| `Function<T,R>` | `andThen(after)`, `compose(before)` |
| `Predicate<T>` | `and(other)`, `or(other)`, `negate()`, `Predicate.not(p)` (static, Java 11+) |
| `Consumer<T>` | `andThen(after)` |
| `Supplier<T>` | ❌ none — it produces, doesn't chain |

### andThen vs compose — THE classic trap

```
f.andThen(g)  →  g(f(x))    f runs FIRST  (left → right, natural reading order)
f.compose(g)  →  f(g(x))    g runs FIRST  (right → left, mathematical notation)
```

```java
Function<Integer, Integer> times2 = x -> x * 2;
Function<Integer, Integer> plus10 = x -> x + 10;

times2.andThen(plus10).apply(5)  // (5*2)+10 = 20   ← times2 first
times2.compose(plus10).apply(5)  // (5+10)*2 = 30   ← plus10 first
```

**Rule of thumb**: Always use `andThen` — it reads in execution order. `compose` exists for
mathematical convention; you rarely need it in production code.

---

## 3. Predicate.not() — Why It Exists (Java 11+)

Before Java 11, negating a method reference was awkward:

```java
// Before Java 11 — ugly cast required
list.stream().filter(s -> !s.isEmpty())         // works but verbose
list.stream().filter(((Predicate<String>) String::isEmpty).negate())  // terrible

// Java 11+ — clean
list.stream().filter(Predicate.not(String::isEmpty))
```

`Predicate.not()` is a static factory that wraps a method reference in a negated Predicate.
Instance `.negate()` still exists — use it when you already have a Predicate variable.

---

## 4. Predicate vs Function\<T, Boolean\> — They Are NOT the Same

```java
Predicate<Integer>        isEven   = n -> n % 2 == 0;
Function<Integer, Boolean> isEvenFn = n -> n % 2 == 0;
```

They compile identically but:
- `Predicate` has `and()`, `or()`, `negate()` → composable
- `Function<T, Boolean>` has only `andThen()` / `compose()` → not useful for boolean logic
- Stream's `.filter()` takes `Predicate<T>`, not `Function<T, Boolean>`

**Always use `Predicate` for boolean-returning logic.**

---

## 5. UnaryOperator and BinaryOperator — When to Use Them

They are specialisations, not new interfaces:

```java
UnaryOperator<T>  extends  Function<T, T>        // use when input and output are same type
BinaryOperator<T> extends  BiFunction<T, T, T>   // use when both inputs AND output are same type
```

Real uses:
```java
// UnaryOperator — transforming a value to the same type
UnaryOperator<String> trim = String::trim;
list.replaceAll(trim);   // List.replaceAll takes UnaryOperator<E>

// BinaryOperator — reducing/merging same-type values
BinaryOperator<Integer> sum = Integer::sum;
list.stream().reduce(0, sum);   // Stream.reduce takes BinaryOperator<T>
```

**Interview tip**: `List.replaceAll()` takes `UnaryOperator` and `Stream.reduce()` takes `BinaryOperator` —
know these two usages.

---

## 6. Supplier — Lazy Evaluation (the orElse trap)

`Supplier` delays computation until `.get()` is called. This is critical with `Optional`:

```java
Optional<String> present = Optional.of("value");

// orElse — default expression is ALWAYS evaluated, even when value is present
String a = present.orElse(expensiveCall());        // expensiveCall() runs regardless

// orElseGet — default is ONLY evaluated if value is absent
String b = present.orElseGet(() -> expensiveCall()); // skipped because value exists
```

**Always prefer `orElseGet` when the fallback involves any computation or I/O.**

Other places Supplier is useful:
- Factory methods / DI containers (lazy bean creation)
- Logging: `logger.debug(() -> "msg: " + buildMsg())` — message not built if DEBUG disabled
- Memoisation: compute once, cache in a field

---

## 7. Custom @FunctionalInterface — Two Real Reasons

### Reason A: Checked Exceptions

Built-in `Function<T,R>` cannot throw checked exceptions. Wrap it:

```java
@FunctionalInterface
interface ThrowingFunction<T, R> {
    R apply(T t) throws Exception;

    static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> fn) {
        return t -> {
            try { return fn.apply(t); }
            catch (Exception e) { throw new RuntimeException(e); }
        };
    }
}

// Usage in a stream — Integer::parseInt throws NumberFormatException (unchecked here)
// but for truly checked exceptions (IOException etc.) this wrapper is essential
list.stream().map(ThrowingFunction.wrap(Files::readString)).collect(...);
```

### Reason B: Domain Clarity

`Validator<T>` communicates intent far better than `Predicate<T>` in business code:

```java
@FunctionalInterface
interface Validator<T> {
    boolean validate(T input);

    default Validator<T> and(Validator<T> other) {
        return input -> this.validate(input) && other.validate(input);
    }
}

Validator<Order> hasItems    = o -> !o.items().isEmpty();
Validator<Order> hasAddress  = o -> o.address() != null;
Validator<Order> orderValid  = hasItems.and(hasAddress);
```

---

## 8. Variable Capture Rules (Closure Semantics)

```java
int x = 10;            // effectively final — OK to capture
x = 20;                // now x is NOT effectively final — lambda below would fail

Function<Integer, Integer> fn = n -> n + x;   // compile error if x was reassigned
```

| What is captured | Rule |
|---|---|
| Local variable / method param | Must be **effectively final** (never reassigned) |
| Instance field (`this.field`) | No restriction — `this` ref is effectively final |
| Static field | No restriction |

**Workaround for mutable counters in lambdas:**
```java
int[] count = {0};         // single-element array — the array ref is final, contents mutable
list.forEach(e -> count[0]++);

// OR
AtomicInteger count = new AtomicInteger();
list.forEach(e -> count.incrementAndGet());
```

---

## 9. Anonymous Inner Class vs Lambda — Key Differences

| | Anonymous Inner Class | Lambda |
|---|---|---|
| `this` keyword | Refers to the anonymous class instance | Refers to the **enclosing class** |
| Can have state (fields) | ✅ Yes | ❌ No |
| Serializable | Conditionally | Conditionally |
| When to use | Need state, multiple methods, or `this` to mean inner class | All other cases |

**The `this` difference is an interview favourite:**
```java
class Outer {
    void demo() {
        Runnable anon = new Runnable() {
            public void run() { System.out.println(this); }  // prints the anonymous class
        };
        Runnable lambda = () -> System.out.println(this);    // prints the Outer instance
    }
}
```

---

## 10. Quick-Fire Interview Q&A

**Q: Can a functional interface have default methods?**
Yes — as many as needed. Only the count of *abstract* methods must be exactly one.

**Q: Can a functional interface extend another interface?**
Yes, as long as the total abstract method count remains one. It can inherit the SAM from a parent.

**Q: Is `Comparator<T>` a functional interface?**
Yes — it has exactly one abstract method `compare(T, T)`. `equals()` doesn't count (it's from Object).
That's why you can write `Comparator<String> c = (a, b) -> a.length() - b.length()`.

**Q: Why can't built-in FIs throw checked exceptions?**
Because the JDK designers chose not to — adding `throws Exception` would force every caller
to handle it even when the lambda doesn't throw. Use a custom `ThrowingFunction` wrapper.

**Q: What is the difference between `Predicate.not(p)` and `p.negate()`?**
Same result. `Predicate.not()` is static so it works directly with method references
(e.g., `Predicate.not(String::isEmpty)`). `.negate()` is an instance method — needs a variable.

**Q: When would you write your own FI instead of using a built-in?**
Three cases: (1) checked exceptions, (2) 3+ parameters, (3) domain-specific naming for clarity.
