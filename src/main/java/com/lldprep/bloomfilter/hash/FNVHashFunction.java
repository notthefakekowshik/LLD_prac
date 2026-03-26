package com.lldprep.bloomfilter.hash;

/**
 * FNV-1a (Fowler-Noll-Vo) hash function implementation.
 * 
 * Characteristics:
 * - Simple and fast
 * - Good distribution for short strings
 * - Lower quality than MurmurHash but faster
 * - Suitable for performance-critical scenarios with acceptable collision rates
 * 
 * @param <T> Type of elements to be hashed
 */
public class FNVHashFunction<T> implements HashFunction<T> {
    
    private static final int FNV_32_INIT = 0x811c9dc5;
    private static final int FNV_32_PRIME = 0x01000193;
    
    @Override
    public int hash(T element, int seed) {
        if (element == null) {
            return 0;
        }
        
        byte[] data = element.toString().getBytes();
        return fnv1a_32(data, seed);
    }
    
    /**
     * FNV-1a 32-bit hash implementation.
     * 
     * Algorithm:
     * 1. Start with FNV offset basis XOR seed
     * 2. For each byte: XOR with hash, then multiply by FNV prime
     */
    private int fnv1a_32(byte[] data, int seed) {
        int hash = FNV_32_INIT ^ seed;
        
        for (byte b : data) {
            hash ^= (b & 0xff);
            hash *= FNV_32_PRIME;
        }
        
        return hash;
    }
}
