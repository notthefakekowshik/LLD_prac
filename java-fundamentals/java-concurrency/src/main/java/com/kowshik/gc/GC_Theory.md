# Garbage Collector Theory

The JVM manages memory automatically, but understanding GC deeply separates average candidates from strong ones. GC pauses, memory leaks, and tuning flags are common interview and production topics.

---

## 1. JVM Memory Layout

```
JVM Process Memory
├── Heap (GC-managed, shared by all threads)
│   ├── Young Generation
│   │   ├── Eden Space          ← new objects allocated here
│   │   ├── Survivor Space S0   ← objects that survived 1+ GCs
│   │   └── Survivor Space S1   ← objects that survived 1+ GCs
│   └── Old Generation (Tenured) ← long-lived objects promoted here
│
├── Non-Heap (NOT GC-managed for objects)
│   ├── Metaspace              ← class metadata (replaced PermGen in Java 8)
│   ├── Code Cache             ← JIT-compiled native code
│   └── Direct Memory          ← off-heap NIO ByteBuffer
│
└── Per-Thread
    ├── JVM Stack              ← local variables, stack frames
    └── PC Register            ← current instruction pointer
```

**Key insight:** Only the **Heap** holds regular Java objects and is managed by GC. Metaspace is managed by the JVM (can cause `OutOfMemoryError: Metaspace` when classes are loaded without limit). Direct memory is managed manually.

---

## 2. GC Roots — Where Reachability Starts

The GC uses **reachability** (not reference counting) to determine what is garbage. Starting from **GC roots**, it traces all reachable objects. Anything NOT reachable is garbage.

**GC Roots are:**
- Active JVM thread stacks (local variables, method parameters)
- Static fields of loaded classes
- JNI references (native code holding Java objects)
- Synchronized objects (objects used as monitor locks)
- Classes loaded by bootstrap/system classloader

```
GC Root: static field → Object A → Object B → Object C
                                 ↘ Object D

GC Root: Thread stack → Object E

Object F (no path from any root) → GARBAGE → collected
```

**Why reference counting fails:** Circular references (`A → B → A`) would never be collected even when neither A nor B is reachable from any root. JVM uses tracing GC instead.

---

## 3. Generational Hypothesis and Object Lifecycle

The generational GC is built on the empirical observation: **most objects die young**.

```
Object lifecycle:
1. Object created → Eden (most objects die here in Minor GC)
2. Survives Minor GC → Survivor S0 (age = 1)
3. Survives another Minor GC → Survivor S1 (age = 2)
4. Reaches MaxTenuringThreshold (default 15) → promoted to Old Gen
5. Old Gen fills → Major GC or Full GC
```

### Minor GC (Young Gen collection)
- Triggered when Eden is full
- Stop-The-World pause — very short (milliseconds)
- Copies surviving Eden + S0 objects to S1 (or Old Gen if too old)
- Swaps S0 and S1 roles

### Major GC (Old Gen collection)
- Triggered when Old Gen is near capacity
- Much longer pause (depends on collector)
- Collects unreachable objects in Old Gen

### Full GC
- Collects both Young and Old Gen (and Metaspace)
- Longest pause
- Often triggered by: `System.gc()`, Metaspace exhaustion, explicit GC calls, GC strategy fallback
- **Avoid in production** — tune to eliminate Full GCs

### Premature Promotion (Problem)
If a short-lived object is too large for Eden, it goes directly to Old Gen → pollutes Old Gen with garbage → triggers more Major GCs.

---

## 4. GC Algorithms — The Evolution

### Serial GC (`-XX:+UseSerialGC`)
- Single-threaded collector for all GC phases
- Stop-The-World for all collections
- Use case: single-core machines, tiny heaps (<~100MB), embedded JVMs
- Avoid in production servers

### Parallel GC / Throughput GC (`-XX:+UseParallelGC`)
- Multi-threaded Minor GC and Major GC
- Still Stop-The-World, but faster due to parallelism
- Optimizes for **throughput** (max work done, accepts longer pauses)
- Default in Java 8 for server JVMs
- Use case: batch jobs, where pause duration doesn't matter but throughput does

