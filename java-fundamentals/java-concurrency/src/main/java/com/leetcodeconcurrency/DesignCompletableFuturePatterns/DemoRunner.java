package com.leetcodeconcurrency.DesignCompletableFuturePatterns;

/**
 * DemoRunner — runs all CompletableFuture pattern demos in order.
 *
 * Order: basic → combining → error handling → timeout → real-world scenario.
 */
public class DemoRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("  CompletableFuture Patterns — Interview Prep");
        System.out.println("═══════════════════════════════════════════");

        System.out.println("\n\n=== 1. BASIC CHAINING ===");
        BasicChaining.main(args);

        System.out.println("\n\n=== 2. COMBINING PATTERNS ===");
        CombiningPatterns.main(args);

        System.out.println("\n\n=== 3. ERROR HANDLING ===");
        ErrorHandlingPatterns.main(args);

        System.out.println("\n\n=== 4. TIMEOUT & CANCELLATION ===");
        TimeoutAndCancellationPatterns.main(args);

        System.out.println("\n\n=== 5. PARALLEL SERVICE AGGREGATION ===");
        ParallelServiceAggregation.main(args);

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("  All demos complete.");
        System.out.println("═══════════════════════════════════════════");
    }
}
