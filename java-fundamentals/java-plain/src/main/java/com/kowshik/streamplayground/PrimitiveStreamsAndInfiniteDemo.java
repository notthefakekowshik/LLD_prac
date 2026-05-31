package com.kowshik.streamplayground;

import java.util.*;
import java.util.stream.*;
import java.util.concurrent.ThreadLocalRandom;

public class PrimitiveStreamsAndInfiniteDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          PRIMITIVE + INFINITE STREAMS DEMONSTRATION          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        part1_IntStreamCreation();
        part2_SummaryStatistics();
        part3_BoxingVsPrimitive_Perf();
        part4_ConversionBetweenStreamTypes();
        part5_StreamIterate_Java8VsJava9();
        part6_StreamGenerate();
        part7_TakeWhile_DropWhile();
        part8_ReduceDeepDive();
        part9_FlatmapVsMap();
    }

    // ================================================================
    // PART 1: IntStream / LongStream / DoubleStream creation
    // ================================================================
    static void part1_IntStreamCreation() {
        System.out.println("┌─ PART 1: Primitive Stream Creation ──────────────────────────┐");

        // range / rangeClosed
        System.out.println("  IntStream.range(1, 5):      " + IntStream.range(1, 5).boxed().toList());
        System.out.println("  IntStream.rangeClosed(1, 5): " + IntStream.rangeClosed(1, 5).boxed().toList());

        // from array
        int[] arr = {10, 20, 30};
        System.out.println("  Arrays.stream(arr):         " + Arrays.stream(arr).boxed().toList());

        // from random
        IntStream randomInts = ThreadLocalRandom.current().ints(5, 1, 100);
        System.out.println("  Random.ints(5, 1, 100):     " + randomInts.boxed().toList());

        // from chars
        String word = "hello";
        IntStream charCodes = word.chars();
        System.out.println("  \"hello\".chars():            " + charCodes.boxed().toList());
        System.out.println("  \"hello\".chars() as chars:   "
            + word.chars().mapToObj(c -> String.valueOf((char) c)).toList());

        // DoubleStream
        DoubleStream doubles = DoubleStream.of(1.5, 2.5, 3.5);
        System.out.println("  DoubleStream.of(1.5, 2.5):  " + doubles.boxed().toList());

        // LongStream
        LongStream longs = LongStream.range(100, 105);
        System.out.println("  LongStream.range(100, 105): " + longs.boxed().toList());
        System.out.println();
    }

    // ================================================================
    // PART 2: SummaryStatistics — 5 stats in one pass
    // ================================================================
    static void part2_SummaryStatistics() {
        System.out.println("┌─ PART 2: SummaryStatistics ──────────────────────────────────┐");

        int[] salaries = {85000, 72000, 95000, 110000, 105000, 130000, 65000, 115000, 78000, 92000};

        IntSummaryStatistics stats = Arrays.stream(salaries).summaryStatistics();
        System.out.println("  Input: " + Arrays.toString(salaries));
        System.out.println("  Count: " + stats.getCount());
        System.out.println("  Sum:   " + stats.getSum());
        System.out.println("  Min:   " + stats.getMin());
        System.out.println("  Avg:   " + stats.getAverage());
        System.out.println("  Max:   " + stats.getMax());
        System.out.println("  All 5 stats computed in a single stream pass.");
        System.out.println();
    }

    // ================================================================
    // PART 3: Boxing cost — IntStream vs Stream<Integer>
    // ================================================================
    static void part3_BoxingVsPrimitive_Perf() {
        System.out.println("┌─ PART 3: Boxing Cost — IntStream vs Stream<Integer> ─────────┐");

        int size = 5_000_000;
        int[] data = ThreadLocalRandom.current().ints(size, 0, 1000).toArray();

        // Warmup
        Arrays.stream(data).sum();

        // Boxing stream: Stream<Integer>
        // We intentionally allocate Stream<Integer> by boxing
        long bStart = System.nanoTime();
        int boxedSum = IntStream.of(data)
            .boxed()
            .mapToInt(Integer::intValue)
            .map(n -> n * 2)
            .sum();
        long bTime = (System.nanoTime() - bStart) / 1_000_000;

        // Primitive stream: IntStream
        long pStart = System.nanoTime();
        int primSum = Arrays.stream(data)
            .map(n -> n * 2)
            .sum();
        long pTime = (System.nanoTime() - pStart) / 1_000_000;

        System.out.println("  N=" + size + " elements, map(n -> n*2) then sum");
        System.out.println("  Stream<Integer> (boxing):   " + bTime + "ms   sum=" + boxedSum);
        System.out.println("  IntStream (no boxing):      " + pTime + "ms   sum=" + primSum);
        System.out.println("  Boxing creates Integer objects → GC pressure + cache misses.");
        System.out.println();
    }

    // ================================================================
    // PART 4: Conversion between stream types
    // ================================================================
    static void part4_ConversionBetweenStreamTypes() {
        System.out.println("┌─ PART 4: Stream Type Conversions ────────────────────────────┐");

        // Stream<T> → IntStream
        List<String> words = List.of("apple", "banana", "kiwi");
        IntStream lengths = words.stream().mapToInt(String::length);
        System.out.println("  Stream<String> → mapToInt(String::length): " + lengths.boxed().toList());

        // IntStream → Stream<T>
        Stream<String> stringified = IntStream.range(1, 5)
            .mapToObj(n -> "Item-" + n);
        System.out.println("  IntStream → mapToObj(n -> \"Item-\" + n): " + stringified.toList());

        // IntStream → Stream<Integer> (boxed)
        Stream<Integer> boxed = IntStream.range(1, 5).boxed();
        System.out.println("  IntStream → boxed(): " + boxed.toList());

        // Stream<String> → flatMapToInt (1-to-many)
        IntStream charCounts = words.stream()
            .flatMapToInt(w -> w.chars());
        System.out.println("  Stream<String> → flatMapToInt(w.chars()): total chars = " + charCounts.count());

        // Stream<Person> → DoubleStream (e.g., salaries)
        Stream<String> strs = Stream.of("10.5", "20.5", "30.5");
        double avg = strs.mapToDouble(Double::parseDouble).average().orElse(0);
        System.out.println("  Stream<String> → mapToDouble → avg: " + avg);
        System.out.println();
    }

    // ================================================================
    // PART 5: Stream.iterate — Java 8 vs Java 9
    // ================================================================
    static void part5_StreamIterate_Java8VsJava9() {
        System.out.println("┌─ PART 5: Stream.iterate — Java 8 vs Java 9 ──────────────────┐");

        // Java 8: infinite (requires limit)
        List<Integer> java8Style = Stream.iterate(0, n -> n + 2)
            .limit(10)
            .toList();
        System.out.println("  Java 8: Stream.iterate(0, n->n+2).limit(10) → " + java8Style);

        // Java 9: finite with predicate (hasNext)
        List<Integer> java9Style = Stream.iterate(0, n -> n < 20, n -> n + 2)
            .toList();
        System.out.println("  Java 9: Stream.iterate(0, n->n<20, n->n+2) → " + java9Style);

        // Why the Java 9 overload?
        // Without predicate, you must remember to limit() or it's infinite.
        // With predicate, the stream self-terminates — cleaner, less error-prone.

        // Real-world: Fibonacci
        List<Integer> fib = Stream.iterate(
                new int[]{0, 1},
                arr -> arr[0] < 100,
                arr -> new int[]{arr[1], arr[0] + arr[1]}
            )
            .map(arr -> arr[0])
            .toList();
        System.out.println("  Fibonacci (<100): " + fib);

        // Java 8 way (infinite + limit)
        List<Integer> fib8 = Stream.iterate(new int[]{0, 1}, arr -> new int[]{arr[1], arr[0] + arr[1]})
            .limit(12)
            .map(arr -> arr[0])
            .toList();
        System.out.println("  Fibonacci (12 terms): " + fib8);
        System.out.println();
    }

    // ================================================================
    // PART 6: Stream.generate (stateless infinite)
    // ================================================================
    static void part6_StreamGenerate() {
        System.out.println("┌─ PART 6: Stream.generate — Stateless Infinite ───────────────┐");

        // Random numbers
        List<Integer> rand5 = Stream.generate(() -> ThreadLocalRandom.current().nextInt(100))
            .limit(5)
            .toList();
        System.out.println("  generate(random).limit(5): " + rand5);

        // UUIDs
        List<String> ids = Stream.generate(() -> java.util.UUID.randomUUID().toString().substring(0, 8))
            .limit(3)
            .toList();
        System.out.println("  generate(UUID).limit(3): " + ids);

        // Constant
        List<String> tenHellos = Stream.generate(() -> "hello")
            .limit(10)
            .toList();
        System.out.println("  generate(\"hello\").limit(10): " + tenHellos.size() + " items");

        // generate vs iterate: generate has no memory of previous value — stateless.
        // iterate has memory — each call depends on the previous result.
        System.out.println("  Key diff: generate is stateless, iterate is stateful (carries prev value).");
        System.out.println();
    }

    // ================================================================
    // PART 7: takeWhile / dropWhile (Java 9)
    // ================================================================
    static void part7_TakeWhile_DropWhile() {
        System.out.println("┌─ PART 7: takeWhile & dropWhile (Java 9) ─────────────────────┐");

        List<Integer> sorted = List.of(1, 2, 3, 5, 8, 13, 21, 34, 55);

        // takeWhile: take elements while true, then stop FOREVER
        List<Integer> small = sorted.stream()
            .takeWhile(n -> n < 10)
            .toList();
        System.out.println("  takeWhile(n<10): " + small + "  (stops at 13, ignores rest)");

        // dropWhile: drop elements while true, then stream the rest
        List<Integer> large = sorted.stream()
            .dropWhile(n -> n < 10)
            .toList();
        System.out.println("  dropWhile(n<10): " + large + "  (drops until 13, then streams rest)");

        // Filter would process ALL elements — takeWhile stops early
        List<Integer> filtered = sorted.stream()
            .filter(n -> n < 10)
            .toList();
        System.out.println("  filter(n<10):    " + filtered + "  (processes ALL 9 elements)");

        // Unsorted source: takeWhile is still correct but less useful
        List<Integer> unsorted = List.of(3, 1, 4, 1, 5, 9, 2, 6);
        List<Integer> twg = unsorted.stream()
            .takeWhile(n -> n < 5)
            .toList();
        System.out.println("  takeWhile(n<5) on unsorted: " + twg + "  (stops at first 5)");
        System.out.println();
    }

    // ================================================================
    // PART 8: reduce() deep dive
    // ================================================================
    static void part8_ReduceDeepDive() {
        System.out.println("┌─ PART 8: reduce() Deep Dive ──────────────────────────────────┐");

        List<Integer> nums = List.of(1, 2, 3, 4, 5);
        List<Integer> empty = List.of();

        // reduce(identity, accumulator) — always returns T
        int sumWithId = nums.stream().reduce(0, Integer::sum);
        int emptySumWithId = empty.stream().reduce(0, Integer::sum);
        System.out.println("  reduce(0, Integer::sum) on 1..5: " + sumWithId);
        System.out.println("  reduce(0, Integer::sum) on empty: " + emptySumWithId + "  (returns identity)");

        // reduce(accumulator) — returns Optional<T>
        Optional<Integer> sumOpt = nums.stream().reduce(Integer::sum);
        Optional<Integer> emptyOpt = empty.stream().reduce(Integer::sum);
        System.out.println("  reduce(Integer::sum) on 1..5: " + sumOpt.orElse(0));
        System.out.println("  reduce(Integer::sum) on empty:  " + emptyOpt + "  (Optional.empty)");

        // reduce with identity + accumulator + combiner — parallel-ready
        int parallelSum = nums.parallelStream()
            .reduce(0, Integer::sum, Integer::sum);
        System.out.println("  reduce(0, sum, sum) parallel on 1..5: " + parallelSum);

        // Identity must be truly an identity for the operation:
        //   accumulator.apply(identity, t) == t                      for all t
        //   combiner.apply(u, identity) == u                         for all u
        //   identity + 0 = identity ✓ for sum
        //   identity * 1 = identity ✓ for product
        //   identity + "" = identity ✓ for string concat

        // Product (identity = 1)
        int product = nums.stream().reduce(1, (a, b) -> a * b);
        System.out.println("  Product reduce(1, (a,b)->a*b) on 1..5: " + product);
        System.out.println();
    }

    // ================================================================
    // PART 9: flatMap vs map — the key distinction
    // ================================================================
    static void part9_FlatmapVsMap() {
        System.out.println("┌─ PART 9: flatMap vs map — The Key Distinction ───────────────┐");

        List<List<Integer>> nested = List.of(
            List.of(1, 2),
            List.of(3, 4),
            List.of(5, 6)
        );

        // map: each list → stream → Stream<Stream<Integer>>
        // You get nested streams — not what you want
        List<Stream<Integer>> mapped = nested.stream()
            .map(List::stream)
            .toList();
        System.out.println("  map(List::stream):  Stream<Stream<Integer>> — " + mapped.size() + " inner streams");

        // flatMap: each list → stream → flattens into Stream<Integer>
        List<Integer> flatMapped = nested.stream()
            .flatMap(List::stream)
            .toList();
        System.out.println("  flatMap(List::stream): " + flatMapped + " — flat list");

        // Multi-level flattening
        List<String> sentences = List.of(
            "hello world",
            "java streams are powerful",
            "flatmap demystified"
        );
        List<String> words = sentences.stream()
            .flatMap(sentence -> Arrays.stream(sentence.split(" ")))
            .toList();
        System.out.println("  Sentences → words via flatMap: " + words);

        // flatMap with filter
        List<String> longWords = sentences.stream()
            .flatMap(s -> Arrays.stream(s.split(" ")))
            .filter(w -> w.length() > 4)
            .distinct()
            .toList();
        System.out.println("  Words with length>4 distinct: " + longWords);

        // map + flatMap chained
        List<Person> people = List.of(
            new Person("Alice", List.of("Java", "Python")),
            new Person("Bob", List.of("Kotlin", "Go")),
            new Person("Charlie", List.of("Java", "Rust", "Go"))
        );
        List<String> uniqueSkills = people.stream()
            .flatMap(p -> p.skills().stream())
            .distinct()
            .sorted()
            .toList();
        System.out.println("  Unique skills across people: " + uniqueSkills);
        System.out.println();
    }

    record Person(String name, List<String> skills) {}
}
