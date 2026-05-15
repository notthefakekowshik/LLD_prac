package com.kowshik.streamplayground;

import java.util.*;
import java.util.stream.*;

public class StreamBasicsDemo {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // Filter + Map + Collect
        List<Integer> evenSquares = numbers.stream()
            .filter(n -> n % 2 == 0)
            .map(n -> n * n)
            .toList();
        System.out.println("Even squares: " + evenSquares);

        // Reduce: sum
        int sum = numbers.stream()
            .reduce(0, Integer::sum);
        System.out.println("Sum: " + sum);

        // Grouping
        Map<String, List<Integer>> grouped = numbers.stream()
            .collect(Collectors.groupingBy(n -> n % 2 == 0 ? "even" : "odd"));
        System.out.println("Grouped: " + grouped);

        // FlatMap
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4), List.of(5, 6));
        List<Integer> flat = nested.stream()
            .flatMap(List::stream)
            .toList();
        System.out.println("Flattened: " + flat);

        // Infinite stream (limited)
        List<Integer> first10Squares = Stream.iterate(1, n -> n + 1)
            .map(n -> n * n)
            .limit(10)
            .toList();
        System.out.println("First 10 squares: " + first10Squares);
    }
}
