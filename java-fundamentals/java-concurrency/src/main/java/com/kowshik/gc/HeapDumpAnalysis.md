# Heap Dump Analysis — Capture & Interpret

A **heap dump** is a snapshot of the JVM heap at a point in time — all objects, their field values, and reference relationships. It's the primary tool for diagnosing memory leaks and understanding heap composition.

---

## 1. How to Capture a Heap Dump

| Method | Command/Flag | When |
|--------|-------------|------|
| Automatic on OOM | `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/path/to/dump.hprof` | Always enable in production |
| jcmd (live process) | `jcmd <pid> GC.heap_dump /path/to/dump.hprof` | On-demand, no restart |
| jmap | `jmap -dump:live,format=b,file=dump.hprof <pid>` | Deprecated for jcmd; `live` flag forces Full GC first |
| JMX programmatic | `com.sun.management:type=HotSpotDiagnostic` → `dumpHeap(path, true)` | In-app trigger on threshold |
| JVisualVM | GUI → right-click process → Heap Dump | Local development |
| Ctrl+Break (Windows) | `-XX:+HeapDumpOnCtrlBreak` | Emergency capture |

**Best practice**: `-XX:+HeapDumpOnOutOfMemoryError` should be on every production JVM. Heap dumps are large (roughly = heap size), so set `HeapDumpPath` to a volume with enough space.

---

## 2. Analyzing with Eclipse MAT (Memory Analyzer Tool)

### Open the dump

```
File → Open Heap Dump → select .hprof file
```

MAT parses the dump and presents:

### Leak Suspects Report (first thing to check)

MAT automatically identifies potential memory leak suspects:
- **Dominator tree**: what object path holds the most retained heap
- **Accumulation points**: collections/arrays growing unbounded
- **ThreadLocals**: common culprit in thread pools

### Histogram View

Shows counts and retained sizes per class:

```
Class Name                          | Objects | Shallow Heap | Retained Heap
byte[]                              | 12,456  | 1.2 GB       | 1.2 GB
java.lang.String                    | 98,234  | 3.8 MB       | 45.2 MB
com.example.CacheEntry              | 50,000  | 2.4 MB       | 890.3 MB   ← LEAK
```

**Key column: Retained Heap** — total heap that would be freed if all instances of this class were collected. A class with unexpectedly high retained heap is the leak.

### Dominator Tree

Shows the object graph ownership hierarchy:

```
Class Name                          | Retained Heap | %
byte[] (from Cache.entries)         | 890.3 MB      | 72.4%   ← dominates heap
  └─ HashMap$Node[] (Cache.entries) | 890.3 MB
      └─ Cache (static in Main)     | 890.3 MB       ← GC root
```

Walk UP the tree from large retained objects to find the GC root holding them alive.

### Path to GC Roots

Right-click any object → Path to GC Roots → exclude weak/soft references:

```
java.util.HashMap$Node ← java.util.HashMap$Node[] ← java.util.HashMap
  ← Cache.entries ← Cache ← static field Main.cache ← System Class (GC root)
```

This shows exactly WHY the object cannot be collected.

### OQL (Object Query Language)

SQL-like queries over the heap:

```sql
-- Find all Strings longer than 1000 chars
SELECT * FROM java.lang.String WHERE @retainedHeapSize > 10000

-- Find all HashMap entries for a specific class
SELECT * FROM java.util.HashMap$Node WHERE toString(key) LIKE ".*User.*"

-- Top 10 classes by instance count with retained size > 1MB
SELECT toString(s) AS class, COUNT(s) AS count, SUM(s.@retainedHeapSize) AS totalMB
FROM INSTANCEOF java.lang.Object s
WHERE s.@retainedHeapSize > 1048576
GROUP BY toString(s)
ORDER BY totalMB DESC
LIMIT 10
```

---

## 3. Common Heap Dump Patterns

### Pattern 1: byte[] dominates heap → normal for data-heavy apps

If `byte[]` dominates (50%+ of heap) in a BloomFilter, search, or caching app → normal. Check that `byte[]` instances have GC roots pointing to expected data structures.

### Pattern 2: HashMap$Node dominates → leaked cache or map

```
HashMap$Node[] → HashMap → static SomeClass.cache → GC root
```

The cache is never cleared. Fix: use `LinkedHashMap` with eviction, or `WeakHashMap`, or Guava `Cache` with TTL.

### Pattern 3: char[] or String dominates → unbounded string accumulation

Common in log aggregators, string deduplication maps, or toString() in logging that captures full state. Check if strings are being interned unintentionally.

### Pattern 4: ThreadLocal values → thread pool leak

```
ThreadLocal$ThreadLocalMap$Entry → LargeObject → Thread (pool thread, never dies)
```

Fix: always call `threadLocal.remove()` in `finally` block when done.

### Pattern 5: Finalizer queue backing up

```
java.lang.ref.Finalizer → Finalizer queue → objects with finalize()
```

Objects with `finalize()` take 2+ GC cycles to collect. If many are queued → slow Object creation or finalize() is slow. Replace `finalize()` with `Cleaner`.

---

## 4. jmap — Live Heap Histogram (no full dump)

For a quick object count without a full heap dump:

```bash
jmap -histo:live <pid> | head -30
```

```
num     #instances  #bytes  class name
1:       145322    14415664  [C          ← char arrays (String backing)
2:       98234     3143488   java.lang.String
3:       23456     1876480   [B          ← byte arrays
4:       50000     1200000   com.app.CacheEntry  ← investigate this
```

The `:live` flag forces a Full GC first (more accurate, but disruptive). Without `:live`, you get uncollected objects.

**When to use**: Quick check of what's in heap without generating a multi-GB dump file.

---

## 5. Heap Dump Best Practices

1. **Enable automatic OOM dumps** on every production JVM: `-XX:+HeapDumpOnOutOfMemoryError`
2. **Set HeapDumpPath** to a location with enough free space (at least heap size + 20%)
3. **Two-dump comparison**: take dumps 5 minutes apart during a leak. Compare: the class whose instance count keeps growing IS the leak
4. **Don't transfer dumps over network** — analyze on a machine with enough RAM (at least dump size × 1.5)
5. **Use MAT's "Leak Suspects" report first**, then drill into histogram/dominator tree
6. **Compress dumps**: `gzip dump.hprof` — they compress to ~20% of original size
7. **Don't run jmap -dump:live in production** without understanding it triggers a Full GC

---

## 6. Interview Q&A

**Q: How do you find a memory leak in production?**

> I'd enable `-XX:+HeapDumpOnOutOfMemoryError` if not already on. If the process is still alive, I'd take two heap dumps 5 minutes apart using `jcmd GC.heap_dump`. Then compare in Eclipse MAT: find classes whose instance count grew between dumps, check their Path to GC Roots to find the retaining reference, then fix the code. The `jmap -histo:live` histogram gives a quick top-N before the full dump.

**Q: What's the difference between shallow heap and retained heap?**

> Shallow heap is the size of the object itself. Retained heap is the shallow heap of the object PLUS all objects reachable ONLY through it — the total memory that would be freed if this object were collected. Dominator tree shows retained heap. A HashMap with 10,000 entries has small shallow heap (~64 bytes) but massive retained heap (all entries + keys + values).

**Q: Why shouldn't you rely on heap dumps alone for performance issues?**

> Heap dumps show spatial composition (what's in memory) but not temporal behavior (allocation rate, GC frequency, GC pause distribution). For performance, combine heap dumps with GC logs, async profiler flame graphs, and thread dumps.
