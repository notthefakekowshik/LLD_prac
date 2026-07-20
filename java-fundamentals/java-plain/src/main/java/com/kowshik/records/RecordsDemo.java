package com.kowshik.records;

import java.util.List;
import java.util.Objects;

/**
 * WHY RECORDS EXIST — The full story.
 *
 * Java Records (Java 16+) are a special class kind for modelling
 * pure data carriers — objects whose only job is to hold values.
 *
 * PROBLEM: Writing a simple "data class" in plain Java requires:
 *   - private final fields
 *   - constructor
 *   - getters
 *   - equals() + hashCode()
 *   - toString()
 * That's 30-50 lines for 3 fields. Boilerplate hides intent.
 *
 * SOLUTION: record Point(int x, int y) {} — compiler generates ALL of the above.
 */
public class RecordsDemo {

    // ─────────────────────────────────────────────
    // THE OLD WAY — plain Java class for a data carrier
    // ─────────────────────────────────────────────
    static class PointOld {
        private final int x;
        private final int y;

        PointOld(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PointOld)) return false;
            PointOld that = (PointOld) o;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "PointOld[x=" + x + ", y=" + y + "]";
        }
    }

    // ─────────────────────────────────────────────
    // THE RECORD WAY — same semantics, 1 line
    // Compiler auto-generates: constructor, accessors, equals, hashCode, toString
    // ─────────────────────────────────────────────
    record Point(int x, int y) {

    }

    // ─────────────────────────────────────────────
    // RECORDS WITH CUSTOM VALIDATION — compact constructor
    // Use when you need to enforce invariants on the data
    // ─────────────────────────────────────────────
    record Range(int min, int max) {
        Range {  // compact constructor — no parameter list, fields assigned automatically after
            if (min > max) {
                throw new IllegalArgumentException("min must be <= max, got: " + min + " > " + max);
            }
        }
    }

    // ─────────────────────────────────────────────
    // RECORDS WITH EXTRA METHODS — records can have instance methods
    // ─────────────────────────────────────────────
    record Money(double amount, String currency) {
        Money {
            if (amount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("Currency required");
            }
        }

        // Custom instance method — perfectly valid
        Money add(Money other) {
            if (!currency.equals(other.currency)) {
                throw new IllegalArgumentException("Cannot add different currencies");
            }
            return new Money(amount + other.amount, currency);
        }

        @Override
        public String toString() {
            return String.format("%.2f %s", amount, currency);
        }
    }

    // ─────────────────────────────────────────────
    // RECORDS AS DTOs — typical real-world use: API response / request objects
    // ─────────────────────────────────────────────
    record UserResponse(long id, String name, String email) {

    }

    record OrderLine(String productId, int quantity, double unitPrice) {
        public double totalPrice() {
            return quantity * unitPrice;
        }
    }

    // ─────────────────────────────────────────────
    // RECORDS ARE IMMUTABLE — no setters, all fields final
    // "Copying with a change" uses the new record value
    // ─────────────────────────────────────────────
    record Config(String host, int port, boolean ssl) {
        Config withPort(int newPort) {
            return new Config(host, newPort, ssl);   // copy-with-change idiom
        }
        Config withSsl(boolean newSsl) {
            return new Config(host, port, newSsl);
        }
    }

    public static void main(String[] args) {

        System.out.println("===== JAVA RECORDS =====\n");

        // ── 1. Boilerplate comparison ──────────────────────────────
        System.out.println("--- 1. Boilerplate: Old class vs Record ---");
        PointOld old = new PointOld(3, 4);
        Point rec = new Point(3, 4);

        System.out.println("Old:    " + old);
        System.out.println("Record: " + rec);
        System.out.println("equals: " + new Point(3, 4).equals(new Point(3, 4)));   // true — auto-generated
        System.out.println("Accessor: rec.x() = " + rec.x() + ", rec.y() = " + rec.y());
        // Note: accessor name matches field name — rec.x(), NOT rec.getX()

        System.out.println();

        // ── 2. Compact constructor validation ─────────────────────
        System.out.println("--- 2. Compact Constructor Validation ---");
        Range valid = new Range(1, 10);
        System.out.println("Valid range: " + valid);
        try {
            new Range(10, 1);   // should throw
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println();

        // ── 3. Records with methods ────────────────────────────────
        System.out.println("--- 3. Records with Methods (Money) ---");
        Money price = new Money(100.0, "INR");
        Money tax   = new Money(18.0, "INR");
        Money total = price.add(tax);
        System.out.println("Price: " + price);
        System.out.println("Tax:   " + tax);
        System.out.println("Total: " + total);

        System.out.println();

        // ── 4. As DTOs ─────────────────────────────────────────────
        System.out.println("--- 4. As DTOs ---");
        UserResponse user = new UserResponse(42L, "Kowshik", "k@example.com");
        System.out.println("User: " + user);

        List<OrderLine> lines = List.of(
            new OrderLine("SKU-001", 3, 50.0),
            new OrderLine("SKU-002", 1, 200.0)
        );
        lines.forEach(line ->
            System.out.printf("  %s × %d = %.2f%n", line.productId(), line.quantity(), line.totalPrice())
        );

        System.out.println();

        // ── 5. Immutability + copy-with-change ────────────────────
        System.out.println("--- 5. Immutability and Copy-with-Change ---");
        Config prod = new Config("prod.example.com", 443, true);
        Config staging = prod.withPort(8080).withSsl(false);
        System.out.println("Prod:    " + prod);
        System.out.println("Staging: " + staging);
        System.out.println("prod is unchanged: " + prod);

        System.out.println();

        // ── 6. What records CANNOT do ─────────────────────────────
        System.out.println("--- 6. What Records CANNOT Do ---");
        System.out.println("  - Cannot extend another class (implicitly extends Record)");
        System.out.println("  - Cannot declare mutable (non-final) instance fields");
        System.out.println("  - Cannot have setters — immutable by design");
        System.out.println("  - CAN implement interfaces");
        System.out.println("  - CAN have static fields and static methods");

        System.out.println();
        System.out.println("Key insight: Use a record when the class's entire purpose");
        System.out.println("is to carry named data values — no behaviour, no mutability.");
        System.out.println("If you need mutability or inheritance, use a regular class.");

        System.out.println("\n===== END RECORDS DEMO =====");
    }
}
