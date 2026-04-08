package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Operator interfaces — specializations of Function where input and output types are the SAME.
 *
 * UnaryOperator<T>  extends Function<T, T>        : T -> T         (one input, same type output)
 * BinaryOperator<T> extends BiFunction<T, T, T>   : (T, T) -> T   (two inputs of same type, same type output)
 *
 * Use them instead of Function/BiFunction when the types are all identical — it's more expressive.
 */
public class OperatorDemo {

    public static void main(String[] args) {

        // --- UnaryOperator<T> ---
        // A Function<T, T>: transforms a value and returns the same type.

        UnaryOperator<String> trim = String::trim;
        UnaryOperator<String> toLowerCase = String::toLowerCase;
        UnaryOperator<String> addBrackets = s -> "[" + s + "]";

        // Chaining with andThen (inherited from Function)
        // trim -> toLowerCase -> addBrackets
        UnaryOperator<String> sanitize = (UnaryOperator<String>) trim
                .andThen(toLowerCase)
                .andThen(addBrackets);

        List<String> rawInputs = List.of("  HELLO ", " World ", "  JAVA  ");

        System.out.println("=== UnaryOperator: sanitize pipeline ===");
        List<String> sanitized = rawInputs.stream()
                .map(sanitize)
                .collect(Collectors.toList());
        sanitized.forEach(System.out::println);

        // Numeric UnaryOperator
        UnaryOperator<Integer> square = n -> n * n;
        UnaryOperator<Integer> addTen = n -> n + 10;

        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        System.out.println("\nSquared then +10:");
        numbers.stream()
                .map(square.andThen(addTen))
                .forEach(System.out::println);

        // --- BinaryOperator<T> ---
        // A BiFunction<T, T, T>: merges two values of the same type into one.

        BinaryOperator<Integer> sum = Integer::sum;
        BinaryOperator<Integer> max = Integer::max;
        BinaryOperator<String> concat = (a, b) -> a + " " + b;

        System.out.println("\n=== BinaryOperator ===");
        System.out.println("sum(3, 7)    = " + sum.apply(3, 7));
        System.out.println("max(3, 7)    = " + max.apply(3, 7));
        System.out.println("concat(Hi, World) = " + concat.apply("Hi", "World"));

        // Classic use: reduce() with BinaryOperator
        int total = numbers.stream().reduce(0, sum);
        System.out.println("reduce sum   = " + total);

        // BinaryOperator.minBy / maxBy — returns the lesser/greater by a comparator
        BinaryOperator<String> longerString = BinaryOperator.maxBy(
                (a, b) -> Integer.compare(a.length(), b.length())
        );
        System.out.println("Longer of ('hi', 'hello') = " + longerString.apply("hi", "hello"));
    }
}
