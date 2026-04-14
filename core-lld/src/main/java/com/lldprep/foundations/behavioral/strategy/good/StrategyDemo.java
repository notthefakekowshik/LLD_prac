package com.lldprep.foundations.behavioral.strategy.good;

import java.util.Arrays;

/**
 * Strategy Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * You have a family of algorithms/behaviors that are interchangeable. Without Strategy,
 * you end up with a giant if-else or switch inside the context class to pick the algorithm.
 * Every new algorithm requires editing that class — violating OCP.
 *
 * <p><b>How it works:</b><br>
 * - {@code Context} (Sorter, OrderPricer) holds a reference to a {@code Strategy} interface.<br>
 * - Concrete strategy classes each implement one algorithm.<br>
 * - The context delegates to the strategy — it never knows which algorithm runs.<br>
 * - Strategy can be swapped at runtime via a setter.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>You need to swap algorithms at runtime (e.g., sort by different criteria).</li>
 *   <li>Multiple classes differ only in their behavior — extract that behavior into strategies.</li>
 *   <li>You want to eliminate conditionals: {@code if type == X ... else if type == Y ...}</li>
 * </ul>
 *
 * <p><b>Strategy vs State (common confusion):</b><br>
 * - {@code Strategy} is injected <i>from outside</i> and stays stable; the context doesn't care which strategy runs.<br>
 * - {@code State} transitions <i>from inside</i>; states know about each other and drive their own changes.
 *
 * <p><b>Real-world examples in this repo:</b><br>
 * {@code EvictionPolicy} in the Cache (LRU/LFU are strategies — swapped with zero changes to Cache).
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Basic strategy with runtime swap (Sorter)</li>
 *   <li>Lambda as a strategy (no class needed for trivial algorithms)</li>
 *   <li>Business-domain strategy (DiscountStrategy with multiple pricing rules)</li>
 * </ol>
 */
public class StrategyDemo {

    public static void main(String[] args) {
        demo1_SortingWithRuntimeSwap();
        demo2_LambdaAsStrategy();
        demo3_DiscountStrategies();
    }

    // -------------------------------------------------------------------------

    private static void demo1_SortingWithRuntimeSwap() {
        section("Demo 1: Sorting — runtime strategy swap");

        int[] data = {5, 2, 8, 1, 9, 3};

        Sorter sorter = new Sorter(new BubbleSortStrategy());
        System.out.println("  Result: " + Arrays.toString(sorter.sort(data)));

        // Swap strategy — context (Sorter) is unchanged
        sorter.setStrategy(new MergeSortStrategy());
        System.out.println("  Result: " + Arrays.toString(sorter.sort(data)));

        sorter.setStrategy(new JavaBuiltInSortStrategy());
        System.out.println("  Result: " + Arrays.toString(sorter.sort(data)));
    }

    private static void demo2_LambdaAsStrategy() {
        section("Demo 2: Lambda as a strategy (no class needed for trivial cases)");

        // Any lambda that matches the interface signature IS a strategy.
        // This shows how Strategy integrates naturally with Java's functional style.
        SortStrategy reverseSort = new SortStrategy() {
            @Override
            public int[] sort(int[] data) {
                int[] arr = java.util.Arrays.copyOf(data, data.length);
                for (int i = 0; i < arr.length / 2; i++) {
                    int tmp = arr[i]; arr[i] = arr[arr.length - 1 - i]; arr[arr.length - 1 - i] = tmp;
                }
                java.util.Arrays.sort(arr);
                // reverse
                for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
                    int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
                }
                return arr;
            }

            @Override
            public String name() { return "ReverseSort (anonymous)"; }
        };

        Sorter sorter = new Sorter(reverseSort);
        int[] data = {3, 1, 4, 1, 5, 9};
        System.out.println("  Result: " + Arrays.toString(sorter.sort(data)));
    }

    private static void demo3_DiscountStrategies() {
        section("Demo 3: Discount strategies — business rule as strategy");

        OrderPricer pricer = new OrderPricer(new NoDiscountStrategy());
        pricer.calculateFinalPrice(1000.0);

        pricer.setDiscountStrategy(new PercentageDiscountStrategy(20));
        pricer.calculateFinalPrice(1000.0);

        pricer.setDiscountStrategy(new FlatDiscountStrategy(150));
        pricer.calculateFinalPrice(1000.0);

        // Evolve: New "Buy above 500 get 10% off" rule — add a new class, zero changes here
        pricer.setDiscountStrategy(new DiscountStrategy() {
            @Override
            public double apply(double originalPrice) {
                return originalPrice > 500 ? originalPrice * 0.90 : originalPrice;
            }
            @Override
            public String description() { return "10% off on orders > 500"; }
        });
        pricer.calculateFinalPrice(1000.0);
        pricer.calculateFinalPrice(400.0);
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
