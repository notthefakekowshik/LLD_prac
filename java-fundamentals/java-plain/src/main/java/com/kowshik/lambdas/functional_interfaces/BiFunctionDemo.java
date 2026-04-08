package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Bi-variants of core functional interfaces — for operations that take TWO inputs.
 *
 * BiFunction<T, U, R>  : (T, U) -> R   — transform two inputs into one output
 * BiPredicate<T, U>    : (T, U) -> boolean — test a condition on two inputs
 * BiConsumer<T, U>     : (T, U) -> void — perform a side-effect on two inputs
 *
 * Note: There is no BiSupplier — a Supplier produces, it needs no input.
 */
public class BiFunctionDemo {

    record Product(String name, double price) {}

    public static void main(String[] args) {

        List<Product> cart = List.of(
                new Product("Laptop", 1200.0),
                new Product("Mouse", 25.0),
                new Product("Keyboard", 75.0)
        );

        // --- BiFunction<T, U, R> ---
        // Takes two inputs, returns one output.
        // Here: apply a percentage discount to a price.
        BiFunction<Double, Double, Double> applyDiscount = (price, discountPct) ->
                price - (price * discountPct / 100);

        System.out.println("=== BiFunction: applyDiscount ===");
        List<Product> discounted = cart.stream()
                .map(p -> new Product(p.name(), applyDiscount.apply(p.price(), 10.0)))
                .collect(Collectors.toList());
        discounted.forEach(System.out::println);

        // BiFunction chaining with andThen
        // andThen appends a Function<R, V> after the BiFunction
        BiFunction<Double, Double, String> discountLabel =
                applyDiscount.andThen(result -> String.format("Final price: $%.2f", result));
        System.out.println("\nBiFunction.andThen -> " + discountLabel.apply(1200.0, 15.0));

        // --- BiPredicate<T, U> ---
        // Returns boolean based on two inputs.
        BiPredicate<Product, Double> isAffordable = (product, budget) ->
                product.price() <= budget;

        System.out.println("\n=== BiPredicate: isAffordable (budget=$100) ===");
        double budget = 100.0;
        cart.forEach(p ->
                System.out.println(p.name() + " affordable: " + isAffordable.test(p, budget))
        );

        // BiPredicate composition
        BiPredicate<Product, Double> isExpensive = isAffordable.negate();
        BiPredicate<Product, Double> isHighEndLaptop = isExpensive
                .and((p, b) -> p.name().equalsIgnoreCase("Laptop"));

        System.out.println("Is Laptop high-end? " + isHighEndLaptop.test(new Product("Laptop", 1200.0), 100.0));

        // --- BiConsumer<T, U> ---
        // Performs a side-effect using two inputs, returns nothing.
        BiConsumer<String, Double> printLineItem = (name, price) ->
                System.out.printf("  %-12s $%.2f%n", name, price);

        System.out.println("\n=== BiConsumer: printLineItem ===");
        cart.forEach(p -> printLineItem.accept(p.name(), p.price()));

        // BiConsumer chaining with andThen
        BiConsumer<String, Double> auditLog = (name, price) ->
                System.out.println("[AUDIT] Item logged: " + name);

        BiConsumer<String, Double> processItem = printLineItem.andThen(auditLog);
        System.out.println("\nBiConsumer.andThen (print + audit):");
        processItem.accept("Monitor", 300.0);
    }
}
