# Virtual Threads (Project Loom) — Deep Dive & Production Guide

> **Prerequisites:** Read `ExecutorTuning_Theory.md` to understand platform thread limits. Virtual threads are the solution to those constraints.

---

## 1. The Problem: Why Platform Threads Don't Scale

### The 1MB-per-Thread Tax

Every `java.lang.Thread` (platform thread) is a **wrapper around an OS thread**. The OS allocates:
- **Stack memory**: ~512KB–1MB per thread (configurable via `-Xss`)
- **Kernel resources**: Thread control blocks, scheduling metadata
- **Context switch overhead**: CPU cycles spent saving/restoring registers

### The Math of Failure

```
Typical server: 8 cores, 32 GB RAM

OS thread limit (kernel parameter): ~10,000–30,000 threads
JVM with 1MB stack: 10,000 threads × 1MB = 10GB of virtual memory
                              ↓
Actual safe limit: ~2,000–5,000 concurrent threads before:
  - Native OOM (cannot create native thread)
  - Thrashing (context switches dominate CPU)
  - GC pressure from massive thread metadata
```

### Why Reactive Programming Emerged

```java
// The problem: 10,000 concurrent HTTP requests
// With platform threads: need 10,000 threads → JVM crashes

// "Solution": Reactive (Netty, WebFlux, RxJava)
// Use FEW threads (event loop) + NON-BLOCKING callbacks
fetchUserAsync(userId)
    .flatMap(user -> fetchOrdersAsync(user.getId()))  // callback hell
    .flatMap(orders -> enrichAsync(orders))
    .subscribe(result -> response.write(result));
```

**Cost of reactive:**
- Callback hell / functional pipeline complexity
- Debugging stack traces become unreadable
- Blocking libraries (JDBC, Hibernate) cannot be used directly
- Cognitive overhead: " colored functions" — async code looks different from sync code

---

## 2. The Virtual Thread Solution

### Core Concept: JVM-Managed, Not OS-Managed

```
┌─────────────────────────────────────────────────────────────────────┐
│                         VIRTUAL THREAD (JVM)                        │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Lightweight: ~few KB stack (growable, starts at ~256 bytes) │   │
│  │  Created/managed by JVM, not OS                             │   │
│  │  Millions possible on same hardware                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                      │
│                              ▼ Mount/Unmount                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              CARRIER THREAD (OS Platform Thread)              │   │
│  │                 ~1 per CPU core (from ForkJoinPool)           │   │
│  │              Virtual thread "pins" here while running         │   │
│  │              Unmounts when blocking I/O occurs                │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### The Blocking Magic

```java
// OLD: Blocking JDBC call on platform thread
// Thread sits doing nothing, consuming 1MB memory for 50ms

// NEW: Same code on virtual thread
// JVM detects blocking I/O → UNMOUNTS virtual thread from carrier
// Carrier picks up another virtual thread to run
// When I/O completes → virtual thread remounts on any available carrier

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        // This is a BLOCKING call on a virtual thread — totally fine!
        ResultSet rs = stmt.executeQuery("SELECT ...");  // JDBC
        // 10,000 of these run concurrently with only 8 carrier threads
    });
}
```

### Key Differences Summary

| Aspect | Platform Thread | Virtual Thread |
|--------|----------------|----------------|
| **Managed by** | Operating System | JVM |
| **Stack size** | Fixed ~1MB (contiguous) | Growable, starts ~KB |
| **Creation cost** | ~1ms (syscall) | ~1μs (JVM heap allocation) |
| **Max count** | Thousands | Millions |
| **Context switch** | OS kernel mode (~μs) | JVM user mode (~ns) |
| **Blocking I/O** | Thread blocks, wastes OS resource | Thread unmounts, carrier freed |
| **Code style** | Blocking or reactive callbacks | Just write blocking code |

---

## 3. Creating Virtual Threads

### Method 1: Thread.ofVirtual() (Low-level)

```java
// Create and start directly
Thread vt = Thread.ofVirtual()
    .name("my-virtual-thread-")
    .start(() -> {
        System.out.println("Running on: " + Thread.currentThread());
    });

vt.join(); // Wait for completion

// Check if current thread is virtual
boolean isVirtual = Thread.currentThread().isVirtual(); // true/false
```

### Method 2: Virtual Thread Executor (Recommended)

```java
// One virtual thread per submitted task
// Auto-shutdown with try-with-resources (Java 19+)
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    
    for (int i = 0; i < 100_000; i++) {
        final int taskId = i;
        executor.submit(() -> processTask(taskId)); // Each gets own virtual thread
    }
    
} // Blocks here until all 100,000 tasks complete
```

### Method 3: Factory for Customization

```java
ThreadFactory factory = Thread.ofVirtual()
    .name("worker-", 0)  // worker-0, worker-1, ...
    .factory();

