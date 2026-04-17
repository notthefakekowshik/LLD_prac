package com.lldprep.systems.ratelimiter;

/**
 * Enumeration of supported rate limiting algorithms.
 * 
 * Each algorithm has different characteristics and use cases.
 * See DESIGN.md for detailed comparison.
 */
public enum AlgorithmType {
    
    /**
     * Token Bucket Algorithm.
     * 
     * Characteristics:
     * - Allows bursts (can accumulate tokens)
     * - Smooth refilling
     * - O(1) time, O(1) space
     * 
     * Best for: API rate limiting, cloud service quotas
     */
    TOKEN_BUCKET,
    
    /**
     * Leaky Bucket Algorithm.
     * 
     * Characteristics:
     * - Smooths bursts (enforces constant rate)
     * - Queue-based
     * - O(1) time, O(n) space
     * 
     * Best for: Traffic shaping, network routers
     */
    LEAKY_BUCKET,
    
    /**
     * Fixed Window Counter Algorithm.
     * 
     * Characteristics:
     * - Simple implementation
     * - Boundary issue (2x rate at window edges)
     * - O(1) time, O(1) space
     * 
     * Best for: Simple scenarios, analytics, low-memory
     */
    FIXED_WINDOW,
    
    /**
     * Sliding Window Log Algorithm.
     * 
     * Characteristics:
     * - Perfect accuracy (no boundary issues)
     * - High memory usage
     * - O(log n) time, O(n) space
     * 
     * Best for: Security, critical systems requiring precision
     */
    SLIDING_WINDOW_LOG,
    
    /**
     * Sliding Window Counter Algorithm (Hybrid).
     * 
     * Characteristics:
     * - Good accuracy (approximation)
     * - Low memory usage
     * - O(1) time, O(1) space
     * 
     * Best for: Production APIs, high-traffic systems
     */
    SLIDING_WINDOW_COUNTER
}
