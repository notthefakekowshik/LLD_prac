package com.lldprep.foundations.behavioral.strategy.good;

import java.util.Arrays;

/**
 * Wraps Java's built-in sort as a strategy.
 * Demonstrates that lambdas or any logic can be a strategy — the context doesn't care.
 */
public class JavaBuiltInSortStrategy implements SortStrategy {

    @Override
    public int[] sort(int[] data) {
        int[] arr = Arrays.copyOf(data, data.length);
        Arrays.sort(arr);
        return arr;
    }

    @Override
    public String name() { return "Java Arrays.sort (TimSort)"; }
}
