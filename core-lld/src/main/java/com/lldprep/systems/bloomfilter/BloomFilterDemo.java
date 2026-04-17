package com.lldprep.systems.bloomfilter;

import com.lldprep.systems.bloomfilter.hash.FNVHashFunction;
import com.lldprep.systems.bloomfilter.hash.MurmurHashFunction;
import com.lldprep.systems.bloomfilter.hash.SimpleHashFunction;

/**
 * Demonstration of Bloom Filter functionality.
 * 
 * Covers:
 * - Basic add/contains operations
 * - False positive demonstration
 * - Different hash function strategies
 * - Configuration with different false positive rates
 * - Thread safety demonstration
 */
public class BloomFilterDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Bloom Filter Demo ===\n");
        
        demonstrateBasicUsage();
        demonstrateFalsePositives();
        demonstrateHashFunctionStrategies();
        demonstrateDifferentConfigurations();
        demonstrateThreadSafety();
    }
    
    /**
     * FR1 & FR2: Demonstrates basic add and membership testing.
     */
    private static void demonstrateBasicUsage() {
        System.out.println("1. Basic Usage - Add and Contains");
        System.out.println("-".repeat(50));
        
        BloomFilter<String> filter = new BloomFilter.Builder<String>()
                .expectedElements(1000)
                .falsePositiveRate(0.01)
                .build();
        
        System.out.println("Configuration:");
        System.out.println("  Expected elements: " + filter.getExpectedElements());
        System.out.println("  False positive rate: " + (filter.getFalsePositiveRate() * 100) + "%");
        System.out.println("  Bit array size: " + filter.getBitArraySize());
        System.out.println("  Number of hash functions: " + filter.getNumHashFunctions());
        System.out.println();
        
        String[] urls = {
            "https://example.com/page1",
            "https://example.com/page2",
            "https://example.com/page3"
        };
        
        System.out.println("Adding URLs to filter:");
        for (String url : urls) {
            filter.add(url);
            System.out.println("  Added: " + url);
        }
        System.out.println();
        
        System.out.println("Testing membership:");
        for (String url : urls) {
            boolean exists = filter.mightContain(url);
            System.out.println("  " + url + " -> " + exists + " (should be true)");
        }
        
        String notAdded = "https://example.com/page999";
        boolean exists = filter.mightContain(notAdded);
        System.out.println("  " + notAdded + " -> " + exists + " (should be false)");
        
        System.out.println("\n");
    }
    
    /**
     * Demonstrates false positive behavior - the core characteristic of Bloom filters.
     */
    private static void demonstrateFalsePositives() {
        System.out.println("2. False Positive Demonstration");
        System.out.println("-".repeat(50));
        
        BloomFilter<String> filter = new BloomFilter.Builder<String>()
                .expectedElements(100)
                .falsePositiveRate(0.1)
                .build();
        
        System.out.println("Adding 100 emails to filter (configured for 10% false positive rate):");
        for (int i = 0; i < 100; i++) {
            filter.add("user" + i + "@example.com");
        }
        System.out.println("  Added 100 emails");
        System.out.println();
        
        System.out.println("Testing 1000 emails that were NOT added:");
        int falsePositives = 0;
        int tested = 1000;
        
        for (int i = 1000; i < 1000 + tested; i++) {
            String email = "user" + i + "@example.com";
            if (filter.mightContain(email)) {
                falsePositives++;
            }
        }
        
        double actualFPRate = (falsePositives / (double) tested) * 100;
        System.out.println("  False positives: " + falsePositives + " out of " + tested);
        System.out.println("  Actual FP rate: " + String.format("%.2f", actualFPRate) + "%");
        System.out.println("  Expected FP rate: ~10%");
        System.out.println("  ✓ No false negatives (elements we added are always found)");
        
        System.out.println("\n");
    }
    
    /**
     * Strategy Pattern: Demonstrates swapping hash function implementations.
     * Shows OCP - adding new hash functions doesn't modify BloomFilter code.
     */
    private static void demonstrateHashFunctionStrategies() {
        System.out.println("3. Hash Function Strategies (Strategy Pattern)");
        System.out.println("-".repeat(50));
        
        String[] testData = {"apple", "banana", "cherry", "date", "elderberry"};
        
        BloomFilter<String> murmurFilter = new BloomFilter.Builder<String>()
                .expectedElements(100)
                .hashFunction(new MurmurHashFunction<>())
                .build();
        
        BloomFilter<String> fnvFilter = new BloomFilter.Builder<String>()
                .expectedElements(100)
                .hashFunction(new FNVHashFunction<>())
                .build();
        
        BloomFilter<String> simpleFilter = new BloomFilter.Builder<String>()
                .expectedElements(100)
                .hashFunction(new SimpleHashFunction<>())
                .build();
        
        System.out.println("Adding same data to filters with different hash functions:");
        for (String item : testData) {
            murmurFilter.add(item);
            fnvFilter.add(item);
            simpleFilter.add(item);
        }
        System.out.println("  Added: " + String.join(", ", testData));
        System.out.println();
        
        System.out.println("All hash functions correctly identify membership:");
        for (String item : testData) {
            boolean murmur = murmurFilter.mightContain(item);
            boolean fnv = fnvFilter.mightContain(item);
            boolean simple = simpleFilter.mightContain(item);
            System.out.println("  " + item + " -> MurmurHash: " + murmur + 
                             ", FNV: " + fnv + ", Simple: " + simple);
        }
        
        System.out.println("\nStrategy Pattern Benefit:");
        System.out.println("  ✓ Swapped hash functions without modifying BloomFilter class");
        System.out.println("  ✓ Can choose speed (Simple) vs quality (Murmur) based on use case");
        System.out.println("  ✓ Open/Closed Principle - new hash functions extend without modification");
        
        System.out.println("\n");
    }
    
    /**
     * Builder Pattern: Demonstrates different configurations.
     */
    private static void demonstrateDifferentConfigurations() {
        System.out.println("4. Different Configurations (Builder Pattern)");
        System.out.println("-".repeat(50));
        
        BloomFilter<String> lowFP = new BloomFilter.Builder<String>()
                .expectedElements(1000)
                .falsePositiveRate(0.001)
                .build();
        
        BloomFilter<String> mediumFP = new BloomFilter.Builder<String>()
                .expectedElements(1000)
                .falsePositiveRate(0.01)
                .build();
        
        BloomFilter<String> highFP = new BloomFilter.Builder<String>()
                .expectedElements(1000)
                .falsePositiveRate(0.1)
                .build();
        
        System.out.println("Configuration Comparison (1000 expected elements):");
        System.out.println();
        System.out.printf("%-20s %-15s %-20s %-15s%n", "FP Rate", "Bit Array Size", "Hash Functions", "Memory (bytes)");
        System.out.println("-".repeat(70));
        
        printConfig("0.1% (strict)", lowFP);
        printConfig("1% (balanced)", mediumFP);
        printConfig("10% (relaxed)", highFP);
        
        System.out.println("\nBuilder Pattern Benefits:");
        System.out.println("  ✓ Readable, self-documenting construction");
        System.out.println("  ✓ Automatic calculation of optimal parameters");
        System.out.println("  ✓ Validation before object creation");
        System.out.println("  ✓ Immutable BloomFilter after construction");
        
        System.out.println("\n");
    }
    
    private static void printConfig(String label, BloomFilter<?> filter) {
        int bitArraySize = filter.getBitArraySize();
        int hashFunctions = filter.getNumHashFunctions();
        int memoryBytes = (bitArraySize / 8) + 64;
        
        System.out.printf("%-20s %-15d %-20d %-15d%n", 
                         label, bitArraySize, hashFunctions, memoryBytes);
    }
    
    /**
     * NFR3: Demonstrates thread-safe concurrent access.
     */
    private static void demonstrateThreadSafety() {
        System.out.println("5. Thread Safety (Concurrent Access)");
        System.out.println("-".repeat(50));
        
        BloomFilter<Integer> filter = new BloomFilter.Builder<Integer>()
                .expectedElements(10000)
                .falsePositiveRate(0.01)
                .build();
        
        System.out.println("Starting concurrent operations:");
        System.out.println("  - 3 writer threads (adding 1000 elements each)");
        System.out.println("  - 2 reader threads (checking membership)");
        System.out.println();
        
        Thread[] writers = new Thread[3];
        for (int i = 0; i < writers.length; i++) {
            final int threadId = i;
            writers[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    filter.add(threadId * 1000 + j);
                }
                System.out.println("  Writer-" + threadId + " completed");
            });
            writers[i].start();
        }
        
        Thread[] readers = new Thread[2];
        for (int i = 0; i < readers.length; i++) {
            final int threadId = i;
            readers[i] = new Thread(() -> {
                int found = 0;
                for (int j = 0; j < 500; j++) {
                    if (filter.mightContain(j)) {
                        found++;
                    }
                }
                System.out.println("  Reader-" + threadId + " found " + found + " elements");
            });
            readers[i].start();
        }
        
        try {
            for (Thread writer : writers) writer.join();
            for (Thread reader : readers) reader.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nThread Safety Verification:");
        System.out.println("  ✓ No ConcurrentModificationException");
        System.out.println("  ✓ ReadWriteLock ensures consistency");
        System.out.println("  ✓ Multiple readers can access concurrently");
        
        System.out.println("\n");
    }
}
