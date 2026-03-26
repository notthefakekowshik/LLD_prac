package com.lldprep.bloomfilter.hash;

/**
 * Simple hash function based on Java's built-in hashCode().
 * 
 * Characteristics:
 * - Fastest implementation (delegates to Object.hashCode())
 * - Lowest quality distribution
 * - Suitable for testing or non-critical use cases
 * - NOT recommended for production Bloom filters
 * 
 * Trade-off: Speed vs. Quality
 * Use this when performance is critical and collision rate is acceptable.
 * 
 * @param <T> Type of elements to be hashed
 */
public class SimpleHashFunction<T> implements HashFunction<T> {
    
    @Override
    public int hash(T element, int seed) {
        if (element == null) {
            return 0;
        }
        
        int hash = element.hashCode();
        
        // Mix the seed into the hash to create different hash functions
        // Using a simple but effective mixing function
        hash ^= seed;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        
        return hash;
    }
}
