package com.kowshik.patternmatching;

import java.util.List;

/**
 * PATTERN MATCHING FOR switch — Java 21 (stable)
 *
 * PROBLEM:
 *   Pre-21 switch only accepted primitives, enums, and strings — not types.
 *   Type-based dispatch required chained if-else instanceof, which:
 *     - does not enforce exhaustiveness
 *     - must handle null separately or risk NPE
 *     - cannot express guarded sub-conditions cleanly
 *
 * SOLUTION (Java 21):
 *   switch accepts any type.
 *   case Circle c  → type pattern: matches and binds in one step.
 *   case Circle c when c.radius() > 10 → guarded pattern: adds a boolean condition.
 *   case null      → explicit null handling inside the switch.
 *   sealed + switch → exhaustiveness checked at compile time, default not required.
 *   case Point(int x, int y) → record deconstruction pattern (destructures in-place).
 *
 * SWITCH FORMS RECAP (stays the same as Java 14 switch expressions):
 *   Arrow case  (->): no fall-through, expression or block
 *   Colon case  (:):  fall-through like classic switch
 *   Switch expression: yields a value
 *   Switch statement: used for side effects
 */
public class SwitchPatternMatchingDemo {

    // ─────────────────────────────────────────────────────────────────────
    // DOMAIN — sealed shape hierarchy (same as SealedClassesDemo)
    // ─────────────────────────────────────────────────────────────────────

    sealed interface Shape permits Shape.Circle, Shape.Rectangle, Shape.Triangle {
        record Circle(double radius)               implements Shape {}
        record Rectangle(double width, double height) implements Shape {}
        record Triangle(double base, double height)   implements Shape {}
    }

    // ─────────────────────────────────────────────────────────────────────
    // BEFORE — chained instanceof with no exhaustiveness guarantee
    // ─────────────────────────────────────────────────────────────────────

