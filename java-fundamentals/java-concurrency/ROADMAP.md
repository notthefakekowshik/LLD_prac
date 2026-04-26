# Java Concurrency Mastery Roadmap

> **This is the primary trusted source for Java concurrency interview preparation.**
> Every concept, every pattern, every gotcha worth knowing is covered here.
> Do not split attention across external blogs or other notes — trust this repo.

Complete phases in order. Each phase builds on the previous one.

---

## Completion Status

| Phase | Status |
|---|---|
| Phase 1 — Threads & JMM | ✅ Complete |
| Phase 2 — Synchronization & Locking | ✅ Complete |
| Phase 3 — JUC Toolkit | ✅ Complete |
| Phase 4.1 — Concurrency Patterns | ✅ Complete |
| Phase 4.2 — Advanced Systems | ✅ Complete |
| Phase 4.3 — LeetCode Concurrency | ⬜ Not started |
| Phase 5 — GC | ✅ Complete |

---

## Session Log

### 2026-04-26
**Theory gaps filled:**
- `Executors_Theory.md` — rewrote all 6 OOTB pool entries with full detail (production warnings, internals, best-for guidance); added `newVirtualThreadPerTaskExecutor` (Java 21) as 6th pool; added `scheduleAtFixedRate` vs `scheduleWithFixedDelay` comparison table; added work-stealing algorithm diagram; added daemon thread explanation for `WorkStealingPool`; added full OOTB comparison table; added Interview Q&A section (8 questions); added Demo Files Reference section (Section 8)

**New demo files created:**
- `executors/FixedThreadPoolDemo.java` — queue backlog proof, thread reuse across batches, bounded production-safe variant
- `executors/CachedThreadPoolDemo.java` — elasticity, thread reuse between waves, OOM risk, bounded safe alternative, live thread-count drain with background monitor
- `executors/SingleThreadExecutorDemo.java` — sequential ordering, thread resurrection after crash, proxy cast protection vs `newFixedThreadPool(1)`, serialized log writer
- `executors/ScheduledThreadPoolDemo.java` — one-shot delay, fixed-rate, fixed-delay, slow-task rate vs delay comparison, silent suppression + fix
- `executors/WorkStealingPoolDemo.java` — throughput vs fixed pool, divide-and-conquer parallel sum, daemon thread proof, uneven work stealing
- `executors/VirtualThreadExecutorDemo.java` — 10k tasks in ~100ms, I/O throughput comparison, isVirtual/isDaemon, carrier pinning `synchronized` vs `ReentrantLock`

---

**Extra files added (beyond roadmap):**
- `locks/Locks_DeepDive.md` — intrinsic/extrinsic tree, `synchronized` vs `Condition`, pessimistic/optimistic locking
- `locks/SemaphoreVsMutexDemo.java` — `Semaphore(1)` ownership bug vs `ReentrantLock`
- `threads/ThreadLocalLeakDemo.java` — data bleeding, large-object leak, unique-TL-per-request anti-pattern
- `advanced/AbstractQueuedSynchronizer_Theory.md` — AQS internals, custom synchronizer code, Q&A
- `advanced/VirtualThreads_Theory.md` — virtual threads, pinning, structured concurrency
- `advanced/HikariCP_PoolSizing_Theory.md` — connection pool sizing under virtual threads
- `executors/BlockingQueue_Theory.md` — extended with `SynchronousQueue`, `PriorityBlockingQueue`, `LinkedTransferQueue`, full comparison table, Q&A

**Remaining gaps:**
- ⬜ `leetcodeconcurrency/` package — all 4 problems not yet implemented
- ⬜ `volatile` double-checked locking demo — broken pattern vs correct `volatile` singleton
- ⬜ Thread interruption contract demo — `interrupt()`, `isInterrupted()`, restore vs propagate

---

## Phase 1 — Foundations: Threads and the Java Memory Model

Master how threads are born, run, and die. Build the correct mental model of Java's memory visibility guarantees before touching any synchronization primitive.

### 1.1 Thread Fundamentals
| File | What it covers |
|---|---|
| `threads/ThreadCreationDemo.java` | Thread vs Runnable vs Callable vs lambda; daemon threads; thread naming |
| `threads/ThreadLifecycleDemo.java` | All 6 states (NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → TERMINATED) with real transitions |
| `threads/ThreadLocalDemo.java` | Thread-confined state; memory leak pattern in thread pools; InheritableThreadLocal |
| `threads/ThreadLocalLeakDemo.java` | Data bleeding between requests; large object leak; unique-ThreadLocal-per-request anti-pattern; correct remove() pattern |

### 1.2 Java Memory Model
| File | What it covers |
|---|---|
| `synchronization/VolatileDemo.java` | CPU cache vs main memory; volatile visibility guarantee; why `count++` is NOT safe with volatile |

**Theory:** `threads/Threads_Theory.md`

**Interview checkpoint:** Explain happens-before, visibility vs atomicity, and why `volatile` alone cannot make `count++` thread-safe.

