package com.kowshik.gc;

import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.NotificationEmitter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * GCMonitoringDemo — Programmatic GC Monitoring via JMX MXBeans
 *
 * INTERVIEW PREP:
 * ==============
 * Q: How do you monitor GC programmatically in Java?
 * A: Via ManagementFactory: GarbageCollectorMXBean (count + cumulative time),
 *    MemoryPoolMXBean (per-pool usage), MemoryMXBean (heap/non-heap totals).
 *    GC notifications via NotificationEmitter + GarbageCollectionNotificationInfo.
 *
 * Q: What information does GarbageCollectionNotificationInfo provide?
 * A: GC name (algorithm), GC cause (allocation failure, explicit, etc.),
 *    GC action (minor/major), duration, memory usage before and after.
 *
 * Q: What does -XX:+HeapDumpOnOutOfMemoryError do?
 * A: On OOM, automatically writes a heap dump (.hprof) to disk.
 *    Analyze with Eclipse MAT or VisualVM to find the leak.
 */
public class GCMonitoringDemo {

    // ─────────────────────────────────────────────────────────────────────────
    // Part 1: List all GC beans — see which collectors are active
    // ─────────────────────────────────────────────────────────────────────────
    static void listGCBeans() {
        System.out.println("--- Part 1: Active GC Collectors ---");
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        for (GarbageCollectorMXBean gc : gcBeans) {
            System.out.printf("  Collector: %-30s | Collections: %3d | Total time: %dms%n",
                    gc.getName(),
                    gc.getCollectionCount(),
                    gc.getCollectionTime());
            System.out.print("    Manages pools: ");
            for (String pool : gc.getMemoryPoolNames()) {
                System.out.print(pool + "  ");
            }
            System.out.println();
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 2: Per-memory-pool usage
    // Shows Eden, Survivor, Old Gen, Metaspace individually
    // ─────────────────────────────────────────────────────────────────────────
    static void memoryPoolUsage() {
        System.out.println("--- Part 2: Memory Pool Usage ---");
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();

        for (MemoryPoolMXBean pool : pools) {
            MemoryUsage usage = pool.getUsage();
            if (usage == null) continue;
            long max = usage.getMax();
            long used = usage.getUsed();
            long committed = usage.getCommitted();

            System.out.printf("  %-40s type=%-8s used=%5dMB committed=%5dMB max=%s%n",
                    pool.getName(),
                    pool.getType().toString().replace("NON_HEAP", "NON-HEAP"),
                    used / (1024 * 1024),
                    committed / (1024 * 1024),
                    max < 0 ? "unlimited" : (max / (1024 * 1024)) + "MB");
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 3: Heap vs Non-heap summary
    // ─────────────────────────────────────────────────────────────────────────
    static void heapSummary() {
        System.out.println("--- Part 3: Heap vs Non-Heap Summary ---");
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heap = memBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

        System.out.printf("  Heap:    used=%dMB  committed=%dMB  max=%dMB%n",
                heap.getUsed() / (1024 * 1024),
                heap.getCommitted() / (1024 * 1024),
                heap.getMax() / (1024 * 1024));
        System.out.printf("  Non-heap: used=%dMB  committed=%dMB%n",
                nonHeap.getUsed() / (1024 * 1024),
                nonHeap.getCommitted() / (1024 * 1024));
        System.out.printf("  Finalization pending: %d objects%n%n",
                memBean.getObjectPendingFinalizationCount());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 4: GC Notification listener — get notified on every GC event
    // Shows: GC name, cause, action, duration, before/after heap
    // ─────────────────────────────────────────────────────────────────────────
    static void installGCNotificationListener() {
        System.out.println("--- Part 4: GC Notification Listener ---");
        System.out.println("  Installing listener on all GC beans...\n");

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean instanceof NotificationEmitter emitter) {
                emitter.addNotificationListener((notification, handback) -> {
                    if (!notification.getType().equals(
                            GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                        return;
                    }
                    GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo
                            .from((javax.management.openmbean.CompositeData) notification.getUserData());

                    long durationMs = info.getGcInfo().getDuration();
                    long beforeUsed = 0, afterUsed = 0;
                    for (var entry : info.getGcInfo().getMemoryUsageBeforeGc().values()) {
                        beforeUsed += entry.getUsed();
                    }
                    for (var entry : info.getGcInfo().getMemoryUsageAfterGc().values()) {
                        afterUsed += entry.getUsed();
                    }
                    long freedMb = (beforeUsed - afterUsed) / (1024 * 1024);

                    System.out.printf("  [GC Event] name=%-25s cause=%-25s action=%-15s duration=%dms freed=%dMB%n",
                            info.getGcName(),
                            info.getGcCause(),
                            info.getGcAction(),
                            durationMs,
                            freedMb);
                }, null, null);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 5: Trigger GC events by allocating memory and observe notifications
    // ─────────────────────────────────────────────────────────────────────────
    static void triggerGCEvents() throws InterruptedException {
        System.out.println("--- Part 5: Triggering GC Events ---");
        System.out.println("  Allocating short-lived objects to trigger Minor GC...\n");

        // Allocate many short-lived objects to fill Eden and trigger Minor GC
        List<byte[]> sink = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            sink.add(new byte[512 * 1024]); // 512KB chunks
            if (i % 20 == 19) {
                sink.clear(); // Make most objects unreachable → Eden fills → Minor GC
                Thread.sleep(50);
            }
        }
        sink.clear();
        System.gc(); // Hint for a more thorough collection
        Thread.sleep(500); // Let GC notifications arrive
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Part 6: GC overhead measurement — how much time is spent in GC?
    // Rule of thumb: > 5% GC time = problematic
    // ─────────────────────────────────────────────────────────────────────────
    static void measureGCOverhead() throws InterruptedException {
        System.out.println("\n--- Part 6: GC Overhead Measurement ---");

        // Snapshot before
        long gcTimeBefore = ManagementFactory.getGarbageCollectorMXBeans()
                .stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        long wallTimeBefore = System.currentTimeMillis();

        // Do some work
        List<byte[]> temp = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            temp.add(new byte[1024 * 1024]);
        }
        temp.clear();
        System.gc();
        Thread.sleep(200);

        // Snapshot after
        long gcTimeAfter = ManagementFactory.getGarbageCollectorMXBeans()
                .stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        long wallTimeAfter = System.currentTimeMillis();

        long gcTime = gcTimeAfter - gcTimeBefore;
        long wallTime = wallTimeAfter - wallTimeBefore;
        double gcOverhead = wallTime > 0 ? (100.0 * gcTime / wallTime) : 0;

        System.out.printf("  Wall time: %dms | GC time: %dms | GC overhead: %.1f%%%n",
                wallTime, gcTime, gcOverhead);
        System.out.println("  Rule of thumb: > 5% GC overhead = investigate heap sizing or leaks");
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== GC Monitoring Demo ===\n");
        System.out.printf("  JVM: %s | GC: %s%n%n",
                System.getProperty("java.vm.name"),
                ManagementFactory.getGarbageCollectorMXBeans().stream()
                        .map(GarbageCollectorMXBean::getName)
                        .reduce((a, b) -> a + " + " + b).orElse("unknown"));

        listGCBeans();
        memoryPoolUsage();
        heapSummary();
        installGCNotificationListener(); // Install before triggering GC
        triggerGCEvents();               // Triggers GC → listener fires
        measureGCOverhead();

        System.out.println("\n=== Done ===");
        System.out.println("Run with: -Xlog:gc*  to see detailed GC logs");
        System.out.println("Run with: -Xmx64m   to see GC under memory pressure");
    }
}