### CMS — Concurrent Mark Sweep (`-XX:+UseConcMarkSweepGC`) — **Deprecated Java 9, Removed Java 14**
- First "low-latency" collector — does most of Old Gen GC concurrently with app threads
- Phases: Initial Mark (STW) → Concurrent Mark → Remark (STW) → Concurrent Sweep
- Problem: **fragmentation** — CMS doesn't compact the heap → eventually needs a compacting Full GC
- Replaced by G1

### G1 GC — Garbage First (`-XX:+UseG1GC`) — **Default since Java 9**
- Divides heap into equal-sized **regions** (~2000 regions)
- Regions can be Eden, Survivor, Old, or Humongous (for large objects)
- Collects regions with most garbage first ("Garbage First")
- Concurrent marking + incremental evacuation = predictable pause times
- Goal: stay within pause time target (`-XX:MaxGCPauseMillis=200`)
- Handles large heaps well (4GB–32GB)
- Does compact during evacuation → no fragmentation like CMS

```
G1 Heap:
[E][E][S][O][O][E][H][O][E][S][E][O][O][E][E][O]
 ↑            ↑   ↑              ↑
 Eden         Old Humongous    Survivor
```

### ZGC (`-XX:+UseZGC`) — **Production-ready Java 15+**
- Concurrent: all major phases run while app threads run
- Sub-millisecond pauses regardless of heap size (tested up to 16TB!)
- Uses **colored pointers** (metadata in unused pointer bits) + **load barriers**
- Pause time: < 1ms, not proportional to heap size
- Slightly higher CPU overhead than G1
- Use case: latency-critical applications, huge heaps

### Shenandoah (`-XX:+UseShenandoahGC`) — **Production-ready Java 12+** (OpenJDK)
- Similar goals to ZGC: concurrent, low-latency
- Uses **Brooks pointers** (forwarding pointer per object) for concurrent compaction
- Sub-millisecond pauses
- Alternative to ZGC; same use cases

### Summary Table

| GC | Pauses | Throughput | Heap Size | Use Case |
|---|---|---|---|---|
| Serial | Long (STW) | Low | Small | Embedded, single-core |
| Parallel | Medium (STW, parallel) | High | Medium | Batch, CPU-bound |
| G1 | Short (tunable) | Good | 4GB–32GB | General purpose (default) |
| ZGC | Sub-ms | Good | Any (up to 16TB) | Latency-critical |
| Shenandoah | Sub-ms | Good | Any | Latency-critical (OpenJDK) |

---

## 5. Reference Types — Controlling GC Behavior

Java has four reference strengths, from strongest to weakest:

### Strong Reference (default)
```java
Object obj = new Object(); // Strong reference — never collected while reachable
```
Object is collected only when no strong reference remains.

### Soft Reference
```java
SoftReference<byte[]> soft = new SoftReference<>(new byte[1024 * 1024]);
byte[] data = soft.get(); // returns null if GC has collected it
```
- Collected **only when JVM is about to throw OutOfMemoryError**
- JVM tries to keep soft-referenced objects alive as long as memory permits
- Use case: **memory-sensitive caches** — the cache automatically shrinks under memory pressure

### Weak Reference
```java
WeakReference<Object> weak = new WeakReference<>(new Object());
Object obj = weak.get(); // returns null if GC has collected it
```
- Collected on the **next GC cycle** after no strong reference remains
- `WeakHashMap`: keys are weakly referenced — entries disappear when key is unreachable
- Use case: canonicalization maps, listener registries where listener should not prevent GC

### Phantom Reference
```java
ReferenceQueue<Object> queue = new ReferenceQueue<>();
PhantomReference<Object> phantom = new PhantomReference<>(new Object(), queue);
// phantom.get() always returns null — cannot access the referent
```
- Object is effectively unreachable before phantom ref is enqueued
- Used for **post-mortem cleanup** — knowing WHEN an object is about to be finalized
- Modern alternative to `finalize()`: `java.lang.ref.Cleaner` (Java 9+)

