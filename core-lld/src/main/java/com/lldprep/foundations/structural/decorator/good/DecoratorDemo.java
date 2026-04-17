package com.lldprep.foundations.structural.decorator.good;

/**
 * Decorator Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * You need to add behaviours to objects dynamically at runtime without creating a subclass
 * for every combination. Subclassing causes M×N class explosion; Decorator gives you
 * M + N classes and unlimited runtime composition.
 *
 * <p><b>How it works:</b><br>
 * - {@code Beverage} — Component interface; both base objects and decorators implement it.<br>
 * - {@code Coffee}, {@code Espresso} — Concrete Components (the real objects).<br>
 * - {@code BeverageDecorator} — Abstract Decorator; wraps a {@code Beverage} and delegates.<br>
 * - {@code MilkDecorator}, {@code SugarDecorator}, {@code VanillaDecorator} — add their cost/description,
 *   then call {@code wrapped.getCost()} / {@code wrapped.getDescription()} to chain the rest.<br>
 * - Decorators are stackable in any order and any depth.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>You need to add responsibilities to individual objects, not to a whole class.</li>
 *   <li>Inheritance is impractical because the number of combinations is large.</li>
 *   <li>Features should be composable at runtime (e.g., user builds their own order).</li>
 * </ul>
 *
 * <p><b>Real-world examples:</b><br>
 * Java I/O streams ({@code BufferedReader} wraps {@code FileReader}),
 * HTTP middleware chains, logging wrappers.
 *
 * <p><b>Decorator vs Inheritance:</b><br>
 * Inheritance is static (fixed at compile time). Decorator is dynamic (composed at runtime).
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Single decorator</li>
 *   <li>Stacked decorators (multiple add-ons)</li>
 *   <li>Double add-on (same decorator applied twice)</li>
 *   <li>Different base + same decorators</li>
 * </ol>
 */
public class DecoratorDemo {

    public static void main(String[] args) {
        demo1_SingleDecorator();
        demo2_StackedDecorators();
        demo3_DoubleAddOn();
        demo4_DifferentBase();
    }

    // -------------------------------------------------------------------------

    private static void demo1_SingleDecorator() {
        section("Demo 1: Coffee + Milk");

        Beverage order = new MilkDecorator(new Coffee());
        printOrder(order);
    }

    private static void demo2_StackedDecorators() {
        section("Demo 2: Coffee + Milk + Sugar + Vanilla (stacked)");

        Beverage order = new VanillaDecorator(
                            new SugarDecorator(
                                new MilkDecorator(
                                    new Coffee())));
        printOrder(order);
    }

    private static void demo3_DoubleAddOn() {
        section("Demo 3: Coffee + double Sugar (same decorator applied twice)");

        Beverage order = new SugarDecorator(new SugarDecorator(new Coffee()));
        printOrder(order);
    }

    private static void demo4_DifferentBase() {
        section("Demo 4: Espresso + Vanilla + Milk — same decorators, different base");

        Beverage order = new MilkDecorator(new VanillaDecorator(new Espresso()));
        printOrder(order);
    }

    // -------------------------------------------------------------------------

    private static void printOrder(Beverage beverage) {
        System.out.printf("  %-45s $%.2f%n", beverage.getDescription(), beverage.getCost());
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
