package com.kowshik.executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

/**
 * ForkJoinPool Demo - Work-Stealing Algorithm Tutorial
 *
 * ForkJoinPool is designed for divide-and-conquer algorithms using
 * work-stealing.
 * Workers that run out of tasks can "steal" work from other busy workers.
 *
 * Key Points:
 * - Divide-and-conquer parallelism
 * - Work-stealing algorithm for load balancing
 * - RecursiveTask<V> for tasks that return a result
 * - RecursiveAction for tasks with no result
 * - Efficient for recursive problems
 *
 * Use Cases:
 * - Parallel sorting (MergeSort, QuickSort)
 * - Tree/graph traversal
 * - Matrix operations
 * - Recursive computations (Fibonacci, factorial)
 * - Parallel stream operations (Java 8+)
 */
public class ForkJoinPoolDemo {

    private static final Logger logger = LoggerFactory.getLogger(ForkJoinPoolDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== ForkJoinPool Demo ===\n");

        // Demo 1: Parallel sum using RecursiveTask
        parallelSumDemo();

        Thread.sleep(1000);

        // Demo 2: Parallel merge sort using RecursiveAction
        parallelMergeSortDemo();

        Thread.sleep(1000);

        // Demo 3: Fibonacci calculation
        fibonacciDemo();

        Thread.sleep(1000);

        // Demo 4: Array processing with threshold
        arrayProcessingDemo();
    }

    /**
     * Demo 1: Parallel sum calculation
     */
    private static void parallelSumDemo() {
        logger.info("Demo 1: Parallel Sum Calculation");

        long[] array = new long[10_000_000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }

        ForkJoinPool pool = ForkJoinPool.commonPool();

        // Sequential sum
        long startSeq = System.nanoTime();
        long sequentialSum = 0;
        for (long num : array) {
            sequentialSum += num;
        }
        long seqTime = System.nanoTime() - startSeq;

        // Parallel sum using ForkJoin
        long startPar = System.nanoTime();
        SumTask task = new SumTask(array, 0, array.length);
        long parallelSum = pool.invoke(task);
        long parTime = System.nanoTime() - startPar;

        logger.info("Sequential sum: {}, time: {} ms", sequentialSum, seqTime / 1_000_000);
        logger.info("Parallel sum: {}, time: {} ms", parallelSum, parTime / 1_000_000);
        logger.info("Speedup: {}x\n", String.format("%.2f", (double) seqTime / parTime));
    }

    /**
     * Demo 2: Parallel merge sort
     */
    private static void parallelMergeSortDemo() {
        logger.info("Demo 2: Parallel Merge Sort");

        int[] array = { 64, 34, 25, 12, 22, 11, 90, 88, 45, 50, 23, 67, 89, 12, 56 };
        logger.info("Original array: {}", arrayToString(array));

        ForkJoinPool pool = new ForkJoinPool();
        MergeSortTask task = new MergeSortTask(array, 0, array.length - 1);
        pool.invoke(task);

        logger.info("Sorted array: {}\n", arrayToString(array));
    }

    /**
     * Demo 3: Fibonacci calculation
     */
    private static void fibonacciDemo() {
        logger.info("Demo 3: Parallel Fibonacci Calculation");

        ForkJoinPool pool = new ForkJoinPool();

        for (int n : new int[] { 10, 20, 30, 35 }) {
            FibonacciTask task = new FibonacciTask(n);
            long result = pool.invoke(task);
            logger.info("Fibonacci({}) = {}", n, result);
        }
        logger.info("");
    }

    /**
     * Demo 4: Array processing with threshold
     */
    private static void arrayProcessingDemo() throws InterruptedException {
        logger.info("Demo 4: Array Processing with Threshold");

        int[] array = new int[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }

        ForkJoinPool pool = new ForkJoinPool();
        ArrayProcessingTask task = new ArrayProcessingTask(array, 0, array.length);
        pool.invoke(task);

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        logger.info("Array processing complete\n");
    }

    /**
     * RecursiveTask for parallel sum calculation
     */
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 10_000;
        private final long[] array;
        private final int start;
        private final int end;

