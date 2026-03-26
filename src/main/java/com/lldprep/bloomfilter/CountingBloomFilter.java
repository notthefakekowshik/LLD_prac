package com.lldprep.bloomfilter;

import com.lldprep.bloomfilter.exception.BloomFilterException;
import com.lldprep.bloomfilter.hash.HashFunction;
import com.lldprep.bloomfilter.hash.MurmurHashFunction;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * CURVEBALL #1: "Add support for deletion"
 * 
 * Counting Bloom Filter - extends standard Bloom filter with deletion support.
 * 
 * Key Difference: Uses counters instead of bits
 * - Standard Bloom Filter: BitSet (1 bit per position)
 * - Counting Bloom Filter: int[] (counter per position)
 * 
 * Trade-off:
 * - Gain: Can delete elements
 * - Cost: 4-8x more memory (32-bit counters vs 1-bit flags)
 * 
 * Design Pattern: This demonstrates OCP (Open/Closed Principle)
 * - We created a NEW class instead of modifying BloomFilter
 * - Both share the same conceptual interface (add, contains, remove)
 * - Could extract a common interface if needed
 * 
 * @param <T> Type of elements to store
 */
public class CountingBloomFilter<T> {
    
    private final int[] counters;
    private final HashFunction<T> hashFunction;
    private final int arraySize;
    private final int numHashFunctions;
    private final int expectedElements;
    private final double falsePositiveRate;
    
    private final ReadWriteLock lock;
    private int elementCount;
    
    private CountingBloomFilter(Builder<T> builder) {
        this.expectedElements = builder.expectedElements;
        this.falsePositiveRate = builder.falsePositiveRate;
        this.hashFunction = builder.hashFunction;
        
        this.arraySize = builder.calculateOptimalArraySize();
        this.numHashFunctions = builder.calculateOptimalHashCount();
        this.counters = new int[arraySize];
        
        this.lock = new ReentrantReadWriteLock();
        this.elementCount = 0;
    }
    
    /**
     * Adds an element to the filter.
     * Increments counters at all hash positions.
     */
    public void add(T element) {
        if (element == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numHashFunctions; i++) {
                int position = getPosition(element, i);
                counters[position]++;
            }
            elementCount++;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * NEW FUNCTIONALITY: Removes an element from the filter.
     * Decrements counters at all hash positions.
     * 
     * Important: Only call this if you're certain the element was added.
     * Removing a non-existent element can corrupt the filter.
     */
    public void remove(T element) {
        if (element == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            for (int i = 0; i < numHashFunctions; i++) {
                int position = getPosition(element, i);
                if (counters[position] > 0) {
                    counters[position]--;
                } else {
                    throw new BloomFilterException(
                        "Cannot remove element - counter already at 0. " +
                        "Element may not have been added or filter is corrupted."
                    );
                }
            }
            elementCount--;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if an element might be in the set.
     * Returns true only if ALL counters are > 0.
     */
    public boolean mightContain(T element) {
        if (element == null) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            for (int i = 0; i < numHashFunctions; i++) {
                int position = getPosition(element, i);
                if (counters[position] == 0) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Resets all counters to zero.
     * CURVEBALL #2: "Add a reset() method"
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < arraySize; i++) {
                counters[i] = 0;
            }
            elementCount = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private int getPosition(T element, int hashIndex) {
        int hash = hashFunction.hash(element, hashIndex);
        return Math.abs(hash % arraySize);
    }
    
    public int getElementCount() {
        return elementCount;
    }
    
    public int getExpectedElements() {
        return expectedElements;
    }
    
    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }
    
    public int getArraySize() {
        return arraySize;
    }
    
    public int getNumHashFunctions() {
        return numHashFunctions;
    }
    
    /**
     * Builder for CountingBloomFilter.
     * Same pattern as BloomFilter.Builder for consistency.
     */
    public static class Builder<T> {
        
        private static final double DEFAULT_FALSE_POSITIVE_RATE = 0.01;
        
        private int expectedElements;
        private double falsePositiveRate = DEFAULT_FALSE_POSITIVE_RATE;
        private HashFunction<T> hashFunction = new MurmurHashFunction<>();
        
        public Builder<T> expectedElements(int expectedElements) {
            if (expectedElements <= 0) {
                throw new BloomFilterException("Expected elements must be positive");
            }
            this.expectedElements = expectedElements;
            return this;
        }
        
        public Builder<T> falsePositiveRate(double falsePositiveRate) {
            if (falsePositiveRate <= 0 || falsePositiveRate >= 1) {
                throw new BloomFilterException("False positive rate must be between 0 and 1");
            }
            this.falsePositiveRate = falsePositiveRate;
            return this;
        }
        
        public Builder<T> hashFunction(HashFunction<T> hashFunction) {
            if (hashFunction == null) {
                throw new BloomFilterException("Hash function cannot be null");
            }
            this.hashFunction = hashFunction;
            return this;
        }
        
        public CountingBloomFilter<T> build() {
            if (expectedElements <= 0) {
                throw new BloomFilterException("Expected elements must be set");
            }
            return new CountingBloomFilter<>(this);
        }
        
        int calculateOptimalArraySize() {
            double numerator = -expectedElements * Math.log(falsePositiveRate);
            double denominator = Math.pow(Math.log(2), 2);
            return (int) Math.ceil(numerator / denominator);
        }
        
        int calculateOptimalHashCount() {
            int m = calculateOptimalArraySize();
            double k = (m / (double) expectedElements) * Math.log(2);
            return Math.max(1, (int) Math.round(k));
        }
    }
}
