package com.lldprep.bloomfilter;

/**
 * Demonstrates curveball scenarios - extensibility without modifying existing code.
 * 
 * This showcases the Open/Closed Principle (OCP):
 * - New functionality added through NEW classes
 * - Existing BloomFilter code remains UNTOUCHED
 * 
 * Curveballs Handled:
 * 1. Deletion support → CountingBloomFilter
 * 2. Reset functionality → CountingBloomFilter.clear()
 * 3. Monitoring/metrics → BloomFilterMetrics
 */
public class CurveballDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Bloom Filter Curveball Scenarios ===\n");
        
        demonstrateDeletionSupport();
        demonstrateMetrics();
    }
    
    /**
     * CURVEBALL #1: "Add support for deletion"
     * 
     * Solution: CountingBloomFilter - NEW class, not modification of existing code.
     * Demonstrates OCP - extended functionality without modifying BloomFilter.
     */
    private static void demonstrateDeletionSupport() {
        System.out.println("Curveball #1: Deletion Support");
        System.out.println("=".repeat(50));
        
        CountingBloomFilter<String> filter = new CountingBloomFilter.Builder<String>()
                .expectedElements(100)
                .falsePositiveRate(0.01)
                .build();
        
        System.out.println("Creating Counting Bloom Filter (supports deletion)");
        System.out.println("Memory trade-off: ~4x more space than standard Bloom filter");
        System.out.println();
        
        String[] sessions = {"session-123", "session-456", "session-789"};
        
        System.out.println("Adding active sessions:");
        for (String session : sessions) {
            filter.add(session);
            System.out.println("  Added: " + session);
        }
        System.out.println("  Element count: " + filter.getElementCount());
        System.out.println();
        
        System.out.println("Checking membership:");
        for (String session : sessions) {
            System.out.println("  " + session + " -> " + filter.mightContain(session));
        }
        System.out.println();
        
        System.out.println("Removing session-456 (user logged out):");
        filter.remove("session-456");
        System.out.println("  Element count: " + filter.getElementCount());
        System.out.println();
        
        System.out.println("Checking membership after removal:");
        for (String session : sessions) {
            boolean exists = filter.mightContain(session);
            String expected = session.equals("session-456") ? "false" : "true";
            System.out.println("  " + session + " -> " + exists + " (expected: " + expected + ")");
        }
        System.out.println();
        
        System.out.println("CURVEBALL #2: Reset functionality");
        filter.clear();
        System.out.println("  Called clear() - all counters reset to 0");
        System.out.println("  Element count: " + filter.getElementCount());
        System.out.println("  session-123 exists? " + filter.mightContain("session-123") + " (expected: false)");
        System.out.println();
        
        System.out.println("Design Pattern: Open/Closed Principle");
        System.out.println("  ✓ Created NEW class (CountingBloomFilter)");
        System.out.println("  ✓ Did NOT modify existing BloomFilter");
        System.out.println("  ✓ Both classes share similar interface");
        System.out.println("  ✓ Could extract common interface if needed");
        
        System.out.println("\n");
    }
    
    /**
     * CURVEBALL #3: "Add monitoring/metrics"
     * 
     * Solution: BloomFilterMetrics - separate utility class for observability.
     * Alternative: Could use Decorator pattern to wrap BloomFilter.
     */
    private static void demonstrateMetrics() {
        System.out.println("Curveball #3: Monitoring & Metrics");
        System.out.println("=".repeat(50));
        
        BloomFilter<String> filter = new BloomFilter.Builder<String>()
                .expectedElements(1000)
                .falsePositiveRate(0.01)
                .build();
        
        BloomFilterMetrics<String> metrics = new BloomFilterMetrics<>(filter);
        
        System.out.println("Adding 800 elements (80% of capacity):");
        for (int i = 0; i < 800; i++) {
            filter.add("user-" + i + "@example.com");
        }
        System.out.println("  Added 800 elements");
        System.out.println();
        
        System.out.println("Metrics:");
        System.out.printf("  Saturation: %.2f%%\n", metrics.getSaturation() * 100);
        System.out.printf("  Configured FPR: %.4f%%\n", filter.getFalsePositiveRate() * 100);
        System.out.printf("  Estimated actual FPR: %.4f%%\n", 
                         metrics.estimateActualFalsePositiveRate(800) * 100);
        System.out.println();
        
        System.out.println("Testing over-capacity scenario (adding 500 more elements):");
        for (int i = 800; i < 1300; i++) {
            filter.add("user-" + i + "@example.com");
        }
        System.out.println("  Total elements: 1300 (130% of expected capacity)");
        System.out.println();
        
        System.out.println("Updated metrics:");
        System.out.printf("  Saturation: %.2f%%\n", metrics.getSaturation() * 100);
        System.out.printf("  Estimated actual FPR: %.4f%%\n", 
                         metrics.estimateActualFalsePositiveRate(1300) * 100);
        System.out.println();
        
        if (metrics.isApproachingSaturation(0.7)) {
            System.out.println("⚠️  WARNING: Filter saturation exceeds 70%!");
            System.out.println("   False positive rate is higher than configured.");
            System.out.println("   Recommendation: Create a new filter with higher capacity.");
        }
        System.out.println();
        
        System.out.println("Health Report:");
        System.out.println(metrics.getHealthReport(1300));
        
        System.out.println("Design Pattern: Separation of Concerns");
        System.out.println("  ✓ Metrics logic separated from core BloomFilter");
        System.out.println("  ✓ BloomFilter remains focused on membership testing");
        System.out.println("  ✓ Metrics can be added/removed without touching BloomFilter");
        System.out.println("  ✓ Alternative: Could use Decorator pattern for transparent wrapping");
        
        System.out.println("\n");
    }
}
