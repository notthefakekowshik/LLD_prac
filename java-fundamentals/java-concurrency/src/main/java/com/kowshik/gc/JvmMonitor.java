package com.kowshik.gc;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.OperatingSystemMXBean;

import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * JvmMonitor — Unified JVM health snapshot: heap, threads, GC, OS, heap dump trigger.
 *
 * Extends GCMonitoringDemo (which covers GC events/listeners) with:
 * - Thread dump programmatic capture
 * - Heap dump programmatic trigger (via HotSpotDiagnostic)
 * - OS resource metrics (CPU, physical memory, swap)
 * - Deadlock detection
 * - Structured health report
 *
 * Interview signal:
 * "I use ManagementFactory to get MXBeans for heap/GC/thread/OS metrics.
 *  For heap dumps, I use the HotSpotDiagnostic MXBean. For thread dumps,
 *  I thread-dump-all via ThreadMXBean. This gives me production observability
 *  without external agents."
 */
public class JvmMonitor {

    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final ThreadMXBean threadBean;
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;
    private final List<MemoryPoolMXBean> poolBeans;
    private final HotSpotDiagnosticMXBean diagnosticBean;

    public JvmMonitor() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.poolBeans = ManagementFactory.getMemoryPoolMXBeans();
        this.diagnosticBean = getDiagnosticBean();
    }

    // ────────────────────────────────────────────────────────────────
    // Data classes
    // ────────────────────────────────────────────────────────────────

    public record MemorySnapshot(
            long heapUsed, long heapCommitted, long heapMax,
            long nonHeapUsed, long nonHeapCommitted,
            int pendingFinalization,
            List<PoolInfo> pools
    ) {
        public double heapUsagePercent() {
            return heapMax > 0 ? (100.0 * heapUsed / heapMax) : 0;
        }
    }

    public record PoolInfo(String name, String type, long used, long committed, long max) {}

    public record GcInfo(String name, long collectionCount, long collectionTimeMs, String[] poolNames) {}

    public record DeadlockInfo(long[] deadlockedThreadIds) {}

    public record OsInfo(
            double systemCpuLoad, double processCpuLoad,
            long freePhysicalMem, long totalPhysicalMem,
            long freeSwap, long totalSwap
    ) {
        public double memoryUsedPercent() {
            return totalPhysicalMem > 0
                    ? 100.0 * (totalPhysicalMem - freePhysicalMem) / totalPhysicalMem
                    : 0;
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Snapshots
    // ────────────────────────────────────────────────────────────────

    public MemorySnapshot getMemorySnapshot() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        List<PoolInfo> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : poolBeans) {
            MemoryUsage usage = pool.getUsage();
            if (usage != null) {
                pools.add(new PoolInfo(
                        pool.getName(), pool.getType().name(),
                        usage.getUsed(), usage.getCommitted(), usage.getMax()));
            }
        }

        return new MemorySnapshot(
                heap.getUsed(), heap.getCommitted(), heap.getMax(),
                nonHeap.getUsed(), nonHeap.getCommitted(),
                memoryBean.getObjectPendingFinalizationCount(),
                pools);
    }

    public List<GcInfo> getGcInfoList() {
        List<GcInfo> result = new ArrayList<>();
        for (GarbageCollectorMXBean gc : gcBeans) {
            result.add(new GcInfo(
                    gc.getName(), gc.getCollectionCount(),
                    gc.getCollectionTime(), gc.getMemoryPoolNames()));
        }
        return result;
    }

    public double getGcOverheadPercent() {
        long totalGcMs = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        long uptime = runtimeBean.getUptime();
        return uptime > 0 ? (100.0 * totalGcMs / uptime) : 0;
    }

    // ────────────────────────────────────────────────────────────────
    // Thread dump (programmatic)
    // ────────────────────────────────────────────────────────────────

    public String getThreadDump() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Thread Dump ===\n");
        sb.append("Timestamp: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        long[] deadlocked = threadBean.findDeadlockedThreads();
        Set<Long> deadlockedSet = deadlocked != null
                ? new HashSet<>(Arrays.stream(deadlocked).boxed().toList())
                : Set.of();

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread t = entry.getKey();
            sb.append(String.format("\"%s\" #%d %s prio=%d",
                    t.getName(), t.getId(), t.getState(), t.getPriority()));
            if (t.isDaemon()) sb.append(" daemon");
            if (deadlockedSet.contains(t.getId())) sb.append(" ⛔ DEADLOCKED");
            sb.append("\n");

            for (StackTraceElement ste : entry.getValue()) {
                sb.append("    at ").append(ste).append("\n");
            }
            sb.append("\n");
        }

        if (deadlocked != null && deadlocked.length > 0) {
            sb.append("⛔ DEADLOCK DETECTED! Involved threads:\n");
            for (long id : deadlocked) {
                java.lang.management.ThreadInfo mgmtTi = threadBean.getThreadInfo(id);
                if (mgmtTi != null) {
                    sb.append(String.format("  Thread #%d: %s%n", id, mgmtTi.getThreadName()));
                }
            }
        }

        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────
    // Heap dump trigger (programmatic — no external tools needed)
    // ────────────────────────────────────────────────────────────────

    /**
     * Triggers a heap dump via HotSpotDiagnostic MXBean.
     * Requires the caller to have appropriate permissions.
     *
     * @param filePath Where to write the .hprof file (absolute or relative)
     * @param liveOnly If true, dump only live objects (forces Full GC first)
     */
    public void triggerHeapDump(String filePath, boolean liveOnly) {
        if (diagnosticBean == null) {
            throw new UnsupportedOperationException(
                    "HotSpotDiagnostic MXBean not available on this JVM");
        }
        try {
            diagnosticBean.dumpHeap(filePath, liveOnly);
            System.out.printf("  Heap dump written: %s (%s)%n",
                    new File(filePath).getAbsolutePath(),
                    liveOnly ? "live objects only" : "full dump");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create heap dump: " + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // OS metrics
    // ────────────────────────────────────────────────────────────────

    public OsInfo getOsInfo() {
        return new OsInfo(
                osBean.getCpuLoad(),
                osBean.getProcessCpuLoad(),
                osBean.getFreeMemorySize(),
                osBean.getTotalMemorySize(),
                osBean.getFreeSwapSpaceSize(),
                osBean.getTotalSwapSpaceSize());
    }

    // ────────────────────────────────────────────────────────────────
    // Deadlock check
    // ────────────────────────────────────────────────────────────────

    public DeadlockInfo getDeadlockInfo() {
        return new DeadlockInfo(threadBean.findDeadlockedThreads());
    }

    // ────────────────────────────────────────────────────────────────
    // Full health report
    // ────────────────────────────────────────────────────────────────

    public String generateHealthReport() {
        MemorySnapshot mem = getMemorySnapshot();
        List<GcInfo> gcInfos = getGcInfoList();
        OsInfo os = getOsInfo();
        DeadlockInfo di = getDeadlockInfo();
        long uptimeMin = runtimeBean.getUptime() / 60000;

        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════════════════════\n");
        sb.append("  JVM HEALTH REPORT\n");
        sb.append("══════════════════════════════════════════════════════\n");
        sb.append(String.format("  Uptime: %d min | JVM: %s %s%n",
                uptimeMin,
                System.getProperty("java.vm.name"),
                System.getProperty("java.version")));
        sb.append(String.format("  Available processors: %d%n%n",
                Runtime.getRuntime().availableProcessors()));

        // Heap
        sb.append("── Heap ───────────────────────────────────────────\n");
        sb.append(String.format("  Heap used:       %d MB / %d MB (%.1f%%)\n",
                mem.heapUsed() / (1024 * 1024),
                mem.heapMax() / (1024 * 1024),
                mem.heapUsagePercent()));
        sb.append(String.format("  Non-heap:        %d MB\n", mem.nonHeapUsed() / (1024 * 1024)));
        sb.append(String.format("  Pending finalize: %d\n\n", mem.pendingFinalization()));

        // Pools
        sb.append("  Memory Pools:\n");
        for (PoolInfo p : mem.pools()) {
            String maxStr = p.max() > 0
                    ? String.format("%d MB", p.max() / (1024 * 1024))
                    : "unlimited";
            sb.append(String.format("    %-35s type=%-10s used=%4d MB max=%s\n",
                    p.name(), p.type(), p.used() / (1024 * 1024), maxStr));
        }
        sb.append("\n");

        // GC
        sb.append("── GC ─────────────────────────────────────────────\n");
        sb.append(String.format("  GC overhead: %.2f%% (%s)\n",
                getGcOverheadPercent(),
                getGcOverheadPercent() < 5 ? "healthy" : "⛔ HIGH"));
        for (GcInfo gc : gcInfos) {
            sb.append(String.format("  %-30s count=%4d time=%6d ms\n",
                    gc.name(), gc.collectionCount(), gc.collectionTimeMs()));
        }
        sb.append("\n");

        // Threads
        sb.append("── Threads ────────────────────────────────────────\n");
        sb.append(String.format("  Live: %d | Daemon: %d | Peak: %d | Started: %d\n",
                threadBean.getThreadCount(),
                threadBean.getDaemonThreadCount(),
                threadBean.getPeakThreadCount(),
                threadBean.getTotalStartedThreadCount()));
        if (di.deadlockedThreadIds() != null && di.deadlockedThreadIds().length > 0) {
            sb.append("  ⛔ DEADLOCK DETECTED!\n");
        } else {
            sb.append("  Deadlock: none\n");
        }
        sb.append("\n");

        // OS
        sb.append("── OS ─────────────────────────────────────────────\n");
        sb.append(String.format("  System CPU:   %.1f%%\n", os.systemCpuLoad() * 100));
        sb.append(String.format("  Process CPU:  %.1f%%\n", os.processCpuLoad() * 100));
        sb.append(String.format("  Physical mem: %d MB / %d MB (%.1f%% used)\n",
                (os.totalPhysicalMem() - os.freePhysicalMem()) / (1024 * 1024),
                os.totalPhysicalMem() / (1024 * 1024),
                os.memoryUsedPercent()));
        sb.append(String.format("  Swap:         %d MB / %d MB\n",
                (os.totalSwap() - os.freeSwap()) / (1024 * 1024),
                os.totalSwap() / (1024 * 1024)));
        sb.append("══════════════════════════════════════════════════════\n");

        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────
    // Demo
    // ────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        System.out.println("=== JVM Monitor Demo ===\n");
        JvmMonitor monitor = new JvmMonitor();

        System.out.println(monitor.generateHealthReport());

        // Demonstrate thread dump
        System.out.println("── Thread Dump (first 20 lines) ───────────────────");
        String threadDump = monitor.getThreadDump();
        threadDump.lines().limit(20).forEach(System.out::println);
        System.out.println("  ... (truncated)\n");

        // Demonstrate heap dump trigger (only if explicitly enabled)
        boolean enableHeapDump = Boolean.parseBoolean(System.getProperty("jvmmonitor.heapdump", "false"));
        if (enableHeapDump) {
            System.out.println("── Heap Dump ────────────────────────────────────");
            String dumpPath = System.getProperty("jvmmonitor.dumpPath", "jvm_monitor_heap.hprof");
            monitor.triggerHeapDump(dumpPath, false);
        } else {
            System.out.println("── Heap Dump ────────────────────────────────────");
            System.out.println("  Skipped. Run with -Djvmmonitor.heapdump=true to trigger.");
            System.out.println("  Set -Djvmmonitor.dumpPath=/path/to/file.hprof for custom path.\n");
        }

        System.out.println("=== Done ===");
    }

    // ────────────────────────────────────────────────────────────────
    // Internal
    // ────────────────────────────────────────────────────────────────

    private static HotSpotDiagnosticMXBean getDiagnosticBean() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            return ManagementFactory.newPlatformMXBeanProxy(
                    server,
                    "com.sun.management:type=HotSpotDiagnostic",
                    HotSpotDiagnosticMXBean.class);
        } catch (Exception e) {
            System.err.println("  Warning: HotSpotDiagnostic MXBean unavailable — heap dump trigger disabled");
            return null;
        }
    }
}