        SumTask(long[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            int length = end - start;

            // Base case: compute directly if small enough
            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }

            // Recursive case: split into subtasks
            int mid = start + length / 2;
            SumTask leftTask = new SumTask(array, start, mid);
            SumTask rightTask = new SumTask(array, mid, end);

            // Fork: asynchronously execute left task
            leftTask.fork();

            // Compute right task directly
            long rightResult = rightTask.compute();

            // Join: wait for left task result
            long leftResult = leftTask.join();

            return leftResult + rightResult;
        }
    }

    /**
     * RecursiveAction for parallel merge sort
     */
    static class MergeSortTask extends RecursiveAction {
        private static final int THRESHOLD = 10;
        private final int[] array;
        private final int left;
        private final int right;

        MergeSortTask(int[] array, int left, int right) {
            this.array = array;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            if (left < right) {
                if (right - left <= THRESHOLD) {
                    // Base case: use simple sort for small arrays
                    insertionSort(array, left, right);
                } else {
                    // Recursive case: divide and conquer
                    int mid = (left + right) / 2;

                    MergeSortTask leftTask = new MergeSortTask(array, left, mid);
                    MergeSortTask rightTask = new MergeSortTask(array, mid + 1, right);

                    // Fork both subtasks
                    invokeAll(leftTask, rightTask);

                    // Merge results
                    merge(array, left, mid, right);
                }
            }
        }

        private void insertionSort(int[] arr, int left, int right) {
            for (int i = left + 1; i <= right; i++) {
                int key = arr[i];
                int j = i - 1;
                while (j >= left && arr[j] > key) {
                    arr[j + 1] = arr[j];
                    j--;
                }
                arr[j + 1] = key;
            }
        }

        private void merge(int[] arr, int left, int mid, int right) {
            int n1 = mid - left + 1;
            int n2 = right - mid;

            int[] leftArray = new int[n1];
            int[] rightArray = new int[n2];

            System.arraycopy(arr, left, leftArray, 0, n1);
            System.arraycopy(arr, mid + 1, rightArray, 0, n2);

            int i = 0, j = 0, k = left;

            while (i < n1 && j < n2) {
                if (leftArray[i] <= rightArray[j]) {
                    arr[k++] = leftArray[i++];
                } else {
                    arr[k++] = rightArray[j++];
                }
            }

            while (i < n1)
                arr[k++] = leftArray[i++];
            while (j < n2)
                arr[k++] = rightArray[j++];
        }
    }

    /**
     * RecursiveTask for Fibonacci calculation
     */
    static class FibonacciTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 10;
        private final int n;

        FibonacciTask(int n) {
            this.n = n;
        }

        @Override
        protected Long compute() {
            if (n <= THRESHOLD) {
                // Base case: compute directly
                return fibonacci(n);
            }

            // Recursive case: fork subtasks
            FibonacciTask f1 = new FibonacciTask(n - 1);
            FibonacciTask f2 = new FibonacciTask(n - 2);

            f1.fork(); // Async execution
            long result2 = f2.compute(); // Direct computation
            long result1 = f1.join(); // Wait for async result

            return result1 + result2;
        }

        private long fibonacci(int n) {
            if (n <= 1)
                return n;
            long prev = 0, curr = 1;
            for (int i = 2; i <= n; i++) {
                long next = prev + curr;
                prev = curr;
                curr = next;
            }
            return curr;
        }
    }

    /**
     * RecursiveAction for array processing
     */
    static class ArrayProcessingTask extends RecursiveAction {
        private static final Logger logger = LoggerFactory.getLogger(ArrayProcessingTask.class);
        private static final int THRESHOLD = 20;
        private final int[] array;
        private final int start;
        private final int end;

        ArrayProcessingTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int length = end - start;

            if (length <= THRESHOLD) {
                // Process directly
                processDirectly();
            } else {
                // Split and fork
                int mid = start + length / 2;
                ArrayProcessingTask left = new ArrayProcessingTask(array, start, mid);
                ArrayProcessingTask right = new ArrayProcessingTask(array, mid, end);

                invokeAll(left, right);
            }
        }

        private void processDirectly() {
            logger.info("Processing range [{}, {}) on thread {}",
                    start, end, Thread.currentThread().getName());

            // Simulate processing
            for (int i = start; i < end; i++) {
                array[i] = array[i] * 2; // Example transformation
            }
        }
    }

    /**
     * Helper method to convert array to string
     */
    private static String arrayToString(int[] array) {
        if (array.length <= 20) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < array.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(array[i]);
            }
            sb.append("]");
            return sb.toString();
        } else {
            return "[" + array.length + " elements]";
        }
    }
}