---

## Phase 2 — Synchronization and Locking

Move from raw `synchronized` blocks through the full `java.util.concurrent.locks` spectrum. Understand deadlocks — how they happen and how to prevent them.

### 2.1 Intrinsic Locks (synchronized)
| File | What it covers |
|---|---|
| `synchronization/SynchronizationDemo.java` | synchronized method vs block; fine-grained locking; lock on wrong object pitfall |
| `synchronization/ConditionDemo.java` | `BoundedBuffer` with `Condition.await/signal`; thundering herd; `BetterBlockingQueue` with executor |

### 2.2 Explicit Locks
| File | What it covers |
|---|---|
| `locks/LockDemo.java` | Intrinsic vs explicit lock comparison |
| `locks/ReentrantLockDemo.java` | `tryLock`, timed lock, `lockInterruptibly`, fairness |
| `locks/ReadWriteLockDemo.java` | Multiple readers / single writer; downgrade allowed, upgrade not |
| `locks/StampedLockDemo.java` | Optimistic reads; `validate(stamp)`; NOT reentrant |

### 2.3 Concurrency Hazards
| File | What it covers |
|---|---|
| `synchronization/DeadlockDemo.java` | Deliberate deadlock; lock ordering avoidance; tryLock timeout avoidance; programmatic detection via ThreadMXBean |

**Theory:** `synchronization/Synchronization_Theory.md`, `locks/Locks_Theory.md`

**Interview checkpoint:** Implement a thread-safe bounded buffer using `ReentrantLock + Condition`. Explain why `signal()` is better than `notifyAll()` here.

---

## Phase 3 — The java.util.concurrent Toolkit

The high-level utilities you use in production every day. Master all of them.

### 3.1 Atomic Variables and CAS
| File | What it covers |
|---|---|
| `atomics/AtomicVariablesDemo.java` | `AtomicInteger`, `AtomicReference`, `LongAdder`; non-blocking Treiber stack |
| `atomics/CASDemo.java` | CAS hardware semantics; manual CAS loop; ABA problem; `AtomicStampedReference` fix |

**Theory:** `atomics/Atomics_Theory.md`

### 3.2 Coordination Primitives
| File | What it covers |
|---|---|
| `coordination/CountDownLatchDemo.java` | Wait-for-N; starting-gun pattern; one-shot (cannot reset) |
| `coordination/CyclicBarrierDemo.java` | Reusable barrier; barrier action; multi-phase computation |
| `coordination/BrokenBarrierExceptionDemo.java` | Barrier failure propagation; broken state handling |
| `coordination/SemaphoreDemo.java` | Resource pool; connection limiter; any-thread-can-release semantics |
| `coordination/PhaserDemo.java` | Dynamic party registration; tiered termination via `onAdvance` |
| `coordination/ExchangerDemo.java` | Pairwise thread data handoff; double-buffering pipeline |
| `coordination/ZeroOneTwoPatternPrinter.java` | Classic semaphore coordination puzzle |

**Theory:** `coordination/Coordination_Theory.md`

### 3.3 Executors and Thread Pools
| File | What it covers |
|---|---|
| `executors/ExecutorServiceDemo.java` | `ExecutorService` API; submit vs execute; shutdown vs shutdownNow |
| `executors/CustomThreadPoolDemo.java` | `ThreadPoolExecutor` parameters; rejection policies; saturation |
| `executors/ForkJoinPoolDemo.java` | Fork-join framework; work stealing; `RecursiveTask` |
| `executors/ThreadFactoryDemo.java` | Custom thread naming; daemon factory |
| `executors/ThreadFactoryDemoImproved.java` | Improved factory with UncaughtExceptionHandler |
| `executors/FixedThreadPoolDemo.java` | Queue backlog; thread reuse; production-safe bounded variant |
| `executors/CachedThreadPoolDemo.java` | Elasticity; thread reuse; OOM risk; bounded safe alternative |
| `executors/SingleThreadExecutorDemo.java` | Sequential ordering; thread resurrection; proxy protection; serialized writer |
| `executors/ScheduledThreadPoolDemo.java` | schedule once; fixedRate vs fixedDelay; slow task comparison; silent suppression fix |
| `executors/WorkStealingPoolDemo.java` | Throughput vs fixed pool; divide-and-conquer; daemon threads; uneven task stealing |
| `executors/VirtualThreadExecutorDemo.java` | 10k tasks; I/O throughput; isVirtual; carrier pinning with synchronized vs ReentrantLock |

**Theory:** `executors/Executors_Theory.md`, `executors/BlockingQueue_Theory.md`

### 3.4 Async Programming
| File | What it covers |
|---|---|
| `future/CompletebleFutureDemo.java` | `CompletableFuture` API overview; chaining; exception handling |
| `future/CompletableFutureRunAsyncPrac.java` | `runAsync` patterns |
| `future/CompletableFutureSupplyAsyncPrac.java` | `supplyAsync` chains; `thenApply`, `thenCombine`, `allOf` |

