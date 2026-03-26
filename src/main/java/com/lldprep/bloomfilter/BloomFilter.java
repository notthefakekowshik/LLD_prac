package com.lldprep.bloomfilter;

import com.lldprep.bloomfilter.exception.BloomFilterException;
import com.lldprep.bloomfilter.hash.HashFunction;
import com.lldprep.bloomfilter.hash.MurmurHashFunction;

import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe Bloom Filter implementation for probabilistic membership testing.
 * 
 * Design Patterns Applied:
 * 1. Strategy Pattern - HashFunction is pluggable via interface
 * 2. Builder Pattern - Complex construction with validation
 * 3. Facade Pattern - Simple API hides complex bit manipulation
 * 
 * SOLID Principles:
 * - SRP: BloomFilter only manages membership testing
 * - OCP: New hash functions can be added without modifying this class
 * - DIP: Depends on HashFunction interface, not concrete implementations
 * 
 * Time Complexity: O(k) for add() and mightContain() where k = number of hash functions
 * Space Complexity: O(m) where m = bit array size
 * 
 * @param <T> Type of elements to store
 */
public class BloomFilter<T> {
    
    private final BitSet bitArray;
    private final HashFunction<T> hashFunction;
    private final int bitArraySize;
    private final int numHashFunctions;
    private final int expectedElements;
    private final double falsePositiveRate;
    
    private final ReadWriteLock lock;
    
    private BloomFilter(Builder<T> builder) {
        this.expectedElements = builder.expectedElements;
        this.falsePositiveRate = builder.falsePositiveRate;
        this.hashFunction = builder.hashFunction;
        
        this.bitArraySize = builder.calculateOptimalBitArraySize();
        this.numHashFunctions = builder.calculateOptimalHashCount();
        this.bitArray = new BitSet(bitArraySize);
        
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * Adds an element to the Bloom filter.
     * 
     * Thread-safe: Uses write lock to ensure consistency.
     * 
     * @param element Element to add (null elements are ignored)
     */
    public void add(T element) {
        if (element == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numHashFunctions; i++) {
                int position = getBitPosition(element, i);
                bitArray.set(position);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if an element might be in the set.
     * 
     * Returns:
     * - true: element MIGHT be present (possible false positive)
     * - false: element is DEFINITELY NOT present (no false negatives)
     * 
     * Thread-safe: Uses read lock for concurrent reads.
     * 
     * @param element Element to check
     * @return true if element might exist, false if definitely doesn't exist
     */
    public boolean mightContain(T element) {
        if (element == null) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            for (int i = 0; i < numHashFunctions; i++) {
                int position = getBitPosition(element, i);
                if (!bitArray.get(position)) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Calculates the bit position for a given element and hash function index.
     * 
     * Uses the hash function with different seeds to simulate k independent hash functions.
     * Maps the hash value to a valid bit position using modulo.
     * 
     * @param element Element to hash
     * @param hashIndex Index of the hash function (0 to k-1)
     * @return Bit position in the range [0, bitArraySize)
     */
    private int getBitPosition(T element, int hashIndex) {
        int hash = hashFunction.hash(element, hashIndex);
        return Math.abs(hash % bitArraySize);
    }
    
    public int getExpectedElements() {
        return expectedElements;
    }
    
    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }
    
    public int getBitArraySize() {
        return bitArraySize;
    }
    
    public int getNumHashFunctions() {
        return numHashFunctions;
    }
    
    /**
     * Builder Pattern implementation for BloomFilter construction.
     * 
     * Handles:
     * - Parameter validation
     * - Optimal parameter calculation (bit array size, hash count)
     * - Default values for optional parameters
     * 
     * @param <T> Type of elements to store
     */
    public static class Builder<T> {
        
        private static final double DEFAULT_FALSE_POSITIVE_RATE = 0.01;
        
        private int expectedElements;
        private double falsePositiveRate = DEFAULT_FALSE_POSITIVE_RATE;
        private HashFunction<T> hashFunction = new MurmurHashFunction<>();
        
        /**
         * Sets the expected number of elements to be inserted.
         * Required parameter.
         * 
         * @param expectedElements Expected number of elements (must be > 0)
         * @return Builder instance for method chaining
         */
        public Builder<T> expectedElements(int expectedElements) {
            if (expectedElements <= 0) {
                throw new BloomFilterException("Expected elements must be positive, got: " + expectedElements);
            }
            this.expectedElements = expectedElements;
            return this;
        }
        
        /**
         * Sets the desired false positive rate.
         * Optional parameter (default: 0.01 = 1%).
         * 
         * @param falsePositiveRate Desired false positive rate (must be between 0 and 1)
         * @return Builder instance for method chaining
         */
        public Builder<T> falsePositiveRate(double falsePositiveRate) {
            if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
                throw new BloomFilterException("False positive rate must be between 0 and 1, got: " + falsePositiveRate);
            }
            this.falsePositiveRate = falsePositiveRate;
            return this;
        }
        
        /**
         * Sets the hash function strategy.
         * Optional parameter (default: MurmurHashFunction).
         * 
         * Strategy Pattern: Allows swapping hash algorithms without modifying BloomFilter.
         * 
         * @param hashFunction Hash function implementation
         * @return Builder instance for method chaining
         */
        public Builder<T> hashFunction(HashFunction<T> hashFunction) {
            if (hashFunction == null) {
                throw new BloomFilterException("Hash function cannot be null");
            }
            this.hashFunction = hashFunction;
            return this;
        }
        
        /**
         * Builds the BloomFilter instance.
         * 
         * Validates that expectedElements has been set.
         * Calculates optimal bit array size and number of hash functions.
         * 
         * @return Configured BloomFilter instance
         */
        public BloomFilter<T> build() {
            if (expectedElements <= 0) {
                throw new BloomFilterException("Expected elements must be set before building");
            }
            return new BloomFilter<>(this);
        }
        
        /**
         * Calculates optimal bit array size based on expected elements and false positive rate.
         * 
         * Formula: m = -(n * ln(p)) / (ln(2)^2)
         * where:
         *   m = bit array size
         *   n = expected number of elements
         *   p = desired false positive rate
         * 
         * @return Optimal bit array size
         */
        int calculateOptimalBitArraySize() {
            double numerator = -expectedElements * Math.log(falsePositiveRate);
            double denominator = Math.pow(Math.log(2), 2);
            return (int) Math.ceil(numerator / denominator);
        }
        
        /**
         * Calculates optimal number of hash functions.
         * 
         * Formula: k = (m / n) * ln(2)
         * where:
         *   k = number of hash functions
         *   m = bit array size
         *   n = expected number of elements
         * 
         * @return Optimal number of hash functions
         */
        int calculateOptimalHashCount() {
            int m = calculateOptimalBitArraySize();
            double k = (m / (double) expectedElements) * Math.log(2);
            return Math.max(1, (int) Math.round(k));
        }
    }
}
