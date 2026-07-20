package com.kowshik.optional;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Every Optional method you actually reach for, grouped by intent.
 *
 * Mental model: Optional<T> is a box that holds 0 or 1 value. It exists to make
 * "might be absent" a compile-time visible fact instead of a runtime NPE surprise.
 *
 * Golden rules (see part9):
 *   - Use as a RETURN type, not as a field / parameter / collection element.
 *   - Never call get() without checking — prefer orElse / map / ifPresent.
 *   - orElse(x)      -> x is ALWAYS built (eager). Use for cheap constants.
 *   - orElseGet(sup) -> sup runs ONLY when empty (lazy). Use for expensive builds.
 */
public class OptionalDemo {

    public static void main(String[] args) {
        part1_creation();
        part2_presenceChecks();
        part3_conditionalActions();
        part4_retrievalAndDefaults();
        part5_transform_map();
        part6_transform_flatMap();
        part7_filter();
        part8_or_and_stream();
        part9_realisticChain_and_pitfalls();
        System.out.println("\nAll assertions passed. (run with -ea to enforce)");
    }

    // ================================================================
    // PART 1: Creation — of / ofNullable / empty
    // ================================================================
    static void part1_creation() {
        header("PART 1: creation");

        Optional<String> a = Optional.of("hello");          // value MUST be non-null, else NPE
        Optional<String> b = Optional.ofNullable(maybeNull(false)); // null-safe: null -> empty
        Optional<String> c = Optional.ofNullable(maybeNull(true));  // -> present
        Optional<String> d = Optional.empty();              // explicit "nothing"

        System.out.println("  of('hello')        = " + a);
        System.out.println("  ofNullable(null)   = " + b);
        System.out.println("  ofNullable('x')    = " + c);
        System.out.println("  empty()            = " + d);

        // of(null) blows up immediately — use it only when null is a bug, not a valid state.
        try {
            Optional.of(maybeNull(false));
        } catch (NullPointerException e) {
            System.out.println("  of(null) threw NPE (as designed)");
        }

        assert b.isEmpty() && c.isPresent();
    }

    // ================================================================
    // PART 2: Presence checks — isPresent / isEmpty
    // ================================================================
    static void part2_presenceChecks() {
        header("PART 2: presence checks");

        Optional<String> present = Optional.of("data");
        Optional<String> absent = Optional.empty();

        System.out.println("  present.isPresent() = " + present.isPresent());
        System.out.println("  present.isEmpty()   = " + present.isEmpty());
        System.out.println("  absent.isEmpty()    = " + absent.isEmpty());   // isEmpty(): Java 11+

        // Anti-pattern: if (o.isPresent()) o.get() ... — that's just a null check in a costume.
        // Prefer ifPresent / map / orElse (parts 3-5).
        assert present.isPresent() && absent.isEmpty();
    }

    // ================================================================
    // PART 3: Conditional actions — ifPresent / ifPresentOrElse
    // ================================================================
    static void part3_conditionalActions() {
        header("PART 3: conditional actions");

        Optional<String> present = Optional.of("job#42");
        Optional<String> absent = Optional.empty();

        present.ifPresent(v -> System.out.println("  ifPresent -> processing " + v));
        absent.ifPresent(v -> System.out.println("  (never runs)"));

        // ifPresentOrElse: value-branch OR empty-branch. (Java 9+)
        present.ifPresentOrElse(
                v -> System.out.println("  ifPresentOrElse present -> " + v),
                () -> System.out.println("  (never runs)"));
        absent.ifPresentOrElse(
                v -> System.out.println("  (never runs)"),
                () -> System.out.println("  ifPresentOrElse empty -> using fallback path"));

    }

    // ================================================================
    // PART 4: Retrieval & defaults — get / orElse / orElseGet / orElseThrow
    // ================================================================
    static void part4_retrievalAndDefaults() {
        header("PART 4: retrieval & defaults");

        Optional<String> present = Optional.of("real");
        Optional<String> absent = Optional.empty();

        // get() — only safe after a presence check. Throws NoSuchElementException if empty.
        System.out.println("  get() on present   = " + present.get());

        // orElse(default) — default is ALWAYS evaluated (eager). Good for constants.
        System.out.println("  orElse('def')      = " + absent.orElse("def"));

        // orElseGet(supplier) — supplier runs ONLY when empty (lazy). Good for expensive defaults.
        System.out.println("  orElseGet(...)     = " + absent.orElseGet(OptionalDemo::expensiveDefault));
        System.out.println("  orElseGet skipped on present:");
        present.orElseGet(OptionalDemo::expensiveDefault); // supplier NOT called — nothing prints

        // orElseThrow() — no-arg (Java 10+): throws NoSuchElementException.
        try {
            absent.orElseThrow();
        } catch (NoSuchElementException e) {
            System.out.println("  orElseThrow() -> NoSuchElementException");
        }

        // orElseThrow(supplier) — your own exception; the idiomatic "required value or fail".
        try {
            absent.orElseThrow(() -> new IllegalStateException("id is required"));
        } catch (IllegalStateException e) {
            System.out.println("  orElseThrow(sup) -> " + e.getMessage());
        }

        assert present.orElse("x").equals("real");
    }

