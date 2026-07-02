# JVM Tuning — Complete Guide

---

## Table of Contents

1. [JVM Architecture](#1-jvm-architecture)
2. [Memory Areas](#2-memory-areas)
3. [Garbage Collection — Concepts](#3-garbage-collection--concepts)
4. [GC Algorithms](#4-gc-algorithms)
5. [Essential JVM Flags](#5-essential-jvm-flags)
6. [GC Logging & Reading GC Logs](#6-gc-logging--reading-gc-logs)
7. [JIT Compiler Tuning](#7-jit-compiler-tuning)
8. [Monitoring & Profiling Tools](#8-monitoring--profiling-tools)
9. [Diagnosing Common Problems](#9-diagnosing-common-problems)
10. [Tuning Recipes by Use Case](#10-tuning-recipes-by-use-case)

---

## 1. JVM Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Java Source (.java)                │
└─────────────────────┬───────────────────────────────┘
                      │  javac
┌─────────────────────▼───────────────────────────────┐
│                  Bytecode (.class)                   │
└─────────────────────┬───────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────┐
│                       JVM                           │
│  ┌──────────────────────────────────────────────┐   │
│  │           ClassLoader Subsystem              │   │
│  │  Bootstrap → Extension → Application         │   │
│  └──────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────┐   │
│  │           Runtime Data Areas                 │   │
│  │  Heap │ Metaspace │ Stacks │ PC Regs          │   │
│  └──────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────┐   │
│  │           Execution Engine                   │   │
│  │  Interpreter → JIT (C1/C2) → GC              │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

**ClassLoader chain:**
| Loader | Loads |
|---|---|
| Bootstrap | `rt.jar`, core Java classes (`java.lang.*`) |
| Extension / Platform | `jre/lib/ext`, JDK modules |
| Application | Your classpath |

**Class loading phases:** Loading → Linking (Verify → Prepare → Resolve) → Initialization

---

## 2. Memory Areas

### 2.1 Heap (GC-managed)

```
┌──────────────────────────────────────────────────────┐
│                        HEAP                          │
│  ┌─────────────────────────┐  ┌────────────────────┐ │
│  │       Young Generation  │  │   Old Generation   │ │
│  │  ┌───────┬────┬────┐    │  │  (Tenured)         │ │
│  │  │ Eden  │ S0 │ S1 │    │  │  Long-lived objs   │ │
│  │  └───────┴────┴────┘    │  │                    │ │
│  └─────────────────────────┘  └────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

| Region | Purpose | GC type |
|---|---|---|
| Eden | New object allocation | Minor GC |
| Survivor 0/1 (S0, S1) | Objects that survived one Minor GC | Minor GC |
| Old Gen (Tenured) | Objects that survived N Minor GCs (default: age 15) | Major/Full GC |

**Object lifecycle:**
1. Allocated in Eden
2. Survives Minor GC → copied to Survivor space, age incremented
3. Age reaches tenuring threshold → promoted to Old Gen
4. Old Gen fills up → Major/Full GC

### 2.2 Non-Heap Areas

| Area | Description | Key flags |
|---|---|---|
| **Metaspace** | Class metadata (replaced PermGen in Java 8+). Backed by native memory, grows dynamically | `-XX:MetaspaceSize`, `-XX:MaxMetaspaceSize` |
| **Code Cache** | JIT-compiled native code | `-XX:ReservedCodeCacheSize` |
| **Thread Stack** | Each thread gets its own stack | `-Xss` |
| **Direct / Off-heap** | `ByteBuffer.allocateDirect()`, not GC managed | `-XX:MaxDirectMemorySize` |
| **PC Register** | Each thread's current instruction pointer | — |

### 2.3 Why Metaspace replaced PermGen
- PermGen had a fixed max → frequent `OutOfMemoryError: PermGen space`
- Metaspace uses native memory → can grow dynamically (bounded by OS)
- Still needs `MaxMetaspaceSize` in production to prevent runaway class-loading leaks

---

## 3. Garbage Collection — Concepts

### 3.1 Core Phases

| Phase | What happens |
|---|---|
| **Mark** | Traverse object graph from GC roots; mark reachable objects |
| **Sweep** | Reclaim memory of unmarked (unreachable) objects |
| **Compact** | Defragment heap — move live objects together |
| **Copy** | (Young Gen) Copy live objects to empty Survivor; free entire Eden |

**GC Roots:** Thread stacks, static fields, JNI references, system classes.

### 3.2 Minor GC vs Major GC vs Full GC

| Type | Scope | Pause |
|---|---|---|
| **Minor GC** | Young Gen only | Short (ms range) |
| **Major GC** | Old Gen | Long (100ms–seconds) |
| **Full GC** | Entire heap + Metaspace | Longest (seconds+), always STW |

**Stop-The-World (STW):** All application threads paused during GC. Main source of latency spikes.

### 3.3 Object Promotion

```
Young GC cycles:  1   2   3   4  ...  15  (TenuringThreshold)
                  │   │   │   │        │
                  S0→S1→S0→S1→ ... →OldGen
```

**Premature promotion** (filling Old Gen too fast): caused by:
- Survivor spaces too small → objects overflow to Old Gen early
- Short-lived large objects (bypass Young Gen entirely if > `PretenureSizeThreshold`)

---

## 4. GC Algorithms

### 4.1 Serial GC
- **Flag:** `-XX:+UseSerialGC`
- Single-threaded, always STW
- Use case: small heaps (<100 MB), single-CPU, embedded

### 4.2 Parallel GC (Throughput Collector)
- **Flag:** `-XX:+UseParallelGC` (default before Java 9)
- Multi-threaded Minor and Major GC
- Goal: maximize throughput, pauses not minimized
- Use case: batch jobs, analytics, where latency doesn't matter

```
-XX:+UseParallelGC
-XX:ParallelGCThreads=<N>        # default: CPU cores
-XX:GCTimeRatio=<N>              # target: 1/(1+N) of time in GC (default 99 → 1% GC)
-XX:MaxGCPauseMillis=<ms>        # soft goal (may sacrifice throughput)
```

### 4.3 G1GC (Garbage First) — Default since Java 9
- **Flag:** `-XX:+UseG1GC`
- Divides heap into equal-sized **regions** (~2048 regions, 1–32 MB each)
- Regions are dynamically assigned as Eden/Survivor/Old/Humongous
- **Concurrent marking** runs alongside app threads; STW only for evacuation
- Targets a pause goal (`-XX:MaxGCPauseMillis`, default 200ms)

```
G1 GC Cycle:
  Young GC (STW)         → evacuates Eden + Survivor regions
  Concurrent Mark        → marks Old Gen objects (concurrent)
  Remark (STW, short)    → finalizes marking
  Cleanup (STW, short)   → identifies regions for reclaim
  Mixed GC (STW)         → evacuates Young + selected Old regions
  Full GC (STW)          → last resort, single-threaded compaction
```

**Key G1 flags:**
```
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200          # pause target (default 200ms)
-XX:G1HeapRegionSize=<N>m         # 1,2,4,8,16,32 MB; auto-calculated if unset
-XX:G1NewSizePercent=5            # min % of heap for Young Gen
-XX:G1MaxNewSizePercent=60        # max % of heap for Young Gen
-XX:G1MixedGCLiveThresholdPercent=85  # regions above this % live are skipped
-XX:InitiatingHeapOccupancyPercent=45 # % heap used to trigger concurrent marking
-XX:G1ReservePercent=10           # headroom to prevent evacuation failure
-XX:ConcGCThreads=<N>             # concurrent marking threads (default: ParallelGCThreads/4)
```

**Humongous objects:** objects > 50% of `G1HeapRegionSize` → allocated directly in Old Gen, can trigger Full GC. Tune `G1HeapRegionSize` to avoid them.

### 4.4 ZGC — Ultra-low latency (Java 15+ production)
- **Flag:** `-XX:+UseZGC`
- Almost entirely concurrent — pauses < 1ms regardless of heap size
- Heap can be TBs in size
- Uses colored pointers + load barriers (not card tables)
- Trade-off: slightly lower throughput than G1 (~10–15%)
- Use case: real-time systems, financial trading, gaming

```
-XX:+UseZGC
-XX:ZCollectionInterval=<seconds>  # force GC every N seconds (0 = disabled)
-XX:ZAllocationSpikeTolerance=2.0  # multiplier for allocation spike headroom
-XX:SoftMaxHeapSize=<bytes>        # soft limit; ZGC will try to stay below this
```

### 4.5 Shenandoah (Red Hat / OpenJDK)
- **Flag:** `-XX:+UseShenandoahGC`
- Also sub-millisecond pauses; concurrent compaction (unlike ZGC which doesn't compact)
- Available in OpenJDK; not in Oracle JDK by default

### 4.6 Choosing a GC

```
Heap size < 100 MB?          → Serial
Batch / high throughput?     → Parallel GC
General purpose?             → G1GC  ← default, start here
Latency < 10ms required?     → ZGC or Shenandoah
Java 8, latency-sensitive?   → G1GC + tuned pause millis
```

---

## 5. Essential JVM Flags

### 5.1 Heap Sizing

```bash
-Xms<size>              # Initial heap size  (e.g., -Xms512m)
-Xmx<size>              # Max heap size      (e.g., -Xmx4g)
-Xss<size>              # Thread stack size  (default ~512k-1m; reduce for many threads)
```

**Rule of thumb:** Set `-Xms == -Xmx` in production to avoid heap resizing pauses.

### 5.2 Metaspace

```bash
-XX:MetaspaceSize=<size>        # Initial commit size (not a limit); triggers first GC
-XX:MaxMetaspaceSize=<size>     # Hard cap (e.g., 256m); prevent OOM from class leaks
```

### 5.3 GC Selection

```bash
-XX:+UseSerialGC
-XX:+UseParallelGC
-XX:+UseG1GC
-XX:+UseZGC
-XX:+UseShenandoahGC
```

### 5.4 GC Tuning (G1)

```bash
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=45
-XX:G1HeapRegionSize=16m
-XX:G1ReservePercent=10
-XX:ParallelGCThreads=<N>
-XX:ConcGCThreads=<N>
```

### 5.5 Object Tenuring

```bash
-XX:MaxTenuringThreshold=15       # Promote after 15 Minor GCs (default)
-XX:+PrintTenuringDistribution    # Diagnose premature promotion
-XX:PretenureSizeThreshold=<bytes> # Objects larger than this go directly to Old Gen
```

### 5.6 Explicit GC

```bash
-XX:+DisableExplicitGC            # Ignore System.gc() calls (recommended in prod)
-XX:+ExplicitGCInvokesConcurrent  # Make System.gc() use concurrent GC instead of Full GC
```

### 5.7 Diagnostic & Safety

```bash
-XX:+HeapDumpOnOutOfMemoryError           # Dump heap on OOM
-XX:HeapDumpPath=/tmp/dump.hprof          # Dump location
-XX:+ExitOnOutOfMemoryError               # Kill JVM on OOM (let k8s restart it)
-XX:OnOutOfMemoryError="kill -9 %p"       # Shell command on OOM
-XX:ErrorFile=/tmp/hs_err_%p.log          # HotSpot crash log location
```

### 5.8 Code Cache (JIT)

```bash
-XX:ReservedCodeCacheSize=256m    # Max code cache (default 240m in Java 11+)
-XX:+UseCodeCacheFlushing         # Allow flushing old compiled code if cache fills
```

### 5.9 Container / Docker Awareness (Java 10+)

```bash
-XX:+UseContainerSupport          # Auto-detect cgroup memory/cpu limits (default on)
-XX:MaxRAMPercentage=75.0         # Set -Xmx as % of container memory
-XX:InitialRAMPercentage=50.0
```

**Pitfall:** Before Java 10, JVM would see the host machine's RAM, not the container limit → OOM kill by OS. Always use `-XX:MaxRAMPercentage` in containers instead of hardcoded `-Xmx`.

---

## 6. GC Logging & Reading GC Logs

### 6.1 Enable GC Logging (Java 9+)

```bash
-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=10,filesize=20m
```

**Breakdown:**
- `gc*` — all GC-related log tags
- `file=...` — log to file (omit for stdout)
- `time,uptime,level,tags` — decorators
- `filecount=10,filesize=20m` — rotate: 10 files × 20 MB

**Java 8 (legacy):**
```bash
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:/var/log/gc.log
-XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=20m
```

### 6.2 Reading a G1 GC Log

```
[2.345s][info][gc] GC(5) Pause Young (Normal) (G1 Evacuation Pause) 512M->128M(2048M) 45.123ms
│                  │     │                     │                     │       │  │        │
│                  │     │                     │                     │       │  │        └─ pause duration
│                  │     │                     │                     │       │  └─ total heap
│                  │     │                     │                     │       └─ heap after GC
│                  │     │                     │                     └─ heap before GC
│                  │     │                     └─ reason
│                  │     └─ GC type
│                  └─ GC ID
└─ timestamp
```

**Types to watch:**
| Log entry | Meaning |
|---|---|
| `Pause Young (Normal)` | Regular Minor GC |
| `Pause Young (Concurrent Start)` | Concurrent marking triggered |
| `Pause Remark` | STW remark phase |
| `Pause Cleanup` | STW cleanup phase |
| `Pause Mixed` | Mixed GC (Young + Old) |
| `Pause Full` | Full GC — **bad, investigate** |
| `To-space Exhausted` | Evacuation failed — **critical** |

### 6.3 GC Log Analysis Tools

| Tool | Use |
|---|---|
| **GCEasy** (web) | Visual GC log analysis, pause histogram |
| **GCViewer** (desktop) | Open-source GC log visualizer |
| **PerfMa** (web) | Advanced G1/ZGC analysis |
| `jstat -gcutil <pid> 1s` | Live GC stats in terminal |

---

## 7. JIT Compiler Tuning

### 7.1 Tiered Compilation (default Java 8+)

```
Level 0: Interpreter
Level 1: C1 (simple, no profiling)
Level 2: C1 (limited profiling)
Level 3: C1 (full profiling)
Level 4: C2 (aggressive optimization — inlining, escape analysis, vectorization)
```

Hot methods climb the tiers. C2 uses profiling data from C1 to make better decisions.

```bash
-XX:+TieredCompilation              # Default on; disable with -XX:-TieredCompilation
-XX:CompileThreshold=10000          # Method calls before JIT (interpreted mode)
-XX:+PrintCompilation               # Log JIT compilations (verbose, dev only)
-XX:+PrintInlining                  # Log inlining decisions
```

### 7.2 Escape Analysis

```bash
-XX:+DoEscapeAnalysis               # Default on; JIT can stack-allocate non-escaping objects
```
Objects that don't escape a method can be stack-allocated → no GC pressure.

### 7.3 Inlining

```bash
-XX:MaxInlineSize=35                # Bytecode size limit for trivial inlining (default 35)
-XX:FreqInlineSize=325              # Bytecode size limit for hot method inlining (default 325)
-XX:MaxInlineLevel=9                # Max inlining depth
```

Aggressive inlining = fewer method call overheads + better optimization context.

### 7.4 AOT / GraalVM Native Image
- **AOT (Ahead-of-Time):** compile to native binary; eliminates JIT warmup
- **Use case:** CLI tools, serverless (cold-start matters)
- **Trade-off:** less runtime optimization, longer build time

```bash
native-image -jar myapp.jar --no-fallback
```

---

## 8. Monitoring & Profiling Tools

### 8.1 Built-in CLI Tools

| Tool | Usage | What it shows |
|---|---|---|
| `jps` | `jps -lv` | List JVM processes |
| `jstat` | `jstat -gcutil <pid> 1000` | GC stats every 1s |
| `jmap` | `jmap -heap <pid>` | Heap summary |
| `jmap` | `jmap -histo <pid> \| head -30` | Object histogram (top 30 classes by count) |
| `jmap` | `jmap -dump:format=b,file=heap.hprof <pid>` | Heap dump |
| `jstack` | `jstack <pid>` | Thread dump (deadlocks, blocked threads) |
| `jcmd` | `jcmd <pid> help` | Diagnostic commands |
| `jcmd` | `jcmd <pid> GC.run` | Trigger GC |
| `jcmd` | `jcmd <pid> VM.flags` | Print all active JVM flags |
| `jcmd` | `jcmd <pid> Thread.print` | Thread dump |
| `jcmd` | `jcmd <pid> JFR.start duration=60s filename=r.jfr` | Java Flight Recorder |

### 8.2 jstat — Key Columns

```
jstat -gcutil <pid> 1000
  S0     S1     E      O      M     CCS    YGC   YGCT   FGC  FGCT    GCT
  0.00  45.00  72.00  34.00  97.00  92.00  1050  25.43    3   1.23  26.66

S0/S1  = Survivor utilization %
E      = Eden utilization %
O      = Old Gen utilization %
M      = Metaspace utilization %
YGC    = Young GC count
YGCT   = Young GC total time (seconds)
FGC    = Full GC count  ← watch this; should be near 0
FGCT   = Full GC total time
GCT    = Total GC time
```

**Signals to watch:**
- `O` continuously rising → memory leak or insufficient heap
- `FGC` increasing → Old Gen pressure, tune heap or GC
- `M` near 100% → class loader leak, set `MaxMetaspaceSize`

### 8.3 Java Flight Recorder (JFR)

Zero (near-zero) overhead profiler built into the JVM (free since Java 11).

```bash
# Start recording
jcmd <pid> JFR.start name=MyRecording duration=120s filename=/tmp/recording.jfr

# Check status
jcmd <pid> JFR.check

# Stop early
jcmd <pid> JFR.stop name=MyRecording

# Or via flag at startup
-XX:StartFlightRecording=duration=60s,filename=recording.jfr
```

Open `.jfr` with **JDK Mission Control (JMC)**.

### 8.4 async-profiler

Low-overhead sampling profiler; works without safepoint bias (unlike JFR CPU sampling).

```bash
./profiler.sh -d 30 -f flamegraph.html <pid>
# generates a flame graph showing where CPU time is spent
```

Use for CPU hotspots, lock contention, allocation pressure.

### 8.5 VisualVM / JConsole
- GUI tools bundled with JDK
- Good for development; **avoid attaching to production JVMs** (can cause pauses)
- JConsole: lightweight, MBean access
- VisualVM: heap dumps, CPU profiling, GC monitoring

---

## 9. Diagnosing Common Problems

### 9.1 OutOfMemoryError Types

| OOM message | Cause | Fix |
|---|---|---|
| `Java heap space` | Heap full — too small or memory leak | Increase `-Xmx`; analyze heap dump |
| `GC overhead limit exceeded` | >98% time in GC, <2% freed | Memory leak; increase heap; tune GC |
| `Metaspace` | Too many classes loaded | Set `-XX:MaxMetaspaceSize`; check for classloader leaks |
| `Direct buffer memory` | Off-heap `ByteBuffer` exhausted | Increase `-XX:MaxDirectMemorySize`; check NIO usage |
| `unable to create new native thread` | Too many threads or stack too large | Reduce thread count; lower `-Xss`; check thread leaks |
| `request size N bytes for reason` | Large array allocation failed | Reduce object size; increase heap |

### 9.2 Diagnosing a Heap Leak

```bash
# 1. Take heap histogram (live objects by class, no full dump needed)
jcmd <pid> GC.heap_info
jmap -histo:live <pid> | head -30

# 2. If histogram shows growth, take a heap dump
jmap -dump:format=b,file=/tmp/heap.hprof <pid>

# 3. Analyze with Eclipse MAT or JMC
#    - "Leak Suspects" report is the fastest starting point
#    - Look for: large retained objects, growing collections, classloader accumulation
```

**Common leak patterns:**
- `static` `Map`/`List` holding references forever
- Event listeners not removed
- Thread-local variables not cleared
- ClassLoader leaks (OSGi, hot-deploy frameworks)
- Caches with no eviction

### 9.3 Long GC Pauses

```bash
# Check pause durations
grep "Pause" /var/log/gc.log | awk '{print $NF}' | sort -n | tail -20

# Check if Full GCs are happening
grep "Pause Full" /var/log/gc.log | wc -l
```

**Causes & fixes:**

| Cause | Fix |
|---|---|
| Old Gen too full → Full GC | Increase `-Xmx`; tune `IHOP` for G1 |
| Humongous object allocations | Increase `G1HeapRegionSize` |
| `System.gc()` called in code | `-XX:+DisableExplicitGC` |
| Insufficient survivor space → premature promotion | Increase `SurvivorRatio`; check with `-XX:+PrintTenuringDistribution` |
| GC threads insufficient | Increase `-XX:ParallelGCThreads` |
| Fragmented heap → compaction needed | G1 reduces this; ensure Mixed GCs run |

### 9.4 Thread Deadlock

```bash
jstack <pid> | grep -A 20 "deadlock"
# or
jcmd <pid> Thread.print | grep -A 20 "Found.*deadlock"
```

JStack output will show a "Found one Java-level deadlock" section with the threads and the locks they're waiting on.

### 9.5 High CPU (Hot Methods)

```bash
# 1. Find hot thread
jstack <pid>
# Look for threads in RUNNABLE state with deep stack traces

# 2. CPU flame graph
./async-profiler/profiler.sh -d 30 -e cpu -f cpu.html <pid>

# 3. Check JIT compilation log
-XX:+PrintCompilation
# Excessive deoptimization: look for "made not entrant" / "uncommon trap"
```

### 9.6 Safe Point Bias

Standard JVM profilers sample only at safepoints → miss hot code between safepoints. Use **async-profiler** with `-e cpu` to avoid this.

---

## 10. Tuning Recipes by Use Case

### 10.1 General-Purpose Web Service (REST API)

```bash
-Xms2g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:InitiatingHeapOccupancyPercent=40
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/
-XX:+ExitOnOutOfMemoryError
-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=20m
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
```

### 10.2 Low-Latency Service (< 5ms P99 target)

```bash
-Xms4g -Xmx4g
-XX:+UseZGC                        # or UseShenandoahGC
-XX:MaxGCPauseMillis=5
-XX:+AlwaysPreTouch                # Pre-fault pages at startup; avoids page-fault latency
-XX:+DisableExplicitGC
-XX:+HeapDumpOnOutOfMemoryError
-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=20m
```

### 10.3 High-Throughput Batch Job

```bash
-Xms8g -Xmx8g
-XX:+UseParallelGC
-XX:ParallelGCThreads=8
-XX:GCTimeRatio=19                 # Target 5% time in GC (1/(1+19))
-XX:+DisableExplicitGC
-XX:+HeapDumpOnOutOfMemoryError
```

### 10.4 Memory-Constrained Container (512 MB)

```bash
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=70.0          # ~350 MB heap
-XX:+UseSerialGC                   # Lower overhead for small heap
-XX:MaxMetaspaceSize=128m
-Xss256k                           # Reduce stack size per thread
-XX:+HeapDumpOnOutOfMemoryError
-XX:+ExitOnOutOfMemoryError
```

### 10.5 Startup Optimization (CLI tools, Lambda)

```bash
# Option A: Aggressive class loading
-XX:TieredStopAtLevel=1            # Skip C2 JIT; faster startup, less peak throughput

# Option B: CDS (Class Data Sharing) — preload shared archive
java -Xshare:dump                  # generate archive
java -Xshare:on -jar myapp.jar     # use archive

# Option C: GraalVM native-image (best startup; trade-off: build time, no dynamic class loading)
native-image -jar myapp.jar
```

---

## Quick Reference Cheat Sheet

```
Memory
  -Xms / -Xmx         → heap min/max (set equal in prod)
  -Xss                → stack per thread
  -XX:MaxMetaspaceSize → class metadata cap
  -XX:MaxRAMPercentage → % of container RAM for heap

GC Choice
  Small heap / batch  → SerialGC / ParallelGC
  General (default)   → G1GC
  Sub-ms latency      → ZGC / Shenandoah

GC Tuning (G1)
  MaxGCPauseMillis         → pause goal (ms)
  IHOP (InitiatingHeapOccupancyPercent) → when to start concurrent marking
  G1HeapRegionSize         → region size (avoid humongous allocations)

Diagnostics
  jstat -gcutil <pid> 1s   → live GC utilization
  jcmd <pid> VM.flags      → all active flags
  jmap -histo:live <pid>   → object histogram
  jstack <pid>             → thread dump
  JFR + JMC                → deep profiling (zero overhead)

OOM Flags (always set in prod)
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/tmp/
  -XX:+ExitOnOutOfMemoryError

GC Logging (Java 9+)
  -Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=20m
```

---

## Common Mistakes

| Mistake | Why it's wrong | Fix |
|---|---|---|
| `-Xms` << `-Xmx` in prod | Heap grows → resizing pauses | Set equal |
| Not setting `MaxMetaspaceSize` | Metaspace OOM on class leaks | Set to 256m or more |
| Ignoring `FGC` count in jstat | Full GCs tank latency | Keep FGC near 0 |
| Reading host memory inside container | JVM over-allocates → OOM kill | Use `-XX:UseContainerSupport` + `MaxRAMPercentage` |
| Hardcoding `-Xmx` in containers | Inflexible across environments | Use `MaxRAMPercentage` |
| Using `System.gc()` in application code | Triggers Full GC uncontrollably | Disable with `DisableExplicitGC` |
| Profiling at safepoints only | Misses real hot paths | Use async-profiler |
| Not pre-touching memory (`-XX:+AlwaysPreTouch`) | Page faults at runtime under load | Pre-touch for latency-sensitive services |
