package com.kowshik.streamplayground;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class ParallelStreamDeepDiveDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            PARALLEL STREAMS DEEP DIVE DEMO                   ║");
        System.out.println("║  Cores: " + Runtime.getRuntime().availableProcessors() + " | CommonPool size: "
            + ForkJoinPool.commonPool().getParallelism() + "                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        part1_WhenParallelWins();
        part2_WhenParallelLoses();
        part3_SharedMutableStatePitfall();
        part4_FindFirstVsFindAny();
        part5_OrderedVsUnordered();
        part6_ReduceCombinerInParallel();
        part7_LinkedListParallelTrap();
    }

    // ================================================================
    // PART 1: When parallel wins — large data, CPU-bound, splittable
    // ================================================================
    static void part1_WhenParallelWins() {
        System.out.println("┌─ PART 1: When Parallel Wins (Large Data + CPU-Bound) ────────┐");

        int size = 10_000_000;
        List<Integer> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            data.add(ThreadLocalRandom.current().nextInt(1000));
        }

        // Sequential
        long seqStart = System.nanoTime();
        long seqSum = data.stream()
            .mapToInt(Integer::intValue)
            .map(n -> (int) Math.sqrt(n * n))  // CPU work
            .sum();
        long seqTime = (System.nanoTime() - seqStart) / 1_000_000;

        // Parallel
        long parStart = System.nanoTime();
        long parSum = data.parallelStream()
            .mapToInt(Integer::intValue)
            .map(n -> (int) Math.sqrt(n * n))
            .sum();
        long parTime = (System.nanoTime() - parStart) / 1_000_000;

        System.out.println("  N=" + size + " elements, ArrayList source (cheaply splittable)");
        System.out.println("  Sequential: " + seqTime + "ms | sum=" + seqSum);
        System.out.println("  Parallel:   " + parTime + "ms | sum=" + parSum);
        double speedup = (double) seqTime / parTime;
        System.out.println("  Speedup: " + String.format("%.1f", speedup) + "x"
            + (speedup > 1.5 ? " ✓" : speedup < 1.0 ? " (parallel slower!)" : ""));
        System.out.println();
    }

    // ================================================================
    // PART 2: When parallel loses — small data, I/O-bound, non-splittable
    // ================================================================
    static void part2_WhenParallelLoses() {
        System.out.println("┌─ PART 2: When Parallel Loses ────────────────────────────────┐");

        // Small data — overhead > gain
        List<Integer> smallData = IntStream.rangeClosed(1, 100).boxed().toList();

        long seqStart = System.nanoTime();
        long seqSum = smallData.stream().mapToLong(Integer::longValue).sum();
        long seqTime = (System.nanoTime() - seqStart) / 1_000_000;

        long parStart = System.nanoTime();
        long parSum = smallData.parallelStream().mapToLong(Integer::longValue).sum();
        long parTime = (System.nanoTime() - parStart) / 1_000_000;

        System.out.println("  N=100 elements (small data)");
        System.out.println("  Sequential: " + seqTime + "ms");
        System.out.println("  Parallel:   " + parTime + "ms");
        System.out.println("  Overhead kills parallelism for small datasets.");
        System.out.println();

        // Simulated I/O — blocking inside fork-join pool = starvation
        System.out.println("  I/O simulation: Thread.sleep(10) per element, N=50");
        List<Integer> ioData = IntStream.rangeClosed(1, 50).boxed().toList();
        long ioSeqStart = System.nanoTime();
        ioData.stream().forEach(n -> {
            try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        long ioSeqTime = (System.nanoTime() - ioSeqStart) / 1_000_000;
        System.out.println("  Sequential: ~" + ioSeqTime + "ms");
        System.out.println("  Parallel would be WORSE — blocking common pool threads starves other parallel tasks.");
        System.out.println("  Use virtual threads or CompletableFuture for I/O parallelism instead.");
        System.out.println();
    }

    // ================================================================
    // PART 3: Shared Mutable State — THE most common parallel stream bug
    // ================================================================
    static void part3_SharedMutableStatePitfall() {
        System.out.println("┌─ PART 3: Shared Mutable State (The Classic Bug) ─────────────┐");

        // WRONG — non-thread-safe ArrayList
        List<Integer> wrongList = new ArrayList<>();
        try {
            IntStream.range(0, 10_000)
                .parallel()
                .filter(n -> n % 2 == 0)
                .forEach(wrongList::add);
            System.out.println("  WRONG (ArrayList::add in parallel forEach):");
            System.out.println("    Expected: 5000 elements, Got: " + wrongList.size() + " elements");
            int expectedSum = IntStream.range(0, 10_000).filter(n -> n % 2 == 0).sum();
            int actualSum = wrongList.stream().mapToInt(Integer::intValue).sum();
            System.out.println("    Expected sum: " + expectedSum + ", Actual sum: " + actualSum);
        } catch (Exception e) {
            System.out.println("  WRONG: Exception — " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // RIGHT — use collector
        List<Integer> rightList = IntStream.range(0, 10_000)
            .parallel()
            .filter(n -> n % 2 == 0)
            .boxed()
            .collect(Collectors.toList());
        System.out.println("  RIGHT (Collectors.toList): Got " + rightList.size() + " elements ✓");

        // RIGHT — use thread-safe collection
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, 10_000)
            .parallel()
            .filter(n -> n % 2 == 0)
            .forEach(syncList::add);
        System.out.println("  OK (Collections.synchronizedList): Got " + syncList.size()
            + " elements (works but slower than collector)");
        System.out.println();
    }

    // ================================================================
    // PART 4: findFirst vs findAny — order matters for perf
    // ================================================================
    static void part4_FindFirstVsFindAny() {
        System.out.println("┌─ PART 4: findFirst vs findAny ───────────────────────────────┐");

        List<Integer> data = IntStream.rangeClosed(1, 1_000_000).boxed().toList();

        // findFirst — respects encounter order (forces coordination in parallel)
        long ffStart = System.nanoTime();
        Optional<Integer> firstResult = data.parallelStream()
            .filter(n -> n > 999_900)
            .findFirst();
        long ffTime = (System.nanoTime() - ffStart) / 1_000;

        // findAny — any element from any substream (no coordination)
        long faStart = System.nanoTime();
        Optional<Integer> anyResult = data.parallelStream()
            .filter(n -> n > 999_900)
            .findAny();
        long faTime = (System.nanoTime() - faStart) / 1_000;

        System.out.println("  Parallel filter(n>999900) on 1M elements:");
        System.out.println("    findFirst: " + ffTime + "µs → " + firstResult.orElse(-1));
        System.out.println("    findAny:   " + faTime + "µs → " + anyResult.orElse(-1));
        System.out.println("  findAny is faster because it doesn't enforce order across substreams.");
        System.out.println("  Use findAny unless the first matching element specifically matters.");
        System.out.println();
    }

    // ================================================================
    // PART 5: forEach vs forEachOrdered
    // ================================================================
    static void part5_OrderedVsUnordered() {
        System.out.println("┌─ PART 5: forEach vs forEachOrdered ───────────────────────────┐");

        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // forEach: no ordering guarantee in parallel
        System.out.print("  forEach (parallel):        ");
        data.parallelStream().forEach(n -> System.out.print(n + " "));
        System.out.println();

        // forEachOrdered: guarantees order
        System.out.print("  forEachOrdered (parallel):  ");
        data.parallelStream().forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();

        // forEach sequential: always ordered
        System.out.print("  forEach (sequential):       ");
        data.stream().forEach(n -> System.out.print(n + " "));
        System.out.println();
        System.out.println("  Use forEachOrdered when output order matters in parallel.");
        System.out.println();
    }

    // ================================================================
    // PART 6: reduce() combiner — why identity + accumulator + combiner
    // ================================================================
    static void part6_ReduceCombinerInParallel() {
        System.out.println("┌─ PART 6: reduce() — The Combiner in Parallel ────────────────┐");

        List<String> words = List.of("a", "b", "c", "d", "e", "f", "g", "h");

        // Sequential reduce (combiner unused)
        String seqConcat = words.stream()
            .reduce("", String::concat, String::concat);
        System.out.println("  Sequential reduce: \"" + seqConcat + "\"");

        // Parallel reduce (combiner merges partial results)
        String parConcat = words.parallelStream()
            .reduce("", String::concat, String::concat);
        System.out.println("  Parallel reduce:   \"" + parConcat + "\"");

        // Length computation in parallel
        int totalLength = words.parallelStream()
            .reduce(0,                              // identity
                (len, word) -> len + word.length(),  // accumulator: fold word into count
                Integer::sum                       // combiner: merge two partial counts
            );
        int expectedLength = words.stream().mapToInt(String::length).sum();
        System.out.println("  Parallel length reduce: " + totalLength + " (expected: " + expectedLength + ")");

        // What happens WITHOUT a proper combiner?
        // If combiner != accumulator and you run in parallel: wrong results!
        System.out.println("  Rule: accumulator combines (partialResult, element)");
        System.out.println("        combiner combines (partialResult, partialResult)");
        System.out.println("        They must be the same when T == U and associative.");
        System.out.println();
    }

    // ================================================================
    // PART 7: LinkedList — terrible for parallel
    // ================================================================
    static void part7_LinkedListParallelTrap() {
        System.out.println("┌─ PART 7: LinkedList Parallel Trap ───────────────────────────┐");

        int size = 100_000;
        List<Integer> arrayList = new ArrayList<>(size);
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        // ArrayList parallel
        long alStart = System.nanoTime();
        long alSum = arrayList.parallelStream().mapToLong(Integer::longValue).sum();
        long alTime = (System.nanoTime() - alStart) / 1_000;

        // LinkedList parallel
        long llStart = System.nanoTime();
        long llSum = linkedList.parallelStream().mapToLong(Integer::longValue).sum();
        long llTime = (System.nanoTime() - llStart) / 1_000;

        // LinkedList sequential (for comparison)
        long llSeqStart = System.nanoTime();
        long llSeqSum = linkedList.stream().mapToLong(Integer::longValue).sum();
        long llSeqTime = (System.nanoTime() - llSeqStart) / 1_000;

        System.out.println("  N=" + size + " elements");
        System.out.println("  ArrayList  (parallel):   " + alTime + "µs ✓ (O(1) split by index range)");
        System.out.println("  LinkedList (parallel):   " + llTime + "µs ✗ (O(n) to find midpoint for split)");
        System.out.println("  LinkedList (sequential): " + llSeqTime + "µs");
        System.out.println("  Rule: Never parallelize a LinkedList — sequential is faster.");
        System.out.println();
    }

}