    // ================================================================
    // PART 5: Transform — map (T -> U, auto-wrapped)
    // ================================================================
    static void part5_transform_map() {
        header("PART 5: map");

        Optional<String> name = Optional.of("kowshik");

        // map applies the fn only if present, and re-wraps the result in an Optional.
        Optional<Integer> len = name.map(String::length);
        Optional<String> upper = name.map(String::toUpperCase);

        System.out.println("  map(length)        = " + len);
        System.out.println("  map(toUpperCase)   = " + upper);

        // On empty, map short-circuits to empty — no fn call, no NPE.
        System.out.println("  empty.map(...)     = " + Optional.<String>empty().map(String::length));

        // If the mapper returns null, map produces empty (not Optional-of-null).
        System.out.println("  map(->null)        = " + name.map(s -> (String) null));

        assert len.get() == 7;
    }

    // ================================================================
    // PART 6: Transform — flatMap (fn already returns Optional, avoid double-wrap)
    // ================================================================
    static void part6_transform_flatMap() {
        header("PART 6: flatMap");

        User user = new User("Ada", new Address("221B", null));

        // Each accessor returns Optional. map would give Optional<Optional<String>>;
        // flatMap keeps it flat -> Optional<String>.
        Optional<String> street = Optional.of(user)
                .flatMap(User::address)
                .flatMap(Address::street);

        Optional<String> zip = Optional.of(user)
                .flatMap(User::address)
                .flatMap(Address::zip);   // zip is null inside -> empty, no NPE

        System.out.println("  street = " + street.orElse("<none>"));
        System.out.println("  zip    = " + zip.orElse("<none>"));

        assert street.get().equals("221B") && zip.isEmpty();
    }

    // ================================================================
    // PART 7: filter — keep the value only if it passes a predicate
    // ================================================================
    static void part7_filter() {
        header("PART 7: filter");

        Optional<Integer> age = Optional.of(20);

        System.out.println("  filter(>=18) on 20 = " + age.filter(x -> x >= 18)); // stays present
        System.out.println("  filter(>=18) on 15 = " + Optional.of(15).filter(x -> x >= 18)); // -> empty

        // Common combo: map then filter then default.
        String access = Optional.of(20)
                .filter(x -> x >= 18)
                .map(x -> "adult")
                .orElse("minor");
        System.out.println("  20 -> " + access);

        assert age.filter(x -> x >= 18).isPresent();
    }

    // ================================================================
    // PART 8: or (fallback Optional, Java 9+) & stream (bridge, Java 9+)
    // ================================================================
    static void part8_or_and_stream() {
        header("PART 8: or / stream");

        // or(supplier): if empty, try another Optional source. Chain fallbacks.
        Optional<String> config = Optional.<String>empty()
                .or(() -> Optional.empty())      // env var: absent
                .or(() -> Optional.of("from-file")); // file: found
        System.out.println("  or-chain -> " + config.get());

        // stream(): 0-or-1 element stream. Lets you flatMap a Stream<Optional<T>>
        // into a stream of only-present values with no manual filtering.
        long presentCount = java.util.stream.Stream.of(
                        Optional.of("a"), Optional.<String>empty(), Optional.of("b"))
                .flatMap(Optional::stream)   // drops the empty
                .count();
        System.out.println("  flatMap(Optional::stream) kept " + presentCount + " values");

        assert config.get().equals("from-file") && presentCount == 2;
    }

    // ================================================================
    // PART 9: realistic chain + the two pitfalls that bite people
    // ================================================================
    static void part9_realisticChain_and_pitfalls() {
        header("PART 9: realistic chain & pitfalls");

        // One readable pipeline: look up user -> address -> street -> normalize -> default.
        String display = findUser("Ada")
                .flatMap(User::address)
                .flatMap(Address::street)
                .map(String::toUpperCase)
                .orElse("ADDRESS UNKNOWN");
        System.out.println("  display = " + display);

        System.out.println("  missing user = " + findUser("nobody")
                .flatMap(User::address)
                .flatMap(Address::street)
                .orElse("ADDRESS UNKNOWN"));

        // Pitfall 1: orElse builds its argument even when unused.
        // Here the "expensive" default runs despite the value being present — wasteful.
        Optional.of("cached").orElse(expensiveDefault()); // expensiveDefault() STILL runs
        System.out.println("  ^ orElse evaluated its default eagerly (use orElseGet to avoid)");

        // Pitfall 2: Optional.of(null) / get() on empty — both throw. Shown in parts 1 & 4.

        assert display.equals("221B".toUpperCase());
    }

    // ---------- helpers & tiny model ----------

    static String maybeNull(boolean nonNull) {
        return nonNull ? "x" : null;
    }

    static String expensiveDefault() {
        System.out.println("    [expensiveDefault() executed]");
        return "expensive";
    }

    static Optional<User> findUser(String name) {
        return name.equals("Ada")
                ? Optional.of(new User("Ada", new Address("221B", null)))
                : Optional.empty();
    }

    // Accessors return Optional so callers chain with flatMap — the point of the pattern.
    // Plain classes (not records) so the accessor can return Optional<T> instead of the raw field.
    static final class User {
        private final String name;
        private final Address addr;
        User(String name, Address addr) { this.name = name; this.addr = addr; }
        Optional<Address> address() { return Optional.ofNullable(addr); }
    }

    static final class Address {
        private final String street;
        private final String zip;
        Address(String street, String zip) { this.street = street; this.zip = zip; }
        Optional<String> street() { return Optional.ofNullable(street); }
        Optional<String> zip() { return Optional.ofNullable(zip); }
    }

    static void header(String title) {
        System.out.println("\n┌─ " + title + " " + "─".repeat(Math.max(0, 56 - title.length())) + "┐");
    }
}
