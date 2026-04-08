package com.kowshik.lambdas.functional_design;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Modern LLD: Functional Design Patterns with Lambdas
 * 
 * Traditional LLD often uses the Strategy or Chain of Responsibility patterns 
 * with heavy class hierarchies. Modern Java (8+) allows us to use:
 * 1. Predicate<T> (Rule)
 * 2. Consumer<T> (Action)
 * 3. Function<T, R> (Transformation)
 * 4. Supplier<T> (Factory)
 */

// Java 17 record for a clean Immutable DTO
record Order(String id, double amount, String category, boolean isDiscounted) {
    // Helper to create a "modified" copy in a functional way
    public Order withDiscount(double newAmount) {
        return new Order(id, newAmount, category, true);
    }
}

public class RuleEngineDemo {

    public static void main(String[] args) {
        List<Order> orders = List.of(
            new Order("1", 150.0, "ELECTRONICS", false),
            new Order("2", 50.0, "BOOKS", false),
            new Order("3", 200.0, "BOOKS", false),
            new Order("4", 300.0, "ELECTRONICS", false)
        );

        System.out.println("=== Original Orders ===");
        orders.forEach(System.out::println);

        // 1. STRATEGY PATTERN (Functional version)
        // Instead of separate classes, we define strategies as Predicates/Functions
        Predicate<Order> isHighValue = o -> o.amount() > 100.0;
        Predicate<Order> isElectronics = o -> "ELECTRONICS".equalsIgnoreCase(o.category());

        // 2. COMPOSING RULES
        // We can combine rules using 'and', 'or', 'negate'
        Predicate<Order> applyDiscountRule = isHighValue.and(isElectronics);

        // 3. TRANSFORMATION (Functional Map)
        // A Strategy to apply a 10% discount
        Function<Order, Order> applyTenPercentDiscount = o -> 
            o.withDiscount(o.amount() * 0.9);

        // 4. CHAIN OF RESPONSIBILITY / PROCESSING PIPELINE
        // Using Streams to filter and transform based on our rules
        List<Order> processedOrders = orders.stream()
            .map(order -> {
                if (applyDiscountRule.test(order)) {
                    return applyTenPercentDiscount.apply(order);
                }
                return order;
            })
            .collect(Collectors.toList());

        System.out.println("\n=== Processed Orders (10% discount on High-Value Electronics) ===");
        processedOrders.forEach(System.out::println);

        // 5. THE "ACTION" (Consumer)
        // An action to take on specific orders (e.g., Logging/Auditing)
        Consumer<Order> auditLog = o -> System.out.println("[AUDIT] Processed order: " + o.id());
        
        System.out.println("\n=== Auditing Discounted Orders ===");
        processedOrders.stream()
            .filter(Order::isDiscounted)
            .forEach(auditLog);
            
        // 6. ADVANCED: CUSTOM RULE ENGINE CLASS
        System.out.println("\n=== Dynamic Rule Engine Execution ===");
        FunctionalRuleEngine<Order> engine = new FunctionalRuleEngine<>();
        engine.addRule(isHighValue, o -> System.out.println("  Alert: High value order found! " + o.id()));
        engine.addRule(isElectronics, o -> System.out.println("  Info: Electronics category detected."));
        
        orders.forEach(engine::process);
    }
}

/**
 * A generic, lightweight Rule Engine using functional interfaces.
 */
class FunctionalRuleEngine<T> {
    private final List<Rule<T>> rules = new ArrayList<>();

    // Internal helper to pair a Predicate with its Consumer
    private record Rule<T>(Predicate<T> condition, Consumer<T> action) {}

    public void addRule(Predicate<T> condition, Consumer<T> action) {
        rules.add(new Rule<>(condition, action));
    }

    public void process(T item) {
        for (Rule<T> rule : rules) {
            if (rule.condition().test(item)) {
                rule.action().accept(item);
            }
        }
    }
}
