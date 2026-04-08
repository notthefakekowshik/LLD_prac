package com.kowshik.atomics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

/**
 * Atomic Variables Demo - Lock-Free Operations Tutorial
 *
 * Atomic classes provide lock-free thread-safe operations on single variables
 * using compare-and-swap (CAS) operations at the hardware level.
 *
 * Key Points:
 * - Lock-free, non-blocking synchronization
 * - Better performance than synchronized for single variable updates
 * - CAS (Compare-And-Swap) based operations
 * - Volatile semantics + atomic updates
 *
 * Common Atomic Classes:
 * - AtomicInteger, AtomicLong, AtomicBoolean
 * - AtomicReference
 * - AtomicIntegerArray, AtomicLongArray, AtomicReferenceArray
 * - LongAdder, LongAccumulator (Java 8+)
 *
 * Use Cases:
 * - Counters and statistics
 * - Non-blocking algorithms
 * - High-performance concurrent updates
 * - Lock-free data structures
 */
public class AtomicVariablesDemo {

    private static final Logger logger = LoggerFactory.getLogger(AtomicVariablesDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== Atomic Variables Demo ===\n");

        // Demo 1: AtomicInteger vs regular int
        compareAtomicVsRegularDemo();

        // Thread.sleep(1000);

        // // Demo 2: Various atomic operations
        // atomicOperationsDemo();

        // Thread.sleep(1000);

        // // Demo 3: AtomicReference
        // atomicReferenceDemo();

        // Thread.sleep(1000);

        // // Demo 4: LongAdder for high contention
        // longAdderDemo();
    }

