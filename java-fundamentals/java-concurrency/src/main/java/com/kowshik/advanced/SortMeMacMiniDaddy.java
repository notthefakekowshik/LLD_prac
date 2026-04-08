package com.kowshik.advanced;

import java.util.Arrays;
import java.util.Random;

/**
 * Parallel Arrays Sorting Performance Demo
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. Arrays.sort() vs Arrays.parallelSort()
 *    - Arrays.sort(): Uses Dual-Pivot Quicksort for primitives, Timsort for objects
 *    - Arrays.parallelSort(): Uses parallel merge sort (ForkJoinPool)
 *    - parallelSort() faster for large arrays (> 8192 elements)
 *    - parallelSort() leverages multiple CPU cores
 *
 * 2. Common Interview Questions:
 *    - When should you use parallelSort() over sort()?
 *    - What algorithm does Arrays.sort() use?
 *    - Explain time complexity: O(n log n) for both
 *    - What is the threshold for parallelSort() to be beneficial?
 *    - How does parallelSort() work internally? (ForkJoinPool)
 *    - Is Arrays.sort() stable for primitives? (No, but yes for objects with Timsort)
 *
 * 3. Performance Considerations:
 *    - Small arrays: sort() is faster (less overhead)
 *    - Large arrays: parallelSort() is faster (parallel processing)
 *    - Threshold: ~8192 elements (varies by JVM)
 *    - Depends on: Array size, CPU cores, data characteristics
 *
 * 4. Use Cases:
 *    - Sorting large datasets
 *    - Big data processing
 *    - Performance benchmarking
 *    - Parallel algorithm demonstrations
 *
 * This demo shows performance comparison for sorting 50 million integers.
 */
public class SortMeMacMiniDaddy {
    // Number of elements to sort
    static final int N = 50_000_000;
    // Seed for reproducible runs
    static final long SEED = 123456789L;

    /**
     * Benchmarks sorting performance on a large integer array.
     * Useful for comparing hardware performance and understanding parallel sorting benefits.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.out.printf("Sorting %,d integers...%n%n", N);

        // Sequential sort
        int[] arr1 = makeRandomArray(N, SEED);
        long t0 = System.nanoTime();
        Arrays.sort(arr1);
        long t1 = System.nanoTime();
        long sequentialTime = t1 - t0;

        long millis = sequentialTime / 1_000_000;
        double seconds = sequentialTime / 1_000_000_000.0;
        System.out.printf("Sequential sort (Arrays.sort): %,d ms (%.3f s)%n", millis, seconds);

        // Parallel sort
        int[] arr2 = makeRandomArray(N, SEED);
        long t2 = System.nanoTime();
        Arrays.parallelSort(arr2);
        long t3 = System.nanoTime();
        long parallelTime = t3 - t2;

        millis = parallelTime / 1_000_000;
        seconds = parallelTime / 1_000_000_000.0;
        System.out.printf("Parallel sort (Arrays.parallelSort): %,d ms (%.3f s)%n", millis, seconds);

        // Performance comparison
        double speedup = (double) sequentialTime / parallelTime;
        System.out.printf("%nSpeedup: %.2fx faster with parallel sort%n", speedup);
        System.out.printf("Available processors: %d%n", Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates an array of random integers.
     *
     * @param n    the size of the array
     * @param seed the random seed for reproducibility
     * @return array of random integers
     */
    static int[] makeRandomArray(int n, long seed) {
        Random rnd = new Random(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt();
        }
        return a;
    }
}
