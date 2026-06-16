package com.kowshik.patternmatching;

import java.util.List;

/**
 * PATTERN MATCHING FOR instanceof — Java 16 (stable)
 *
 * PROBLEM:
 *   Classic instanceof check + cast is a two-step ceremony that hides intent:
 *
 *     if (obj instanceof String) {
 *         String s = (String) obj;   // redundant cast — we JUST verified it's a String
 *         doSomethingWith(s);
 *     }
 *
 *   The compiler already knows obj IS a String inside the if-block.
 *   The manual cast is pure noise. It also creates a window for bugs
 *   when the cast target type and the instanceof type drift apart.
 *
 * SOLUTION:
 *   if (obj instanceof String s) { ... }
 *   The binding variable s is automatically typed and in scope only where the pattern holds.
 *   No manual cast. Single point of truth for the type.
 */
public class PatternMatchingInstanceofDemo {

    // ─────────────────────────────────────────────────────────────────────
    // BEFORE (Java 15 and earlier)
    // ─────────────────────────────────────────────────────────────────────

    static String describeOld(Object obj) {
        if (obj instanceof String) {
            String s = (String) obj;           // ← redundant cast
            return "String of length " + s.length();
        } else if (obj instanceof Integer) {
            Integer i = (Integer) obj;
            return "Integer: " + (i * 2);
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            return "List of size " + list.size();
        } else {
            return "Unknown: " + obj.getClass().getSimpleName();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // AFTER — pattern matching for instanceof
    // ─────────────────────────────────────────────────────────────────────

    static String describe(Object obj) {
        if (obj instanceof String s) {
            return "String of length " + s.length();    // s is String, no cast
        } else if (obj instanceof Integer i) {
            return "Integer: " + (i * 2);               // i is Integer
        } else if (obj instanceof List<?> list) {
            return "List of size " + list.size();
        } else {
            return "Unknown: " + obj.getClass().getSimpleName();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SCOPING — binding variable is in scope only where the pattern is true
    // ─────────────────────────────────────────────────────────────────────

    static void scopingRules(Object obj) {
        // Binding variable s is in scope in the if-body (where instanceof is true)
        if (obj instanceof String s) {
            System.out.println("  In-scope in if-body: " + s.toUpperCase());
        }
        // s is NOT in scope here — wouldn't compile if you tried

        // ── Negation scope (useful for early-return guards) ──
        // If the pattern is in a negated condition and the method returns/throws,
        // the binding variable is in scope in the code after the guard.
        if (!(obj instanceof String s)) {
            return;   // not a string — bail out
        }
        // s IS in scope here because we know it survived the guard
        System.out.println("  After guard: " + s.trim());
    }

    // ─────────────────────────────────────────────────────────────────────
    // COMBINING WITH && — binding variable scope extends through &&
    // ─────────────────────────────────────────────────────────────────────

    static String checkLength(Object obj) {
        // Both conditions must be true — s is in scope for the length check
        if (obj instanceof String s && s.length() > 5) {
            return "Long string: " + s;
        }
        return "Not a long string";
    }

    // !! Cannot combine with || — s would not be definitely assigned in both branches
    // if (obj instanceof String s || s.isEmpty()) { }  ← compile error

    // ─────────────────────────────────────────────────────────────────────
    // NULL — instanceof always returns false for null (same as before)
    // No change in behaviour, but worth noting explicitly
    // ─────────────────────────────────────────────────────────────────────

    static void nullBehaviour(Object obj) {
        if (obj instanceof String s) {
            System.out.println("  String: " + s);
        } else {
            // null falls here — instanceof never matches null
            System.out.println("  Not a String (or null): " + obj);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRACTICAL: polymorphic processing without a common interface
    // Common when working with legacy APIs, serialized types, or mixed collections
    // ─────────────────────────────────────────────────────────────────────

    sealed interface JsonNode permits JsonNode.JsonString, JsonNode.JsonNumber,
                                      JsonNode.JsonBoolean, JsonNode.JsonNull {}
    record JsonString(String value)   implements JsonNode {}
    record JsonNumber(double value)   implements JsonNode {}
    record JsonBoolean(boolean value) implements JsonNode {}
    record JsonNull()                 implements JsonNode {}

    static String renderJson(Object raw) {
        // raw could be String, Number, Boolean, null from any JSON parser
        if (raw instanceof String s)  return "\"" + s + "\"";
        if (raw instanceof Number n)  return String.valueOf(n.doubleValue());
        if (raw instanceof Boolean b) return b.toString();
        if (raw == null)              return "null";
        return "\"" + raw + "\"";
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRACTICAL: equals() implementation — classic use case
    // ─────────────────────────────────────────────────────────────────────

    static class Point {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object obj) {
            // Old way needed two lines (instanceof + cast). New way: one.
            return obj instanceof Point p && p.x == x && p.y == y;
        }

        @Override
        public int hashCode() { return 31 * x + y; }

        @Override
        public String toString() { return "Point(" + x + ", " + y + ")"; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        System.out.println("===== PATTERN MATCHING FOR instanceof DEMO =====\n");

        // ── 1. Old vs new ───────────────────────────────────────────────
        System.out.println("--- 1. Old Way vs Pattern Matching ---");
        List<Object> values = List.of("hello", 42, List.of(1, 2, 3), 3.14);
        for (Object v : values) {
            System.out.println("  old: " + describeOld(v));
            System.out.println("  new: " + describe(v));
        }

        System.out.println();

        // ── 2. Scope rules ──────────────────────────────────────────────
        System.out.println("--- 2. Scope Rules ---");
        scopingRules("  Hello World  ");
        scopingRules(42);  // hits the return guard

        System.out.println();

        // ── 3. Combining with && ────────────────────────────────────────
        System.out.println("--- 3. Combining With && ---");
        System.out.println("  " + checkLength("Hi"));
        System.out.println("  " + checkLength("Hello World"));
        System.out.println("  " + checkLength(42));

        System.out.println();

        // ── 4. Null behaviour ───────────────────────────────────────────
        System.out.println("--- 4. Null Behaviour ---");
        nullBehaviour("test");
        nullBehaviour(null);

        System.out.println();

        // ── 5. equals() ─────────────────────────────────────────────────
        System.out.println("--- 5. Cleaner equals() ---");
        Point p1 = new Point(1, 2);
        Point p2 = new Point(1, 2);
        Point p3 = new Point(3, 4);
        System.out.println("  p1.equals(p2): " + p1.equals(p2));   // true
        System.out.println("  p1.equals(p3): " + p1.equals(p3));   // false
        System.out.println("  p1.equals(\"x\"): " + p1.equals("x")); // false

        System.out.println();

        // ── 6. JSON render ─────────────────────────────────────────────
        System.out.println("--- 6. JSON Renderer ---");
        List<Object> jsonValues = List.of("hello", 42.0, true);
        for (Object v : jsonValues) {
            System.out.println("  " + v + " → " + renderJson(v));
        }
        System.out.println("  null → " + renderJson(null));

        System.out.println();

        System.out.println("Key rules:");
        System.out.println("  instanceof with pattern var → binding in scope only where true");
        System.out.println("  !(instanceof) + return/throw → binding in scope after the guard");
        System.out.println("  Can combine with && (binding extends through)");
        System.out.println("  Cannot combine with || (binding not definitely assigned)");
        System.out.println("  null → instanceof always false (unchanged from classic instanceof)");

        System.out.println("\n===== END =====");
    }
}
