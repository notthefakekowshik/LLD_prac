package com.lldprep.systems.bloomfilter.hash;

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
    
    /**
     * Converts element to stable UTF-8 bytes and applies FNV-1a with caller seed.
     *
     * Why seed is mixed in: BloomFilter calls same HashFunction multiple times
     * for same element. Changing seed simulates multiple independent hash
     * functions without creating separate implementations.
     */
    @Override
    public int hash(T element, int seed) {
        if (element == null) {
            return 0;
        }
        
        byte[] data = element.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return fnv1a_32(data, seed);
    }
    
    /**
     * FNV-1a 32-bit hash implementation.
     * 
     * How it works:
     * 1. Start with fixed offset basis, then XOR seed to get a different
     *    starting state per Bloom-filter hash index.
     * 2. XOR next input byte into current hash so byte value affects low bits.
     * 3. Multiply by FNV prime. Integer overflow is intentional here; wrapping
     *    spreads changed bits across the 32-bit hash state.
     *
     * Why XOR before multiply: FNV-1a generally gives better avalanche behavior
     * for short strings than original FNV-1, which multiplies before XOR.
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