### Reference Queue
All reference types can be registered with a `ReferenceQueue`. When the referent is collected, the `Reference` object itself is enqueued in the queue. A monitoring thread can poll this queue to react to collections.

---

## 6. finalize() — Deprecated (Java 9) and Removed (Java 18+)

**Why finalize() is broken:**
1. **No guaranteed timing** — finalize() may run much later than when the object becomes unreachable
2. **No guaranteed execution** — JVM may exit without running finalizers
3. **Performance** — objects with finalizers require two GC cycles to collect (one to call finalizer, one to actually free)
4. **Can resurrect objects** — a finalizer can store `this` in a static field, preventing collection
5. **Thread safety** — finalizer thread is arbitrary; you must synchronize all fields

**Modern replacement: `java.lang.ref.Cleaner` (Java 9+)**
```java
Cleaner cleaner = Cleaner.create();

class Resource {
    private static class State implements Runnable {
        private final int fd;
        State(int fd) { this.fd = fd; }
        @Override public void run() {
            System.out.println("Cleaning fd: " + fd); // Close fd
        }
    }

    private final Cleaner.Cleanable cleanable;

    Resource(int fd) {
        // IMPORTANT: State must NOT hold a reference to Resource — would prevent GC
        cleanable = cleaner.register(this, new State(fd));
    }

    public void close() { cleanable.clean(); } // Explicit close (preferred)
    // If close() is never called, Cleaner calls State.run() as a safety net
}
```

Use `Cleaner` only as a safety net for `AutoCloseable` resources. Explicit `close()` in try-with-resources is always preferred.

---

## 7. Common Memory Leak Patterns

### 1. Static collection never cleared
```java
static List<Object> cache = new ArrayList<>(); // GC root — objects never collected
```

### 2. Unclosed resources (streams, connections)
```java
InputStream is = new FileInputStream("file.txt");
// Forgot to close → stream held in memory + OS file descriptor leak
```

### 3. Event listeners not removed
```java
button.addActionListener(this); // 'this' is referenced by button forever
// Fix: button.removeActionListener(this) when no longer needed
// Or: use WeakReference-based listener list
```

### 4. ThreadLocal not removed in thread pools
```java
threadLocal.set(new LargeObject());
// Thread is reused from pool — LargeObject lives until thread dies (= forever in pool)
// Fix: always threadLocal.remove() in finally block
```

### 5. Inner class holding outer class reference
```java
class Outer {
    class Inner { } // Inner holds implicit reference to Outer instance
    // Outer cannot be GC'd as long as Inner is reachable
    // Fix: use static nested class
}
```

### 6. Classloader leak (common in app servers)
Dynamic classloaders (e.g., web app hot reload) hold references to classes. If any object loaded by the classloader is still reachable, the entire classloader — and ALL its loaded classes — cannot be GC'd.

---

## 8. Key JVM Flags

```bash
# GC algorithm selection:
-XX:+UseG1GC              # G1 (default Java 9+)
-XX:+UseZGC               # ZGC (Java 15+ GA)
-XX:+UseShenandoahGC      # Shenandoah (OpenJDK)
-XX:+UseSerialGC          # Serial
-XX:+UseParallelGC        # Parallel

# Heap sizing:
-Xms512m                  # Initial heap size
-Xmx4g                    # Max heap size
-XX:NewRatio=2            # Old/Young ratio (default: Old = 2x Young)
-XX:SurvivorRatio=8       # Eden/Survivor ratio (default: Eden = 8x each Survivor)

# G1-specific:
-XX:MaxGCPauseMillis=200  # Target max pause (G1 tries to stay under this)
-XX:G1HeapRegionSize=16m  # Region size (1m to 32m, power of 2)

# GC logging (Java 9+):
-Xlog:gc*:file=gc.log     # Log all GC events to file
-Xlog:gc+heap=debug       # Heap before/after each GC

# Diagnostic:
-XX:+PrintGCDetails       # (pre-Java 9) verbose GC output
-verbose:gc               # Basic GC output
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heap.hprof
```