    static double areaOld(Shape shape) {
        if (shape instanceof Shape.Circle c) {
            return Math.PI * c.radius() * c.radius();
        } else if (shape instanceof Shape.Rectangle r) {
            return r.width() * r.height();
        } else if (shape instanceof Shape.Triangle t) {
            return 0.5 * t.base() * t.height();
        } else {
            throw new IllegalArgumentException("Unknown shape");  // needed but unreachable
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // TYPE PATTERNS — switch dispatches on type
    // Compiler verifies exhaustiveness because Shape is sealed.
    // ─────────────────────────────────────────────────────────────────────

    static double area(Shape shape) {
        return switch (shape) {
            case Shape.Circle c    -> Math.PI * c.radius() * c.radius();
            case Shape.Rectangle r -> r.width() * r.height();
            case Shape.Triangle t  -> 0.5 * t.base() * t.height();
            // No default needed — sealed hierarchy is exhaustive.
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // GUARDED PATTERNS — refine a type pattern with a boolean condition
    // Syntax: case TypePattern when booleanExpr
    // ─────────────────────────────────────────────────────────────────────

    static String classify(Shape shape) {
        return switch (shape) {
            case Shape.Circle c when c.radius() > 100  -> "Large circle";
            case Shape.Circle c when c.radius() > 10   -> "Medium circle";
            case Shape.Circle c                         -> "Small circle";
            // More specific guards must come before broader type patterns —
            // compiler checks for dominance (a dominated case is a compile error).
            case Shape.Rectangle r when r.width() == r.height() -> "Square";
            case Shape.Rectangle r                               -> "Rectangle";
            case Shape.Triangle t when t.base() == t.height()   -> "Isoceles-ish triangle";
            case Shape.Triangle t                                -> "Triangle";
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // NULL HANDLING — explicit case null inside the switch
    // Without case null, switch throws NullPointerException for null input (same as pre-21).
    // case null + case default can be combined: case null, default ->
    // ─────────────────────────────────────────────────────────────────────

    static String renderShapeLabel(Shape shape) {
        return switch (shape) {
            case null              -> "(no shape)";
            case Shape.Circle c    -> "○ r=" + c.radius();
            case Shape.Rectangle r -> "▭ " + r.width() + "×" + r.height();
            case Shape.Triangle t  -> "△ b=" + t.base();
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // RECORD DECONSTRUCTION — destructure record components inside the case
    // Java 21: case Record(T component) — bind component variables directly
    // ─────────────────────────────────────────────────────────────────────

    record Point(int x, int y) {}
    record Line(Point start, Point end) {}

    static String describePoint(Object obj) {
        return switch (obj) {
            // Deconstruct Point into x and y inline — no .x() calls needed
            case Point(int x, int y) when x == 0 && y == 0 -> "Origin";
            case Point(int x, int y) when x == 0            -> "On Y-axis at y=" + y;
            case Point(int x, int y) when y == 0            -> "On X-axis at x=" + x;
            case Point(int x, int y)                         -> "Point(" + x + ", " + y + ")";
            // Nested deconstruction — unwrap Line then unwrap Points
            case Line(Point(int x1, int y1), Point(int x2, int y2))
                -> "Line from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
            case null  -> "null";
            default    -> "Unknown: " + obj;
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // OPEN HIERARCHY — when type is not sealed, default is required
    // ─────────────────────────────────────────────────────────────────────

    static String formatObject(Object obj) {
        return switch (obj) {
            case Integer i           -> "int: " + i;
            case Long l              -> "long: " + l;
            case Double d            -> "double: " + String.format("%.2f", d);
            case String s when s.isEmpty() -> "(empty string)";
            case String s            -> "\"" + s + "\"";
            case null                -> "null";
            default                  -> obj.getClass().getSimpleName() + ": " + obj;
            // default required because Object is not sealed
        };
    }

    // ─────────────────────────────────────────────────────────────────────
    // SWITCH STATEMENT FORM — when you want side effects, not a value
    // ─────────────────────────────────────────────────────────────────────

    static void logShape(Shape shape) {
        switch (shape) {
            case Shape.Circle c    -> System.out.println("  [METRIC] circle.area=" + area(c));
            case Shape.Rectangle r -> System.out.println("  [METRIC] rect.area=" + area(r));
            case Shape.Triangle t  -> System.out.println("  [METRIC] tri.area=" + area(t));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        System.out.println("===== SWITCH PATTERN MATCHING DEMO =====\n");

        // ── 1. Type patterns: old vs new ────────────────────────────────
        System.out.println("--- 1. Type Patterns: Old vs New ---");
        List<Shape> shapes = List.of(
            new Shape.Circle(5),
            new Shape.Rectangle(4, 6),
            new Shape.Triangle(3, 8)
        );
        for (Shape s : shapes) {
            System.out.printf("  old: %.2f   new: %.2f%n", areaOld(s), area(s));
        }

        System.out.println();

        // ── 2. Guarded patterns ─────────────────────────────────────────
        System.out.println("--- 2. Guarded Patterns (when clause) ---");
        List<Shape> mixed = List.of(
            new Shape.Circle(200),
            new Shape.Circle(50),
            new Shape.Circle(3),
            new Shape.Rectangle(5, 5),
            new Shape.Rectangle(3, 7),
            new Shape.Triangle(4, 4)
        );
        for (Shape s : mixed) {
            System.out.println("  " + s + " → " + classify(s));
        }

        System.out.println();

        // ── 3. Null handling ────────────────────────────────────────────
        System.out.println("--- 3. Null Handling ---");
        List<Shape> withNulls = new java.util.ArrayList<>(shapes);
        withNulls.add(null);
        for (Shape s : withNulls) {
            System.out.println("  " + renderShapeLabel(s));
        }

        System.out.println();

        // ── 4. Record deconstruction ────────────────────────────────────
        System.out.println("--- 4. Record Deconstruction Patterns ---");
        List<Object> points = List.of(
            new Point(0, 0),
            new Point(0, 5),
            new Point(3, 0),
            new Point(3, 4),
            new Line(new Point(1, 2), new Point(5, 6)),
            "not a point",
            42
        );
        for (Object o : points) {
            System.out.println("  " + describePoint(o));
        }

        System.out.println();

        // ── 5. Open hierarchy (Object) ──────────────────────────────────
        System.out.println("--- 5. Open Hierarchy (Object, requires default) ---");
        List<Object> objs = List.of(42, 100L, 3.14, "hello", "", true);
        for (Object o : objs) {
            System.out.println("  " + formatObject(o));
        }
        System.out.println("  " + formatObject(null));

        System.out.println();

        // ── 6. Switch statement for side effects ────────────────────────
        System.out.println("--- 6. Switch Statement Form ---");
        shapes.forEach(SwitchPatternMatchingDemo::logShape);

        System.out.println();

        System.out.println("Key rules:");
        System.out.println("  sealed type → default not needed if all cases covered");
        System.out.println("  guarded patterns must be ordered most-specific first (dominance rule)");
        System.out.println("  case null must be explicit; absent → switch NPEs on null (same as pre-21)");
        System.out.println("  record deconstruction → bind components directly in case label");
        System.out.println("  nested deconstruction → works for nested records too");
        System.out.println("  open type (Object etc.) → default always required");

        System.out.println("\n===== END =====");
    }
}
