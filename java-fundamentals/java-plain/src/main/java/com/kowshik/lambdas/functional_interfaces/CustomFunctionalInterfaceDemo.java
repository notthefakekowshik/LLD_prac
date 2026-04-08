package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Custom Functional Interfaces
 *
 * While java.util.function covers most cases, you define your own when:
 * 1. The built-in signatures don't fit (e.g., checked exceptions, 3+ params).
 * 2. You want a domain-specific name that makes intent clearer (e.g., Validator vs Predicate).
 * 3. You need extra default/static methods for composition specific to your domain.
 *
 * @FunctionalInterface is optional but recommended — it makes the compiler enforce the SAM rule.
 */
public class CustomFunctionalInterfaceDemo {

    // --- Example 1: Domain-specific name for clarity ---
    // A Predicate<T> would work, but "Validator" reads better in business code.
    @FunctionalInterface
    interface Validator<T> {
        boolean validate(T input);

        // default methods for composition — just like Predicate's and/or
        default Validator<T> and(Validator<T> other) {
            return input -> this.validate(input) && other.validate(input);
        }

        default Validator<T> or(Validator<T> other) {
            return input -> this.validate(input) || other.validate(input);
        }

        default Validator<T> negate() {
            return input -> !this.validate(input);
        }
    }

    // --- Example 2: Checked exception — built-in Function can't throw checked exceptions ---
    // This is one of the most common reasons to write a custom functional interface.
    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T input) throws Exception;

        // Wrap into a regular Function, converting checked -> unchecked
        static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> fn) {
            return input -> {
                try {
                    return fn.apply(input);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    // --- Example 3: More than 2 inputs (no built-in TriFunction) ---
    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    // --- Domain model ---
    record Order(String id, double amount, String category) {}

    public static void main(String[] args) {

        // Using Validator<Order> — reads like a business rule
        Validator<Order> isHighValue   = o -> o.amount() > 100.0;
        Validator<Order> isElectronics = o -> "ELECTRONICS".equalsIgnoreCase(o.category());
        Validator<Order> qualifiesForDiscount = isHighValue.and(isElectronics);

        List<Order> orders = List.of(
                new Order("1", 150.0, "ELECTRONICS"),
                new Order("2", 50.0,  "BOOKS"),
                new Order("3", 200.0, "ELECTRONICS")
        );

        System.out.println("=== Validator: qualifiesForDiscount ===");
        orders.forEach(o ->
                System.out.println(o.id() + " -> " + qualifiesForDiscount.validate(o))
        );

        // Using ThrowingFunction (wraps checked exception)
        System.out.println("\n=== ThrowingFunction: parse doubles safely ===");
        List<String> rawValues = List.of("10.5", "20.0", "30.99");
        List<Double> parsed = rawValues.stream()
                .map(ThrowingFunction.wrap(Double::parseDouble))
                .collect(Collectors.toList());
        System.out.println(parsed);

        // Using TriFunction (3 input params)
        System.out.println("\n=== TriFunction: format order summary ===");
        TriFunction<String, Double, String, String> formatSummary =
                (id, amount, category) ->
                        String.format("Order[%s] | %s | $%.2f", id, category, amount);

        orders.forEach(o ->
                System.out.println(formatSummary.apply(o.id(), o.amount(), o.category()))
        );
    }
}
