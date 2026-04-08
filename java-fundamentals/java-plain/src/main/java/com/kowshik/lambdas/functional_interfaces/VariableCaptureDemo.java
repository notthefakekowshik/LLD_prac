package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Variable Capture in Lambdas (Closures)
 *
 * A lambda can capture variables from its enclosing scope, but ONLY if they are
 * effectively final — meaning they are never reassigned after their initial assignment.
 * (The `final` keyword is optional; the compiler infers it.)
 *
 * Why this rule?
 * Lambdas may outlive the scope they were defined in (e.g., passed to another thread).
 * Allowing mutable captured variables would cause race conditions and unpredictable state.
 *
 * Things a lambda CAN capture:
 *   - Local variables (effectively final)
 *   - Method parameters (effectively final)
 *   - Instance fields (via 'this' — no restriction, because 'this' itself is final)
 *   - Static fields (no restriction)
 */
public class VariableCaptureDemo {

    private String instanceField = "I am an instance field";
    private static String staticField = "I am a static field";

    public void demonstrate() {

        // --- 1. Capturing a local variable (effectively final) ---
        String greeting = "Hello";  // never reassigned -> effectively final
        Function<String, String> greeter = name -> greeting + ", " + name + "!";
        System.out.println("1. Captured local var -> " + greeter.apply("Kowshik"));

        // Uncommenting this would cause a compile error: "Variable used in lambda should be final or effectively final"
        // greeting = "Hi";

        // --- 2. Capturing a method parameter (effectively final) ---
        captureParameter(10);

        // --- 3. Capturing an instance field (no restriction) ---
        // Instance fields are accessed via 'this', which IS effectively final.
        // Lambdas can both read AND modify instance fields.
        Runnable modifyInstance = () -> {
            instanceField = "Modified by lambda";  // perfectly legal
            System.out.println("3. Instance field -> " + instanceField);
        };
        modifyInstance.run();

        // --- 4. Capturing a static field (no restriction) ---
        Runnable readStatic = () -> System.out.println("4. Static field -> " + staticField);
        readStatic.run();

        // --- 5. Practical pattern: building parameterized predicates ---
        // A common interview example: using a captured variable to create
        // a "configured" predicate at runtime.
        int threshold = 100;  // effectively final
        Predicate<Integer> isAboveThreshold = value -> value > threshold;

        List<Integer> prices = List.of(50, 120, 80, 200, 99, 101);
        List<Integer> expensive = prices.stream()
                .filter(isAboveThreshold)
                .collect(Collectors.toList());
        System.out.println("5. Prices above threshold (" + threshold + "): " + expensive);

        // --- 6. The "workaround" for mutating a value inside a lambda ---
        // Since primitives/Strings can't be mutated via capture, use a single-element array
        // or an AtomicInteger/AtomicReference for counters inside lambdas.
        int[] counter = {0};  // array reference is effectively final; its contents are not
        prices.forEach(p -> counter[0] += p);
        System.out.println("6. Sum via array counter -> " + counter[0]);
    }

    private void captureParameter(int multiplier) {
        // 'multiplier' is a method parameter — effectively final since it's never reassigned.
        Function<Integer, Integer> multiply = n -> n * multiplier;
        System.out.println("2. Captured method param (x5) -> " + multiply.apply(5));
    }

    public static void main(String[] args) {
        new VariableCaptureDemo().demonstrate();
    }
}