**Theory:** `future/Future_CompletableFuture_Theory.md`

### 3.5 Concurrent Collections
| File | What it covers |
|---|---|
| `collections/ConcurrentHashMapDemo.java` | Java 8 node-level locking; `computeIfAbsent`; no null keys |
| `collections/ConcurrentSkipListMapDemo.java` | Lock-free sorted map; skip list structure; use cases |

**Theory:** `collections/Collections_Theory.md`

**Interview checkpoint:** Explain lock striping in `ConcurrentHashMap`. Why is `ConcurrentSkipListMap` preferred over a `Collections.synchronizedMap(new TreeMap())`?

---

## Phase 4 — Patterns and Advanced Topics

Production-grade system implementations and modern Java concurrency.

### 4.1 Concurrency Patterns
| File | What it covers |
|---|---|
| `patterns/ProducerConsumerDemo.java` | `BlockingQueue`; `wait/notify`; poison pill termination; priority queue variant |
| `patterns/RateLimiter.java` | Token bucket with CAS; thread-safe refill logic |
| `patterns/ConcurrentCacheDemo.java` | TTL cache with `ReadWriteLock`; background cleanup; eviction |
| `patterns/DataPipelineBarrierDemo.java` | ETL pipeline; `CyclicBarrier` for multi-stage synchronization |

**Theory:** `patterns/Patterns_Theory.md`

### 4.2 Advanced Systems
| File | What it covers |
|---|---|
| `advanced/DistributedTaskSchedulerDemo.java` | Leader election; work stealing; health monitoring with heartbeat |
| `advanced/ParallelWebCrawlerDemo.java` | `CompletableFuture` fan-out; `ConcurrentHashMap` deduplication; rate limiting |
| `advanced/SortMeMacMiniDaddy.java` | `Arrays.parallelSort` vs sequential; `ForkJoinPool.commonPool` internals |
| `advanced/VirtualThreadsDemo.java` | Java 21 virtual threads; pinning; `StructuredTaskScope` |

**Theory:** `advanced/Advanced_Theory.md`

### 4.3 LeetCode Concurrency (Applied Practice)

Solve these to validate understanding of synchronization primitives under interview pressure.

| Problem | Files | Key Concepts |
|---|---|---|
| [1114] Print In Order | `leetcodeconcurrency/DesignPrintInOrder/` | `CountDownLatch`, `Semaphore` |
| [1115] Print FizzBuzz Multithreaded | `leetcodeconcurrency/DesignFizzBuzzMultiThreaded/` | `Semaphore`, `AtomicInteger` |
| [1188] Design Bounded Blocking Queue | `leetcodeconcurrency/DesignBoundedBlockingQueue/` | `synchronized`, two-lock optimization |
| [1116] Print Zero Even Odd | `leetcodeconcurrency/DesignPrintZeroEvenOdd/` | Semaphore coordination |

---

---

## Phase 5 — JVM Internals: Garbage Collector

Understanding GC separates Java developers from Java engineers. GC pauses, memory leaks, and heap tuning are common in senior interviews and production incidents.

| File | What it covers |
|---|---|
| `gc/GCRootsDemo.java` | Reachability tracing; GC roots (static fields, thread stacks); circular references; why ref-counting fails; object resurrection via finalize() |
| `gc/ReferenceTypesDemo.java` | Strong, Soft, Weak, Phantom references; `WeakHashMap`; `ReferenceQueue`; `Cleaner` API (modern `finalize()` replacement) |
| `gc/MemoryLeakPatternsDemo.java` | 5 classic leak patterns: static collection, unregistered listeners, ThreadLocal in thread pools, inner class holding outer, missing equals/hashCode |
| `gc/GCMonitoringDemo.java` | `GarbageCollectorMXBean`; `MemoryPoolMXBean`; GC notification listener; measuring GC overhead %; heap dump flags |

**Theory:** `gc/GC_Theory.md`

**GC Algorithms covered in theory:** Serial → Parallel → G1 (default) → ZGC → Shenandoah
**Key flags in theory:** `-Xms`, `-Xmx`, `-XX:+UseZGC`, `-Xlog:gc*`, `-XX:+HeapDumpOnOutOfMemoryError`

**Interview checkpoint:** Explain the difference between Minor GC and Full GC. What are the four GC root types? What is a memory leak in Java and how would you diagnose one?

---

## Quick Revision Map (Before an Interview)

**60-minute refresh:**
1. `Synchronization_Theory.md` (JMM, volatile, synchronized pitfalls)
2. `Locks_Theory.md` (ReentrantLock vs synchronized, AQS)
3. `Atomics_Theory.md` (CAS, ABA)
4. `Coordination_Theory.md` (CountDownLatch vs CyclicBarrier table)
5. Run `DeadlockDemo.java` mentally — explain the four conditions and two fixes

**30-minute refresh:**
- The four Theory.md files above (skip demos, read Q&A sections only)

**Day-of-interview:**
- Re-read all Q&A sections in all Theory.md files