ExecutorService executor = Executors.newThreadPerTaskExecutor(factory);
```

---

## 4. The Pinning Problem — Virtual Threads' Achilles' Heel

### What is Pinning?

**Pinning** occurs when a virtual thread **cannot unmount** from its carrier thread during a blocking operation. The carrier remains blocked, defeating the entire purpose of virtual threads.

```java
// BAD: synchronized block pins the virtual thread
Object lock = new Object();

Thread.ofVirtual().start(() -> {
    synchronized (lock) {  // ← PINNING STARTS HERE
        // Virtual thread CANNOT unmount while holding monitor
        Thread.sleep(1000);  // Carrier blocked for full 1 second!
    }  // ← PINNING ENDS
});
```

### Why Does Pinning Happen?

The JVM cannot unmount a virtual thread that holds a **synchronized monitor** because:
- The monitor is associated with the **carrier thread's** internal state
- Moving the virtual thread to another carrier would orphan the monitor
- Could cause deadlocks if another thread is waiting on that monitor

### The Fix: Replace `synchronized` with `ReentrantLock`

```java
// GOOD: ReentrantLock allows unmounting
ReentrantLock lock = new ReentrantLock();

Thread.ofVirtual().start(() -> {
    lock.lock();
    try {
        // Virtual thread CAN unmount during this sleep!
        // Carrier is freed to run other virtual threads
        Thread.sleep(1000);
    } finally {
        lock.unlock();
    }
});
```

### Detection: JVM Flag

```bash
# Full stack trace when pinning detected
java -Djdk.tracePinnedThreads=full MyApp

# Short summary only
java -Djdk.tracePinnedThreads=short MyApp
```

**Output example:**
```
Pinned thread: VirtualThread[#23]/runnable@ForkJoinPool-1-worker-3
    at java.base/java.lang.VirtualThread$VThreadContinuation.onPinned(...)
    at MyService.process(MyService.java:42)  // ← Fix this synchronized block
```

### Pinning Checklist for Code Review

| Dangerous Pattern | Safe Replacement |
|-------------------|------------------|
| `synchronized` method/block | `ReentrantLock` |
| `Object.wait()` / `notify()` | `Condition` from `ReentrantLock` |
| Native code with JNI locks | Restructure to minimize hold time |
| `Thread.holdsLock()` | Avoid — indicates synchronization |
| `java.io.FileInputStream` read (native) | Use `java.nio` channels where possible |

---

## 5. Excellent Fit vs Poor Fit

| Excellent Fit | Poor Fit |
|---------------|----------|
| HTTP request handling | CPU-heavy analytics |
| DB + network I/O apps | Tight lock-heavy systems |
| Microservices waiting on APIs | Native blocking legacy code |
| Simplifying async code | Extreme low-latency trading loops |

---

## 6. When NOT to Use Virtual Threads

### CPU-Bound Work (No Benefit)

```java
// BAD use case: Pure computation
Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
    // Prime number calculation — no blocking I/O
    return calculatePrimesUpTo(1_000_000);
});
// Virtual threads add overhead with zero benefit here
// Use ForkJoinPool.commonPool() or CPU-bound executor instead
```

### Synchronization-Heavy Codebases

```java
// BAD: Legacy code full of synchronized blocks
// Every block causes pinning → carriers blocked
// Better to stay with platform threads until refactoring complete
```

### Thread-Local Assumptions

```java
// DANGEROUS: Code assuming ThreadLocal stays on same carrier
ThreadLocal<String> context = new ThreadLocal<>();

Thread.ofVirtual().start(() -> {
    context.set("user-123");
    blockingCall();  // Virtual thread may migrate to different carrier!
    String user = context.get();  // Still works (JVM handles TL), BUT...
    
    // ThreadLocal is now SLOWER (lookup via carrier + virtual thread)
    // ScopedValue (Java 21+) is the modern replacement
});
```

### What About ThreadPoolExecutor?

```java
// DO NOT do this — virtual threads should not be pooled!
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    100, 10_000, ...  // Virtual threads are cheap, don't pool them
);

// Virtual threads are meant to be short-lived and abundant
// The "per task executor" pattern is correct:
Executors.newVirtualThreadPerTaskExecutor();  // Unlimited, per-task threads
```

---

## 7. Structured Concurrency with `StructuredTaskScope`

### The Problem with "Fire and Forget"

```java
// DANGEROUS: Task leak — what if fetchUser() throws?
CompletableFuture<String> user = CompletableFuture.supplyAsync(() -> fetchUser(id));
CompletableFuture<String> orders = CompletableFuture.supplyAsync(() -> fetchOrders(id));

