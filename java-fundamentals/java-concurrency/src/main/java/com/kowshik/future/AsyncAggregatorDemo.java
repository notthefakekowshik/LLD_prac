package com.kowshik.future;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Demonstrates AsyncResultAggregator<T> in three realistic scenarios.
 *
 * INTERVIEW PREP — Key Concepts Demonstrated:
 * =============================================
 * 1. PECS (Producer Extends, Consumer Super)
 *    - aggregate() accepts List<? extends Supplier<? extends T>>
 *    - Scenario 3 passes List<Supplier<Integer>> where T=Number — works via PECS
 *
 * 2. Partial failure isolation
 *    - One slow/failing task does NOT cancel other tasks
 *    - Results split into successes and failures in AggregatedResult<T>
 *
 * 3. Per-task timeout
 *    - Each task gets its own orTimeout() deadline independent of others
 *
 * 4. Generic type safety without casting
 *    - The same aggregator works for Double (prices), String (API responses),
 *      Number (mixed numeric types via bounded wildcard)
 *
 * Part 1 — Happy path: all 5 exchange price feeds succeed
 * Part 2 — Partial failure: 2 of 4 feeds fail (one throws, one times out)
 * Part 3 — PECS demo: Supplier<Integer> passed to AsyncResultAggregator<Number>
 */
public class AsyncAggregatorDemo {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(8);

        System.out.println("=".repeat(60));
        System.out.println("Part 1 — Happy path: all feeds succeed");
        System.out.println("=".repeat(60));
        happyPath(pool);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Part 2 — Partial failure: 2 of 4 feeds fail");
        System.out.println("=".repeat(60));
        partialFailure(pool);

        System.out.println("\n" + "=".repeat(60));
        System.out.println("Part 3 — PECS: Supplier<Integer> → AsyncResultAggregator<Number>");
        System.out.println("=".repeat(60));
        pecsDemo(pool);

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("\nAll demos complete.");
    }

    // -------------------------------------------------------------------------
    // Part 1: All 5 price feeds return successfully

    private static void happyPath(ExecutorService pool) {
        AsyncResultAggregator<Double> aggregator = new AsyncResultAggregator<>(pool, 2000);

        List<Supplier<Double>> feeds = List.of(
                () -> fetchPrice("NYSE",    100),
                () -> fetchPrice("NASDAQ",  120),
                () -> fetchPrice("LSE",     150),
                () -> fetchPrice("TSE",      80),
                () -> fetchPrice("NSE",      90)
        );

        AggregatedResult<Double> result = aggregator.aggregate(feeds);

        System.out.println("Result: " + result);
        System.out.print("Prices: ");
        result.getSuccesses().forEach(p -> System.out.printf("%.2f  ", p));
        System.out.println();

        double average = result.getSuccesses().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        System.out.printf("Average price: %.2f%n", average);
    }

    // -------------------------------------------------------------------------
    // Part 2: 2 tasks fail — one throws, one exceeds the 500ms timeout

    private static void partialFailure(ExecutorService pool) {
        // 500ms timeout — feed "SLOW" will time out, feed "BAD" will throw
        AsyncResultAggregator<Double> aggregator = new AsyncResultAggregator<>(pool, 500);

        List<Supplier<Double>> feeds = List.of(
                () -> fetchPrice("NYSE",   100),
                () -> fetchPrice("NASDAQ", 120),
                () -> fetchPriceSlow("SLOW", 800),   // exceeds 500ms timeout
                () -> fetchPriceBad("BAD")            // throws RuntimeException
        );

        AggregatedResult<Double> result = aggregator.aggregate(feeds);

        System.out.println("Result: " + result);

        System.out.println("Successes:");
        result.getSuccesses().forEach(p -> System.out.printf("  price=%.2f%n", p));

        System.out.println("Failures:");
        result.getFailures().forEach(f ->
                System.out.printf("  task[%d] → %s: %s%n",
                        f.getTaskIndex(),
                        f.getCause().getClass().getSimpleName(),
                        f.getCause().getMessage()));
    }

    // -------------------------------------------------------------------------
    // Part 3: PECS — Supplier<Integer> fed into AsyncResultAggregator<Number>
    // Integer extends Number, so Supplier<Integer> is a Supplier<? extends Number>
    // This compiles without casting because aggregate() accepts List<? extends Supplier<? extends T>>

    private static void pecsDemo(ExecutorService pool) {
        AsyncResultAggregator<Number> aggregator = new AsyncResultAggregator<>(pool, 1000);

        // Supplier<Integer> — Integer is a subtype of Number (PECS: producer extends)
        List<Supplier<Integer>> integerSuppliers = List.of(
                () -> computeScore("A", 50),
                () -> computeScore("B", 100),
                () -> computeScore("C", 75)
        );

        // PECS in action: List<Supplier<Integer>> satisfies List<? extends Supplier<? extends Number>>
        // because Integer extends Number, making Supplier<Integer> a subtype of Supplier<? extends Number>
        AggregatedResult<Number> result = aggregator.aggregate(integerSuppliers);

        System.out.println("Result: " + result);
        System.out.println("Scores (as Number): " + result.getSuccesses());

        // Downstream: safe to use as Number (no cast needed)
        double total = result.getSuccesses().stream()
                .mapToDouble(Number::doubleValue)
                .sum();
        System.out.printf("Total score: %.0f%n", total);
    }

    // -------------------------------------------------------------------------
    // Simulated data sources

    private static double fetchPrice(String exchange, long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted fetching from " + exchange, e);
        }
        return 100.0 + Math.random() * 50;
    }

    private static double fetchPriceSlow(String exchange, long sleepMs) {
        return fetchPrice(exchange, sleepMs); // will exceed timeout
    }

    private static double fetchPriceBad(String exchange) {
        throw new RuntimeException("Connection refused to " + exchange);
    }

    private static int computeScore(String participant, long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted computing score for " + participant, e);
        }
        return (int) (Math.random() * 100);
    }
}
