package com.kowshik.patternmatching;

import java.util.List;

/**
 * SEALED CLASSES — Java 17 (stable)
 *
 * PROBLEM they solve:
 *   Interfaces and abstract classes are open by default — any class anywhere can implement/extend them.
 *   This is fine for extension points (plugins, frameworks), but it is wrong for closed domain models
 *   like "a shape is exactly one of Circle, Rectangle, Triangle — nothing else".
 *
 *   Open hierarchy = switch/if-else over subtypes MUST have a default case, because
 *   the compiler cannot verify you've handled everything. Miss a case = silent bug.
 *
 * SOLUTION:
 *   sealed interface/class + permits clause → compiler knows the complete set of subtypes.
 *   Every permitted subtype must be final, sealed, or non-sealed.
 *   Switch over a sealed type with all cases covered → NO default needed → compile error if you miss one.
 *
 * THREE KEYWORDS:
 *   sealed    — this type's subtypes are restricted to the permits list
 *   permits   — exhaustive list of allowed subtypes
 *   final     — no further subclassing (leaf node)
 *   non-sealed — deliberately re-opens the hierarchy at that subtype (escape hatch)
 */
public class SealedClassesDemo {

    // ─────────────────────────────────────────────────────────────────────
    // BEFORE SEALED — open interface, exhaustiveness not enforceable
    // ─────────────────────────────────────────────────────────────────────

    interface OldShape {}

    record OldCircle(double radius) implements OldShape {}
    record OldRectangle(double width, double height) implements OldShape {}

    static double areaOld(OldShape shape) {
        if (shape instanceof OldCircle c) {
            return Math.PI * c.radius() * c.radius();
        } else if (shape instanceof OldRectangle r) {
            return r.width() * r.height();
        } else {
            // Forced to have this — anyone could add OldTriangle later and this silently returns 0
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEALED INTERFACE — Shape hierarchy is closed
    // ─────────────────────────────────────────────────────────────────────

    sealed interface Shape permits Circle, Rectangle, Triangle {}

    // Each permitted subtype must declare final, sealed, or non-sealed.
    // Using records here: records are implicitly final → natural fit.
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double base, double height) implements Shape {}

    // No default needed — compiler sees all three permitted types are covered.
    static double area(Shape shape) {
        return switch (shape) {
            case Circle c    -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Triangle t  -> 0.5 * t.base() * t.height();
            // If you delete any case above — COMPILE ERROR. That's the point.
        };
    }

    static String describe(Shape shape) {
        return switch (shape) {
            case Circle c    -> "Circle with radius " + c.radius();
            case Rectangle r -> "Rectangle " + r.width() + "×" + r.height();
            case Triangle t  -> "Triangle base=" + t.base() + " h=" + t.height();
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESULT<T, E> — sealed ADT pattern (like Rust Result / Kotlin sealed class)
    // Models success/failure without exceptions. Forces callers to handle both.
    // ─────────────────────────────────────────────────────────────────────

    sealed interface Result<T> permits Result.Success, Result.Failure {
        record Success<T>(T value)      implements Result<T> {}
        record Failure<T>(String error) implements Result<T> {}
    }

    static Result<Integer> parseInt(String input) {
        try {
            return new Result.Success<>(Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return new Result.Failure<>("Not a number: " + input);
        }
    }

    static String handleResult(Result<Integer> result) {
        return switch (result) {
            case Result.Success<Integer> s -> "Parsed: " + s.value();
            case Result.Failure<Integer> f -> "Error:  " + f.error();
            // No default. Sealed = exhaustive. New variant = compile error until handled.
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // NON-SEALED — escape hatch that re-opens the hierarchy at a subtype
    // ─────────────────────────────────────────────────────────────────────

    sealed interface Notification permits EmailNotification, SmsNotification, CustomNotification {}

    record EmailNotification(String to, String subject) implements Notification {}
    record SmsNotification(String phone, String text)   implements Notification {}

    // non-sealed: third parties can extend CustomNotification freely.
    // Cost: switch over Notification must include a default when reaching CustomNotification subtypes.
    non-sealed interface CustomNotification extends Notification {}

    // ─────────────────────────────────────────────────────────────────────
    // EXPRESSION TREE — multi-level sealed hierarchy (classic use case)
    // ─────────────────────────────────────────────────────────────────────

    sealed interface Expr permits Expr.Num, Expr.Add, Expr.Mul {
        record Num(int value)             implements Expr {}
        record Add(Expr left, Expr right) implements Expr {}
        record Mul(Expr left, Expr right) implements Expr {}
    }

    // Recursive tree walk — exhaustive at every level, no default
    static int compute(Expr expr) {
        return switch (expr) {
            case Expr.Num n  -> n.value();
            case Expr.Add a  -> compute(a.left()) + compute(a.right());
            case Expr.Mul m  -> compute(m.left()) * compute(m.right());
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        System.out.println("===== SEALED CLASSES DEMO =====\n");

        // ── 1. Shape hierarchy ──────────────────────────────────────────
        System.out.println("--- 1. Shape Area (sealed + records + exhaustive switch) ---");
        List<Shape> shapes = List.of(
            new Circle(5),
            new Rectangle(4, 6),
            new Triangle(3, 8)
        );
        for (Shape s : shapes) {
            System.out.printf("  %-35s  area = %.2f%n", describe(s), area(s));
        }

        System.out.println();

        // ── 2. Result ADT ────────────────────────────────────────────────
        System.out.println("--- 2. Result<T> ADT (success/failure without exceptions) ---");
        List<String> inputs = List.of("42", "hello", "100", "3.14");
        for (String input : inputs) {
            System.out.println("  \"" + input + "\" → " + handleResult(parseInt(input)));
        }

        System.out.println();

        // ── 3. Expression tree ───────────────────────────────────────────
        System.out.println("--- 3. Expression Tree (2 + 3) * 4 ---");
        Expr expr = new Expr.Mul(
            new Expr.Add(new Expr.Num(2), new Expr.Num(3)),
            new Expr.Num(4)
        );
        System.out.println("  Result: " + compute(expr));  // 20

        System.out.println();

        // ── 4. Key rules summary ─────────────────────────────────────────
        System.out.println("--- 4. Key Rules ---");
        System.out.println("  sealed type   → subclasses listed in 'permits'");
        System.out.println("  final         → no further subclassing (most common with records)");
        System.out.println("  non-sealed    → deliberately re-opens the hierarchy");
        System.out.println("  switch over sealed with all cases covered → no default needed");
        System.out.println("  add a new permitted subtype → every exhaustive switch → compile error");
        System.out.println("  records are implicitly final → natural sealed subtype");

        System.out.println("\n===== END =====");
    }
}
