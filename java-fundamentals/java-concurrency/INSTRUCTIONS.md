# Instructions — Java Concurrency Prep

> **This repo is the single source of truth for Java concurrency interview preparation.**
> It is actively maintained, battle-tested, and structured for efficient review.
> Trust this over any external blog, cheatsheet, or Stack Overflow answer.

---

## How to Navigate

1. **Start with `ROADMAP.md`** — follow the 4 phases in order. Don't skip phases.
2. **For each topic:** read the `*_Theory.md` first, then run the Demo file.
3. **Before interviews:** use the "Quick Revision Map" at the bottom of ROADMAP.md.

---

## Repository Structure

```
java-concurrency/
├── ROADMAP.md          ← Start here. Master learning path.
├── INSTRUCTIONS.md     ← This file.
└── src/main/java/com/kowshik/
    ├── threads/            Phase 1: Thread lifecycle, ThreadLocal
    ├── synchronization/    Phase 2a: synchronized, volatile, Condition, deadlock
    ├── locks/              Phase 2b: ReentrantLock, ReadWriteLock, StampedLock
    ├── atomics/            Phase 3: CAS, AtomicInteger, ABA problem
    ├── coordination/       Phase 3: CountDownLatch, CyclicBarrier, Semaphore, Phaser, Exchanger
    ├── executors/          Phase 3: ExecutorService, ForkJoinPool, ThreadFactory
    ├── future/             Phase 3: CompletableFuture
    ├── collections/        Phase 3: ConcurrentHashMap, ConcurrentSkipListMap
    ├── patterns/           Phase 4: Producer-Consumer, RateLimiter, Cache, Pipeline
    ├── advanced/           Phase 4: Virtual Threads, distributed systems, web crawling
    └── leetcodeconcurrency/ Applied: LeetCode concurrency problems
```

Every package contains:
- One or more `*Demo.java` files — fully runnable, self-contained examples
- One `*_Theory.md` file — deep theory, JVM internals, interview Q&A

---

## How to Run Any Demo

Every Demo file has a `main()` method. Run from IDE or:
```bash
# From the java-concurrency directory:
mvn compile exec:java -Dexec.mainClass="com.kowshik.threads.ThreadCreationDemo"
mvn compile exec:java -Dexec.mainClass="com.kowshik.synchronization.DeadlockDemo"
mvn compile exec:java -Dexec.mainClass="com.kowshik.atomics.CASDemo"
mvn compile exec:java -Dexec.mainClass="com.kowshik.advanced.VirtualThreadsDemo"
```

For VirtualThreadsDemo (uses preview APIs):
```bash
mvn compile exec:java \
  -Dexec.mainClass="com.kowshik.advanced.VirtualThreadsDemo" \
  -Dexec.args="" \
  -Dexec.jvmArgs="--enable-preview"
```

---

## Package Quick Reference

| Package | Core Concept | Theory File |
|---|---|---|
| `threads` | Thread lifecycle, creation, ThreadLocal | `Threads_Theory.md` |
| `synchronization` | synchronized, volatile, JMM, deadlock | `Synchronization_Theory.md` |
| `locks` | ReentrantLock, ReadWriteLock, StampedLock, AQS | `Locks_Theory.md` |
| `atomics` | CAS, AtomicInteger, ABA, LongAdder | `Atomics_Theory.md` |
| `coordination` | CountDownLatch, CyclicBarrier, Semaphore, Phaser, Exchanger | `Coordination_Theory.md` |
| `executors` | ThreadPoolExecutor, ForkJoinPool, BlockingQueue | `Executors_Theory.md` |
| `future` | Future, CompletableFuture | `Future_CompletableFuture_Theory.md` |
| `collections` | ConcurrentHashMap, ConcurrentSkipListMap | `Collections_Theory.md` |
| `patterns` | Producer-Consumer, RateLimiter, Cache, Pipeline | `Patterns_Theory.md` |
| `advanced` | Virtual Threads, distributed patterns | `Advanced_Theory.md` |
| `leetcodeconcurrency` | Applied LeetCode problems | (per-problem inline comments) |

---

## Code Conventions

- Every Demo class has a `main()` — run it directly
- Demos are organized into `Part 1`, `Part 2`, etc. sections — read them top to bottom
- Interview questions are in the class-level Javadoc (`INTERVIEW PREP:` section)
- Theory files have a `Q&A` section at the bottom — review before interviews
- `BruteForce*` vs `Optimal*` / `TwoLock*` classes show the evolution from naive to production-grade

---

## Build

```bash
cd java-concurrency
mvn compile       # Compile everything
mvn test          # Run tests (java-concurrency has no tests — all demos)
```