// If fetchUser throws after 10ms, fetchOrders keeps running for 500ms
// Wasted resources, potential consistency issues
```

### StructuredTaskScope: Parent Waits for Children

```java
// Java 21+ (finalized in Java 23)
// Parent scope cannot close until ALL forked tasks complete or are cancelled

try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<String> user = scope.fork(() -> fetchUser(id));
    Subtask<String> orders = scope.fork(() -> fetchOrders(id));
    
    scope.join();           // Wait for both
    scope.throwIfFailed();  // If either failed, exception thrown here
    
    // Both succeeded — safe to use results
    return new Response(user.get(), orders.get());
    
} // ← Scope closes: any running tasks are auto-cancelled. NO LEAKS.
```

### Policy: ShutdownOnSuccess (Race for First Result)

```java
try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
    scope.fork(() -> queryReplica("replica-1"));  // 500ms
    scope.fork(() -> queryReplica("replica-2"));  // 100ms ← wins
    scope.fork(() -> queryReplica("replica-3"));  // 300ms
    
    String result = scope.result();  // Returns replica-2 result
    // Other two forks automatically cancelled
    return result;
}
```

---

## 8. Virtual Threads ≠ Infinite Scalability Trap

### The Myth

> "Virtual threads give us unlimited scalability — we can just spawn millions with no limits!"

**This is dangerously wrong.**

### The Reality: Shared Resources Still Limit You

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1_000_000; i++) {
        executor.submit(() -> {
            // Each virtual thread opens a DB connection
            Connection conn = dataSource.getConnection();  // ← BOTTLENECK
            // ... use connection ...
        });
    }
}
// Result: 1,000,000 threads competing for 50 DB connections
// Massive contention, timeouts, thundering herd
```

### What Virtual Threads Actually Solve

| Problem | Virtual Threads Solve? |
|---------|------------------------|
| Thread creation cost / memory | ✅ Yes — cheap, millions possible |
| Context switch overhead | ✅ Yes — user-mode scheduling |
| Blocking I/O efficiency | ✅ Yes — carriers shared across waits |
| Callback complexity | ✅ Yes — write blocking code again |
| Database connection limits | ❌ No — external resources unchanged |
| Network bandwidth | ❌ No — still finite |
| Heap memory for task data | ❌ No — objects still allocated |
| CPU cores for actual work | ❌ No — computation still needs CPUs |

### The Correct Mental Model

```
Virtual threads remove the ARTIFICIAL constraint of OS thread limits.
They do NOT remove REAL constraints of hardware and external systems.

BEFORE Virtual Threads:
  10,000 HTTP requests → need 10,000 OS threads → JVM dies
  Constraint: OS thread memory/stack

AFTER Virtual Threads:
  10,000 HTTP requests → 10,000 virtual threads, 8 carriers → JVM happy
  BUT: 10,000 DB queries → DB connection pool exhausted → DB dies
  Constraint: DB connection pool (unchanged!)
```

### Production Safeguards

```java
// Even with virtual threads, use semaphores for external resources
Semaphore dbPermits = new Semaphore(50);  // Match connection pool size

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        executor.submit(() -> {
            dbPermits.acquire();  // Limit concurrent DB access
            try {
                // DB work here — only 50 concurrent
            } finally {
                dbPermits.release();
            }
        });
    }
}
```

> **Runnable Demo:** See `VirtualThreadsDbBottleneckDemo.java` for full working code showing:
> - Unbounded concurrency (the 1M threads / 30 DB connections problem)
> - Semaphore-guarded fix
> - Caching, batching, and backpressure patterns

---

## 9. Real-World Case Study: The "Silent Death" Scenario

> **Scenario:** Spring Boot on Java 21. You enable virtual threads for Tomcat (`spring.threads.virtual.enabled=true`). Under load, the service stops responding. JVM is alive, no OOM, no crash. What went wrong?

### The Most Likely Culprit: Pinning Deadlock

```
Tomcat with virtual threads enabled:
  HTTP Request 1 → Virtual Thread VT-1 starts processing
  HTTP Request 2 → Virtual Thread VT-2 starts processing
  ...
  HTTP Request N → Virtual Thread VT-N starts processing

All virtual threads run on a small pool of carrier threads
(typically 1 per CPU core, from ForkJoinPool.commonPool).

Suppose your request handler contains:

@Controller
public class OrderController {
    private final Object lock = new Object();

    @GetMapping("/order/{id}")
    public Order getOrder(@PathVariable Long id) {
        synchronized (lock) {              // ← PINNING STARTS
            // Virtual thread CANNOT unmount!
            Order order = orderService.fetch(id);  // Blocking DB call
            return order;
        }                                 // ← PINNING ENDS
    }
}

Now under load:
  VT-1 enters synchronized(lock), blocks on DB call → CARRIER-1 PINNED
  VT-2 enters synchronized(lock), blocks on DB call → CARRIER-2 PINNED
  ...
  VT-N enters synchronized(lock), blocks on DB call → CARRIER-N PINNED

Result: All carrier threads are pinned and blocked.
No carrier is free to run other virtual threads.
The JVM is alive (no OOM — virtual threads are cheap!)
but NO NEW REQUESTS can be processed.
The service appears "dead" while the process is technically healthy.
```

