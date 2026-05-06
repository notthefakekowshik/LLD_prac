package com.kowshik.lambdas.functional_interfaces;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

/**
 * FUNCTIONAL INTERFACES — INTERVIEW SCOPE
 *
 * This file covers the angles interviewers actually probe:
 *   1.  Which FI to pick — decision logic
 *   2.  andThen() vs compose() — execution order trap
 *   3.  Predicate.not() and negation
 *   4.  Chaining Predicates with and() / or()
 *   5.  Function composition chains (pipeline building)
 *   6.  Custom FI for checked exceptions (ThrowingFunction)
 *   7.  Supplier for lazy initialisation
 *   8.  Consumer.andThen() for side-effect pipelines
 *   9.  Writing your own @FunctionalInterface
 *  10.  Common gotchas
 */
public class FunctionalInterfacesInterviewDemo {

    // ─────────────────────────────────────────────────────────────
    // SECTION 1: WHICH FI TO PICK
    // The mental model: think about IN → OUT shape
    //
    //   Nothing  → T          →  Supplier<T>           () -> T
    //   T        → Nothing    →  Consumer<T>           T -> void
    //   T        → boolean    →  Predicate<T>          T -> boolean
    //   T        → R          →  Function<T,R>         T -> R
    //   T        → T          →  UnaryOperator<T>      T -> T  (special Function)
    //   (T,U)    → R          →  BiFunction<T,U,R>
    //   (T,T)    → T          →  BinaryOperator<T>
    //   (T,U)    → boolean    →  BiPredicate<T,U>
    //   (T,U)    → void       →  BiConsumer<T,U>
    // ─────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────
    // SECTION 2: andThen() vs compose() — THE CLASSIC TRAP
    //
    //   f.andThen(g)  →  g(f(x))   — f FIRST, then g   (left to right)
    //   f.compose(g)  →  f(g(x))   — g FIRST, then f   (right to left)
    //
    // Memory hook: andThen = "do this, AND THEN do that" (natural order)
    //              compose = mathematical f∘g notation (reverse order)
    // ─────────────────────────────────────────────────────────────
    static void andThenVsCompose() {
        System.out.println("--- 2. andThen() vs compose() ---");

        Function<Integer, Integer> times2  = x -> x * 2;
        Function<Integer, Integer> plus10  = x -> x + 10;

        // andThen: times2 first, THEN plus10
        // (5 * 2) + 10 = 20
        Function<Integer, Integer> times2ThenPlus10 = times2.andThen(plus10);
        System.out.println("andThen (times2 → plus10): " + times2ThenPlus10.apply(5));  // 20

        // compose: plus10 first, THEN times2
        // (5 + 10) * 2 = 30
        Function<Integer, Integer> times2AfterPlus10 = times2.compose(plus10);
        System.out.println("compose (plus10 → times2): " + times2AfterPlus10.apply(5)); // 30

        // INTERVIEW QUESTION: "What's the difference?"
        // andThen = pipe left-to-right. compose = pipe right-to-left.
        // In practice, always use andThen — it reads in execution order.
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 3: Predicate Composition — and() / or() / negate() / not()
    // ─────────────────────────────────────────────────────────────
    static void predicateComposition() {
        System.out.println("--- 3. Predicate Composition ---");

        Predicate<String> notEmpty  = s -> !s.isEmpty();
        Predicate<String> longEnough = s -> s.length() >= 5;
        Predicate<String> hasAt      = s -> s.contains("@");

        // Compose: valid email = not empty AND long enough AND has @
        Predicate<String> validEmail = notEmpty.and(longEnough).and(hasAt);

        List<String> candidates = List.of("", "hi", "hello", "user@example.com", "a@b");
        System.out.println("Valid emails:");
        candidates.stream()
                  .filter(validEmail)
                  .forEach(s -> System.out.println("  ✓ " + s));

        // Predicate.not() — Java 11+, cleaner than s -> !pred.test(s)
        Predicate<String> invalid = Predicate.not(validEmail);
        System.out.println("Invalid:");
        candidates.stream()
                  .filter(invalid)
                  .forEach(s -> System.out.println("  ✗ " + s));

        // negate() vs Predicate.not() — same result, different style
        // notEmpty.negate()        ← instance method, chains fluently
        // Predicate.not(notEmpty)  ← static, works on method references
        // e.g: .filter(Predicate.not(String::isEmpty))  ← very common in streams
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 4: Function Pipeline (real-world: request processing)
    // ─────────────────────────────────────────────────────────────
    static void functionPipeline() {
        System.out.println("--- 4. Function Pipeline ---");

        Function<String, String> trim       = String::trim;
        Function<String, String> toLower    = String::toLowerCase;
        Function<String, String> addPrefix  = s -> "user_" + s;

        // Build a pipeline left-to-right with andThen
        Function<String, String> normalise = trim.andThen(toLower).andThen(addPrefix);

        List<String> rawInputs = List.of("  Alice  ", "BOB", " Charlie ");
        System.out.println("Normalised usernames:");
        rawInputs.stream()
                 .map(normalise)
                 .forEach(s -> System.out.println("  " + s));

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 5: Consumer.andThen() — side-effect pipelines
    // Useful for logging + saving + notifying without nesting
    // ─────────────────────────────────────────────────────────────
    static void consumerChain() {
        System.out.println("--- 5. Consumer Chain ---");

        Consumer<String> log    = s -> System.out.println("  [LOG]   Processing: " + s);
        Consumer<String> save   = s -> System.out.println("  [SAVE]  Saved: " + s);
        Consumer<String> notify = s -> System.out.println("  [EMAIL] Notified for: " + s);

        Consumer<String> pipeline = log.andThen(save).andThen(notify);

        pipeline.accept("order-123");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 6: Custom @FunctionalInterface — the two real reasons
    //
    //   Reason A: checked exceptions — built-in Function can't throw them
    //   Reason B: domain clarity — Validator<T> reads better than Predicate<T>
    // ─────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;

        // Static helper: wraps a ThrowingFunction into a regular Function,
        // converting checked exception to unchecked
        static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> fn) {
            return t -> {
                try {
                    return fn.apply(t);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    @FunctionalInterface
    interface Validator<T> {
        boolean validate(T input);

        default Validator<T> and(Validator<T> other) {
            return input -> this.validate(input) && other.validate(input);
        }
    }

    static void customFunctionalInterfaces() throws Exception {
        System.out.println("--- 6. Custom FI: ThrowingFunction + Validator ---");

        // ThrowingFunction — simulate a call that might throw (e.g., DB lookup, parsing)
        ThrowingFunction<String, Integer> parse = Integer::parseInt;
        System.out.println("Parsed: " + parse.apply("42"));

        // Wrap into a stream-safe Function
        Function<String, Integer> safeParse = ThrowingFunction.wrap(Integer::parseInt);
        List<String> numbers = List.of("1", "2", "3");
        numbers.stream()
               .map(safeParse)
               .forEach(n -> System.out.println("  parsed: " + n));

        // Validator with domain clarity + custom and()
        Validator<String> notBlank  = s -> s != null && !s.isBlank();
        Validator<String> minLength = s -> s.length() >= 3;
        Validator<String> nameRule  = notBlank.and(minLength);

        System.out.println("'Al' valid?   " + nameRule.validate("Al"));      // false
        System.out.println("'Alice' valid? " + nameRule.validate("Alice"));  // true
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 7: Supplier — lazy initialisation
    // Key interview point: value is NOT computed until get() is called
    // ─────────────────────────────────────────────────────────────
    static void supplierLaziness() {
        System.out.println("--- 7. Supplier — Lazy Init ---");

        // Eager: computed immediately even if never used
        String eagerValue = expensiveComputation("eager");

        // Lazy: computation deferred until actually needed
        Supplier<String> lazyValue = () -> expensiveComputation("lazy");

        System.out.println("Supplier created — nothing computed yet.");
        System.out.println("Now calling get(): " + lazyValue.get());

        // Real pattern: Optional.orElseGet() vs orElse()
        // orElse(default)         — default is ALWAYS evaluated (even if value present)
        // orElseGet(() -> default) — default is ONLY evaluated if value absent
        Optional<String> present = Optional.of("value");

        // Both print nothing for the Optional — but orElse still calls the method
        String a = present.orElse(expensiveComputation("orElse — called regardless"));
        String b = present.orElseGet(() -> expensiveComputation("orElseGet — skipped"));

        System.out.println("orElse result:    " + a);
        System.out.println("orElseGet result: " + b);
        System.out.println();
    }

    static String expensiveComputation(String label) {
        System.out.println("  [EXPENSIVE] computing for: " + label);
        return "result-" + label;
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION 8: COMMON GOTCHAS INTERVIEWERS PROBE
    // ─────────────────────────────────────────────────────────────
    static void gotchas() {
        System.out.println("--- 8. Common Gotchas ---");

        // GOTCHA A: UnaryOperator<T> extends Function<T,T>
        // — use it when input and output are the SAME type
        UnaryOperator<String> shout = String::toUpperCase;
        Function<String, String> alsoShout = String::toUpperCase;
        // Both compile — but UnaryOperator communicates intent better
        System.out.println("UnaryOperator: " + shout.apply("hello"));

        // GOTCHA B: BinaryOperator<T> extends BiFunction<T,T,T>
        // — used heavily in Stream.reduce()
        BinaryOperator<Integer> sum = Integer::sum;
        List<Integer> nums = List.of(1, 2, 3, 4, 5);
        int total = nums.stream().reduce(0, sum);
        System.out.println("BinaryOperator reduce sum: " + total);   // 15

        // GOTCHA C: Predicate<T> vs Function<T, Boolean>
        // They look equivalent but Predicate has and()/or()/negate() — use Predicate for conditions
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Function<Integer, Boolean> isEvenFn = n -> n % 2 == 0;
        // isEvenFn.and(...)  ← COMPILE ERROR — Function has no and()
        // isEven.and(...)    ← works fine
        System.out.println("Predicate isEven(4): " + isEven.test(4));

        // GOTCHA D: lambda captures effectively-final variables only
        int multiplier = 3;   // effectively final
        Function<Integer, Integer> tripler = x -> x * multiplier;
        // multiplier = 4;   // ← uncommenting this breaks compilation
        System.out.println("Tripler(5): " + tripler.apply(5));

        // GOTCHA E: @FunctionalInterface annotation is optional but recommended
        // Without it, a second abstract method can be added silently, breaking all lambdas

        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===== FUNCTIONAL INTERFACES — INTERVIEW SCOPE =====\n");

        andThenVsCompose();
        predicateComposition();
        functionPipeline();
        consumerChain();
        customFunctionalInterfaces();
        supplierLaziness();
        gotchas();

        System.out.println("===== END =====");
    }
}
