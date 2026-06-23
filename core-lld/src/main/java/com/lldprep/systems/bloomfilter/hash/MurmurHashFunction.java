package com.lldprep.systems.bloomfilter.hash;

/**
 * MurmurHash implementation - high quality, non-cryptographic hash function.
 * 
 * Characteristics:
 * - Excellent distribution properties
 * - Fast performance
 * - Low collision rate
 * - Industry standard for Bloom filters (used by Google Guava, Redis)
 * 
 * @param <T> Type of elements to be hashed
 */
public class MurmurHashFunction<T> implements HashFunction<T> {
    
    private static final int MURMUR_SEED = 0x9747b28c;
    
    /**
     * Converts element to stable UTF-8 bytes and applies MurmurHash3.
     *
     * Why seed is XORed with MURMUR_SEED: caller seed gives BloomFilter k
     * different hash streams, while fixed seed avoids starting from small values
     * like 0, 1, 2 that may have weaker early mixing for tiny inputs.
     */
    @Override
    public int hash(T element, int seed) {
        if (element == null) {
            return 0;
        }
        
        byte[] data = element.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return murmur3_32(data, seed ^ MURMUR_SEED);
    }
    
    /**
     * MurmurHash3 32-bit variant implementation.
     * 
     * Based on Austin Appleby's MurmurHash3 algorithm.
     * Public domain implementation.
     *
     * How it works:
     * 1. Consume input in 4-byte little-endian blocks because 32-bit mixing is
     *    strongest when each round works on one full int.
     * 2. Mix each block using multiply-rotate-multiply. Constants are chosen by
     *    MurmurHash3 to create avalanche: small input changes affect many bits.
     * 3. Fold block hash into running hash h, then rotate/multiply/add so block
     *    order matters and repeated chunks do not cancel out.
     * 4. Mix remaining 1-3 tail bytes with same block constants.
     * 5. Finalize length and upper/lower bits so similar prefixes, suffixes, and
     *    short inputs still spread across output range.
     *
     * Integer overflow is intentional in all multiplications; Java int overflow
     * gives required modulo-2^32 behavior.
     */
    private int murmur3_32(byte[] data, int seed) {
        int h = seed;
        int len = data.length;
        int i = 0;
        
        // Process 4-byte chunks
        while (len >= 4) {
            int k = (data[i] & 0xff)
                  | ((data[i + 1] & 0xff) << 8)
                  | ((data[i + 2] & 0xff) << 16)
                  | ((data[i + 3] & 0xff) << 24);
            
            k *= 0xcc9e2d51;
            k = Integer.rotateLeft(k, 15);
            k *= 0x1b873593;
            
            h ^= k;
            h = Integer.rotateLeft(h, 13);
            h = h * 5 + 0xe6546b64;
            
            i += 4;
            len -= 4;
        }
        
        // Process remaining bytes
        int k = 0;
        switch (len) {
            case 3:
                k ^= (data[i + 2] & 0xff) << 16;
                // Fall through: Murmur tail intentionally packs all leftover bytes.
            case 2:
                k ^= (data[i + 1] & 0xff) << 8;
                // Fall through: byte 1 and byte 0 must share same final mix.
            case 1:
                k ^= (data[i] & 0xff);
                k *= 0xcc9e2d51;
                k = Integer.rotateLeft(k, 15);
                k *= 0x1b873593;
                h ^= k;
        }
        
        // Finalization
        h ^= data.length;
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        
        return h;
    }
}