### Why This Happens

| Platform Threads | Virtual Threads |
|---|---|
| 200 threads, 50 blocked on `synchronized` → 150 still working | 8 carriers, ALL pinned by `synchronized` → **0 capacity left** |
| Degraded performance, not complete death | **Complete throughput collapse to zero** |

With platform threads, partial blocking is tolerable because you have many threads. With virtual threads, **pinning even a few carriers kills the entire throughput** because there are very few carriers.

### The Fix

```java
@Controller
public class OrderController {
    private final ReentrantLock lock = new ReentrantLock();

    @GetMapping("/order/{id}")
    public Order getOrder(@PathVariable Long id) {
        lock.lock();
        try {
            // Virtual thread CAN unmount during DB call!
            Order order = orderService.fetch(id);
            return order;
        } finally {
            lock.unlock();
        }
    }
}
```

### Other Possible Causes (Less Likely)

| Symptom | Likely Cause | Detection |
|---------|-------------|-----------|
| Complete stop, no errors | **Pinning deadlock** (synchronized + blocking) | `-Djdk.tracePinnedThreads=full` |
| Timeouts, slow degradation | External resource exhaustion (DB pool, HTTP client pool) | Monitor connection pool metrics |
| Thread dump shows threads waiting on same monitor | Classic deadlock (not pinning-specific) | `jstack` analysis |

### Diagnostic Steps

```bash
# 1. Enable pinning detection in production
java -Djdk.tracePinnedThreads=short -jar app.jar

# 2. Check thread dump — look for ForkJoinPool workers blocked in synchronized
jstack <pid> | grep -A 5 "ForkJoinPool"

# 3. Look for these red flags in logs:
# "Pinned thread: VirtualThread[#...]" → immediate action needed

# 4. Temporarily disable virtual threads to confirm
# spring.threads.virtual.enabled=false
# If service recovers → pinning is confirmed
```

---

## 10. Migration Checklist from Platform Threads

| Step | Action | Priority |
|------|--------|----------|
| 1 | Profile your workload — is it I/O bound? | Must |
| 2 | Run with `-Djdk.tracePinnedThreads=full` | Must |
| 3 | Replace `synchronized` with `ReentrantLock` | Must |
| 4 | Audit `ThreadLocal` usage — migrate to `ScopedValue` | Should |
| 5 | Update connection pool sizing (may need reduction) | Should |
| 6 | Review timeout configurations (virtual threads tolerate higher) | Can |
| 7 | Load test with realistic external resource constraints | Must |

---

## 11. Quick Reference: Syntax & APIs

```java
// Java 21+ — Virtual Threads GA

// Create and run
Thread vt = Thread.ofVirtual().start(runnable);
Thread vt = Thread.ofVirtual().name("vt-").start(runnable);

// Executor (recommended)
try (ExecutorService e = Executors.newVirtualThreadPerTaskExecutor()) { }

// Factory
ThreadFactory tf = Thread.ofVirtual().factory();

// Inspection
boolean isVirtual = Thread.currentThread().isVirtual();
Thread currentCarrier = Thread.currentThread().getClass(); // No direct API

// Structured Concurrency (Java 23+ finalized, 21+ preview)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) { }
try (var scope = new StructuredTaskScope.ShutdownOnSuccess<T>()) { }

// ScopedValue (replacement for ThreadLocal, Java 21+)
ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
ScopedValue.where(REQUEST_ID, "abc-123").run(() -> {
    // Within this scope, REQUEST_ID is bound
    String id = REQUEST_ID.get();
});
```

---

## 12. Summary: Key Interview Points

1. **Virtual threads are JVM-managed, not OS-managed** — millions vs. thousands
2. **They unmount from carriers during blocking I/O** — the core efficiency gain
3. **Pinning via `synchronized` is the #1 gotcha** — use `ReentrantLock`
4. **Virtual threads excel at I/O-bound concurrency, not CPU-bound work**
5. **External resource limits still apply** — DB connections, bandwidth, heap
6. **Structured concurrency prevents task leaks** — parent scope owns child lifetimes
7. **ScopedValue replaces ThreadLocal** — more efficient with virtual thread migration


### Interview-Level One-Liner

> **Virtual threads improve concurrency scalability for blocking I/O workloads, but they do not increase CPU parallelism and still require careful control of scarce resources like DB pools, locks, and downstream services.**