---

## 9. GC Monitoring — Programmatic

```java
// Get all GC MXBeans:
List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
for (GarbageCollectorMXBean bean : gcBeans) {
    System.out.println(bean.getName() + ": count=" + bean.getCollectionCount()
        + " time=" + bean.getCollectionTime() + "ms");
}

// Memory pools:
List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();
for (MemoryPoolMXBean pool : memPools) {
    MemoryUsage usage = pool.getUsage();
    System.out.printf("%s: used=%dMB, max=%dMB%n",
        pool.getName(), usage.getUsed()/1_000_000, usage.getMax()/1_000_000);
}

// GC Notification listener (Java 7+):
for (GarbageCollectorMXBean bean : gcBeans) {
    ((NotificationEmitter) bean).addNotificationListener((notification, handback) -> {
        GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo
            .from((CompositeData) notification.getUserData());
        System.out.printf("GC: %s cause=%s duration=%dms%n",
            info.getGcName(), info.getGcCause(), info.getGcInfo().getDuration());
    }, null, null);
}
```

---

## 10. Interview Q&A

**Q: What is the difference between Minor GC and Full GC?**
A: Minor GC collects only the Young Generation (Eden + Survivors). It's short and frequent. Full GC collects both Young and Old Gen (and Metaspace), compacts the heap, and takes much longer. Full GC is a red flag in production — tune to avoid it.

**Q: Why does generational GC exist? Why not just collect the whole heap each time?**
A: Most objects die young (generational hypothesis). Collecting only the small Young Gen is much faster than collecting the entire heap. Long-lived objects in Old Gen are rarely garbage, so collecting them less frequently is efficient.

**Q: What causes `OutOfMemoryError: Java heap space`?**
A: Heap is full and GC cannot free enough space. Causes: genuine memory leak (objects accumulate in static/long-lived collections), heap too small for workload, large object allocations exceeding available space.

**Q: What causes `OutOfMemoryError: Metaspace`?**
A: Too many classes loaded (e.g., dynamic code generation, classloader leak in hot-reload scenarios). Fix: set `-XX:MaxMetaspaceSize=256m` to cap it, and investigate classloader leaks.

**Q: What is a memory leak in Java? Isn't GC supposed to prevent them?**
A: GC prevents collection of unreachable objects — but it cannot collect reachable-but-logically-garbage objects. A memory leak in Java is when objects accumulate in long-lived references (static collections, caches, event listeners) that are never cleared. The GC cannot collect them because they're still reachable.

**Q: Difference between `WeakHashMap` and `HashMap`?**
A: `WeakHashMap` holds keys with weak references. When a key has no strong reference outside the map, GC can collect it — the entry is then automatically removed. Useful for caches keyed by objects that may become unreachable.

**Q: What is Stop-The-World (STW)?**
A: All application threads are paused so the GC can safely traverse the object graph. Modern GCs (G1, ZGC, Shenandoah) minimize STW phases — ZGC achieves sub-millisecond STW pauses by doing most work concurrently.

**Q: Why can't you rely on `finalize()` to close resources?**
A: (1) No guaranteed timing — finalize may run long after the object is unreachable. (2) No guaranteed execution — JVM may exit without running finalizers. (3) Objects with finalizers take two GC cycles to collect. Use `try-with-resources` + `AutoCloseable` instead.

**Q: What is GC tuning?**
A: Adjusting heap sizes, GC algorithm, region sizes, pause targets, and logging to meet latency/throughput requirements. Process: measure GC pauses with `-Xlog:gc*`, identify whether issue is Minor/Major/Full GC frequency or duration, then adjust heap ratios or switch GC algorithm.
