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
    
    @Override
    public int hash(T element, int seed) {
        if (element == null) {
            return 0;
        }
        
        byte[] data = element.toString().getBytes();
        return murmur3_32(data, seed ^ MURMUR_SEED);
    }
    
    /**
     * MurmurHash3 32-bit variant implementation.
     * 
     * Based on Austin Appleby's MurmurHash3 algorithm.
     * Public domain implementation.
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
            case 2:
                k ^= (data[i + 1] & 0xff) << 8;
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
