package com.lldprep.systems.bloomfilter.hash;

/**
 * Strategy Pattern: Interface for hash function implementations.
 * 
 * Different hash functions can be swapped without modifying BloomFilter code.
 * This follows the Open/Closed Principle - new hash algorithms can be added
 * by creating new implementations without changing existing code.
 * 
 * @param <T> Type of elements to be hashed
 */
public interface HashFunction<T> {
    
    /**
     * Generates a hash value for the given element with a seed.
     * 
     * Contract:
     * - Must be deterministic: same element + seed = same hash
     * - Should distribute values uniformly across the integer range
     * - Different seeds should produce different hash values
     * 
     * @param element The element to hash
     * @param seed    Seed value to generate different hash functions
     * @return Hash value as an integer
     */
    int hash(T element, int seed);
}
