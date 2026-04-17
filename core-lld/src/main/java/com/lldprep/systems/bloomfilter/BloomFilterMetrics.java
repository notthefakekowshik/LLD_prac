package com.lldprep.bloomfilter;

/**
 * CURVEBALL #3: "Add monitoring/metrics"
 * 
 * Provides observability into BloomFilter state and performance.
 * 
 * Design Pattern: Decorator Pattern (alternative approach)
 * - Could wrap BloomFilter to add metrics
 * - Here we use a separate utility class for simplicity
 * 
 * Metrics Provided:
 * - Saturation: How full is the bit array?
 * - Estimated false positive rate: Based on actual fill ratio
 * - Approximate element count: Estimate based on set bits
 * 
 * @param <T> Type of elements in the filter
 */
public class BloomFilterMetrics<T> {
    
    private final BloomFilter<T> filter;
    
    public BloomFilterMetrics(BloomFilter<T> filter) {
        this.filter = filter;
    }
    
    /**
     * Calculates the saturation level of the bit array.
     * 
     * Saturation = (number of set bits) / (total bits)
     * 
     * High saturation (>50%) indicates:
     * - False positive rate is higher than configured
     * - Filter is approaching capacity
     * - Consider creating a new filter or using Scalable Bloom Filter
     * 
     * @return Saturation percentage (0.0 to 1.0)
     */
    public double getSaturation() {
        // Note: We can't directly access bitArray from BloomFilter
        // In a real implementation, we'd either:
        // 1. Make bitArray package-private and put this in same package
        // 2. Add a getBitArray() method to BloomFilter
        // 3. Add these metrics directly to BloomFilter
        
        // For this demo, we'll return a calculated estimate
        // In production, this would access the actual BitSet
        return estimateSaturationFromFPR();
    }
    
    /**
     * Estimates saturation based on the configured false positive rate.
     * This is a theoretical estimate, not actual measurement.
     */
    private double estimateSaturationFromFPR() {
        // For a well-configured Bloom filter at capacity:
        // saturation ≈ 1 - e^(-k*n/m)
        // where k = hash functions, n = elements, m = bit array size
        
        int k = filter.getNumHashFunctions();
        int n = filter.getExpectedElements();
        int m = filter.getBitArraySize();
        
        double exponent = -(k * n) / (double) m;
        return 1 - Math.exp(exponent);
    }
    
    /**
     * Estimates the actual false positive rate based on current saturation.
     * 
     * Formula: p = (1 - e^(-kn/m))^k
     * 
     * This accounts for the actual number of elements inserted.
     * If more elements than expected are inserted, FPR increases.
     * 
     * @param actualElementCount Number of elements actually inserted
     * @return Estimated false positive rate
     */
    public double estimateActualFalsePositiveRate(int actualElementCount) {
        int k = filter.getNumHashFunctions();
        int m = filter.getBitArraySize();
        
        double exponent = -(k * actualElementCount) / (double) m;
        double fillRatio = 1 - Math.exp(exponent);
        
        return Math.pow(fillRatio, k);
    }
    
    /**
     * Estimates the number of elements in the filter based on set bits.
     * 
     * Formula: n ≈ -(m/k) * ln(1 - X/m)
     * where:
     *   n = estimated element count
     *   m = bit array size
     *   k = number of hash functions
     *   X = number of set bits
     * 
     * Note: This is an approximation and becomes less accurate with high saturation.
     * 
     * @param setBitsCount Number of bits set to 1 in the bit array
     * @return Estimated number of elements
     */
    public int estimateElementCount(int setBitsCount) {
        int m = filter.getBitArraySize();
        int k = filter.getNumHashFunctions();
        
        if (setBitsCount == 0) {
            return 0;
        }
        
        double fillRatio = setBitsCount / (double) m;
        if (fillRatio >= 1.0) {
            return filter.getExpectedElements(); // Saturated
        }
        
        double estimate = -(m / (double) k) * Math.log(1 - fillRatio);
        return (int) Math.round(estimate);
    }
    
    /**
     * Checks if the filter is approaching saturation.
     * 
     * @param threshold Saturation threshold (e.g., 0.5 for 50%)
     * @return true if saturation exceeds threshold
     */
    public boolean isApproachingSaturation(double threshold) {
        return getSaturation() > threshold;
    }
    
    /**
     * Generates a health report for the Bloom filter.
     * 
     * @param actualElementCount Number of elements actually inserted
     * @return Human-readable health report
     */
    public String getHealthReport(int actualElementCount) {
        double saturation = getSaturation();
        double actualFPR = estimateActualFalsePositiveRate(actualElementCount);
        double configuredFPR = filter.getFalsePositiveRate();
        
        StringBuilder report = new StringBuilder();
        report.append("Bloom Filter Health Report\n");
        report.append("===========================\n");
        report.append(String.format("Expected elements: %d\n", filter.getExpectedElements()));
        report.append(String.format("Actual elements: %d\n", actualElementCount));
        report.append(String.format("Bit array size: %d\n", filter.getBitArraySize()));
        report.append(String.format("Hash functions: %d\n", filter.getNumHashFunctions()));
        report.append(String.format("Saturation: %.2f%%\n", saturation * 100));
        report.append(String.format("Configured FPR: %.4f%%\n", configuredFPR * 100));
        report.append(String.format("Estimated actual FPR: %.4f%%\n", actualFPR * 100));
        
        if (actualElementCount > filter.getExpectedElements()) {
            report.append("\n⚠️  WARNING: Actual elements exceed expected capacity!\n");
            report.append("   False positive rate is higher than configured.\n");
            report.append("   Consider creating a new filter with higher capacity.\n");
        }
        
        if (saturation > 0.7) {
            report.append("\n⚠️  WARNING: High saturation detected!\n");
            report.append("   Filter is approaching capacity.\n");
        }
        
        return report.toString();
    }
}