    /**
     * Demo 1: Compare atomic vs non-atomic operations
     */
    private static void compareAtomicVsRegularDemo() throws InterruptedException {
        logger.info("Demo 1: Atomic vs Regular Counter");

        int iterations = 1000;
        int numThreads = 10;

        // Regular counter (thread-unsafe)
        Counter regularCounter = new Counter();

        // Atomic counter (thread-safe)
        AtomicCounter atomicCounter = new AtomicCounter();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Test regular counter
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    regularCounter.increment();
                }
            });
        }

        // Test atomic counter
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    atomicCounter.increment();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        logger.info("Regular counter (expected: {}, actual: {})",
                numThreads * iterations, regularCounter.get());
        logger.info("Atomic counter (expected: {}, actual: {})",
                numThreads * iterations, atomicCounter.get());
        logger.info("");
    }

    /**
     * Demo 2: Various atomic operations
     */
    private static void atomicOperationsDemo() throws InterruptedException {
        logger.info("Demo 2: Various Atomic Operations");

        AtomicInteger atomicInt = new AtomicInteger(10);
        logger.info("Initial value: {}", atomicInt.get());

        // Basic operations
        logger.info("incrementAndGet(): {}", atomicInt.incrementAndGet()); // 11
        logger.info("getAndIncrement(): {}", atomicInt.getAndIncrement()); // 11, then 12
        logger.info("Current value: {}", atomicInt.get()); // 12

        logger.info("addAndGet(5): {}", atomicInt.addAndGet(5)); // 17
        logger.info("getAndAdd(3): {}", atomicInt.getAndAdd(3)); // 17, then 20

        // Compare and Set (CAS)
        boolean success = atomicInt.compareAndSet(20, 100);
        logger.info("compareAndSet(20, 100): {} -> {}", success, atomicInt.get());

        success = atomicInt.compareAndSet(50, 200); // Will fail
        logger.info("compareAndSet(50, 200): {} -> {}", success, atomicInt.get());

        // Update and accumulate
        atomicInt.updateAndGet(x -> x * 2);
        logger.info("updateAndGet(x * 2): {}", atomicInt.get());

        atomicInt.accumulateAndGet(10, Integer::sum);
        logger.info("accumulateAndGet(10, sum): {}", atomicInt.get());

        logger.info("");
    }

    /**
     * Demo 3: AtomicReference for object updates
     */
    private static void atomicReferenceDemo() throws InterruptedException {
        logger.info("Demo 3: AtomicReference");

        AtomicReference<User> currentUser = new AtomicReference<>(new User("Alice", 25));
        logger.info("Initial user: {}", currentUser.get());

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Multiple threads trying to update user
        executor.submit(() -> {
            User oldUser = currentUser.get();
            User newUser = new User(oldUser.name, oldUser.age + 1);
            if (currentUser.compareAndSet(oldUser, newUser)) {
                logger.info("Thread 1: Updated age to {}", newUser.age);
            }
        });

        executor.submit(() -> {
            currentUser.updateAndGet(user -> new User("Bob", user.age));
            logger.info("Thread 2: Changed name to Bob");
        });

        executor.submit(() -> {
            currentUser.accumulateAndGet(
                    new User("Charlie", 30),
                    (old, update) -> new User(update.name, old.age));
            logger.info("Thread 3: Changed name to Charlie");
        });

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        logger.info("Final user: {}", currentUser.get());
        logger.info("");
    }

    /**
     * Demo 4: LongAdder for high contention scenarios
     */
    private static void longAdderDemo() throws InterruptedException {
        logger.info("Demo 4: LongAdder for high contention");

        int iterations = 10000;
        int numThreads = 20;

        // AtomicLong
        AtomicLong atomicLong = new AtomicLong();
        long startAtomic = System.nanoTime();

        ExecutorService executor1 = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            executor1.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    atomicLong.incrementAndGet();
                }
            });
        }
        executor1.shutdown();
        executor1.awaitTermination(5, TimeUnit.SECONDS);
        long atomicTime = System.nanoTime() - startAtomic;

        // LongAdder
        LongAdder longAdder = new LongAdder();
        long startAdder = System.nanoTime();

        ExecutorService executor2 = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            executor2.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    longAdder.increment();
                }
            });
        }
        executor2.shutdown();
        executor2.awaitTermination(5, TimeUnit.SECONDS);
        long adderTime = System.nanoTime() - startAdder;

        logger.info("AtomicLong result: {}, time: {} ms",
                atomicLong.get(), atomicTime / 1_000_000);
        logger.info("LongAdder result: {}, time: {} ms",
                longAdder.sum(), adderTime / 1_000_000);
        logger.info("LongAdder is {}x faster",
                String.format("%.2f", (double) atomicTime / adderTime));
    }

    /**
     * Non-thread-safe counter
     */
    static class Counter {
        private int count = 0;

        public void increment() {
            count++; // Not atomic!
        }

        public int get() {
            return count;
        }
    }

    /**
     * Thread-safe atomic counter
     */
    static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.incrementAndGet();
        }

        public int get() {
            return count.get();
        }
    }

    /**
     * User class for AtomicReference demo
     */
    static class User {
        final String name;
        final int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return String.format("User{name='%s', age=%d}", name, age);
        }
    }

    /**
     * Real-world example: Website statistics tracker
     */
    static class WebsiteStatistics {
        private final AtomicLong pageViews = new AtomicLong();
        private final AtomicLong uniqueVisitors = new AtomicLong();
        private final LongAdder totalRequests = new LongAdder();
        private final AtomicReference<String> mostPopularPage = new AtomicReference<>("home");

        public void recordPageView() {
            pageViews.incrementAndGet();
            totalRequests.increment();
        }

        public void recordUniqueVisitor() {
            uniqueVisitors.incrementAndGet();
        }

        public void updateMostPopularPage(String page) {
            mostPopularPage.set(page);
        }

        public void printStats() {
            logger.info("Statistics:");
            logger.info("  Page views: {}", pageViews.get());
            logger.info("  Unique visitors: {}", uniqueVisitors.get());
            logger.info("  Total requests: {}", totalRequests.sum());
            logger.info("  Most popular: {}", mostPopularPage.get());
        }
    }

    /**
     * Example: Non-blocking stack using AtomicReference
     */
    static class NonBlockingStack<T> {
        private final AtomicReference<Node<T>> head = new AtomicReference<>();

        public void push(T value) {
            Node<T> newHead = new Node<>(value);
            Node<T> oldHead;
            do {
                oldHead = head.get();
                newHead.next = oldHead;
            } while (!head.compareAndSet(oldHead, newHead));
        }

        public T pop() {
            Node<T> oldHead;
            Node<T> newHead;
            do {
                oldHead = head.get();
                if (oldHead == null) {
                    return null;
                }
                newHead = oldHead.next;
            } while (!head.compareAndSet(oldHead, newHead));
            return oldHead.value;
        }

        private static class Node<T> {
            final T value;
            Node<T> next;

            Node(T value) {
                this.value = value;
            }
        }
    }
}
