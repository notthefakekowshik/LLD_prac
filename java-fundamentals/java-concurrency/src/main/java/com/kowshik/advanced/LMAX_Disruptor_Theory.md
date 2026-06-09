# LMAX Disruptor — Theory, Design, and Implementation

> **Goal:** understand *why* the Disruptor exists, what hardware problems it is designed around,
> and how the ring buffer design follows directly from those constraints.
> Implementation details make no sense without this foundation.

---

## 1. Origin — What Was LMAX Trying to Solve?

LMAX Exchange (2010–2011) was building a **retail financial trading platform** that needed to
process **6 million transactions per second** on a single thread, with deterministic
sub-millisecond latency. Every transaction was an order (buy/sell) that had to be:

1. Received from a network thread
2. Validated (business rules)
3. Matched against the order book
4. Sent to downstream consumers (journal, replication, market data)

The team — Martin Thompson, Mike Barker, and others — discovered through profiling that
**the queue between their threads was the bottleneck**. Not the business logic. The queue.

They wrote a paper in 2011 titled *"Disruptor: High Performance Alternative to Bounded
Queues for Exchanging Data Between Concurrent Threads"* that explained why conventional
queues cannot achieve this throughput, and what must be built instead.

The Disruptor is the answer to: *"what is the theoretically optimal data structure for
passing messages between threads on modern hardware?"*

---

## 2. Mechanical Sympathy — The Governing Philosophy

Martin Thompson coined the term **mechanical sympathy** (borrowed from racing driver
Jackie Stewart: "you don't have to be an engineer to be a racing driver, but you do have
to have mechanical sympathy").

Applied to software: **write code that works with the hardware, not against it.**

Most Java developers treat the CPU as a black box that executes instructions. The Disruptor
team treated it as a physical machine with specific characteristics that software must
respect:

- CPUs access memory in **cache lines**, not bytes
- CPUs **reorder instructions** for performance
- CPUs have a deep **cache hierarchy** that is orders of magnitude faster than RAM
- **Locks** force CPUs to do expensive coordination work they are otherwise avoiding

Every design decision in the Disruptor can be traced back to one of these hardware facts.

---

## 3. The CPU Cache — The Most Important Hardware Fact

### 3.1 Cache Hierarchy

Modern CPUs do not read from RAM directly. They read from a cache hierarchy:

```
CPU Core 0        CPU Core 1
┌─────────┐       ┌─────────┐
│  L1 (32KB, ~1ns)│  L1 (32KB, ~1ns)│
│  L2 (256KB,~4ns)│  L2 (256KB,~4ns)│
└────┬────┘       └────┬────┘
     │                 │
     └────┬────────────┘
     ┌────▼────────────────┐
     │  L3 (shared, ~10ns) │
     └────────┬────────────┘
              │
     ┌────────▼────────────┐
     │   RAM (~100ns)      │
     └─────────────────────┘
```

A memory access that hits **L1** takes ~1 ns. One that misses all the way to **RAM** takes
~100 ns — 100× slower. At 6M events/sec, every unnecessary RAM access costs you throughput.

### 3.2 Cache Lines — The Unit of Transfer

The CPU never loads a single byte. It loads a **cache line**: a contiguous 64-byte block
of memory. When you read `int x` at address 1000, the CPU loads bytes 960–1023 into the cache.

```
Cache line (64 bytes):
┌──────────────────────────────────────────────────────────────────┐
│  byte 0  │  byte 1  │ ... │ byte 31 │ ... │ byte 62 │  byte 63  │
└──────────────────────────────────────────────────────────────────┘
         ↑                                              ↑
    your int x                              something else entirely
```

**Why this matters:** if you access one field in a class, every other field in the same
64-byte region is loaded into cache for free. Accessing them next is free (L1 hit).
This is called **spatial locality** — it is why array traversal is faster than linked list
traversal.

A linked list node's `next` pointer points to a random memory address. Every node traversal
is potentially a cache miss → RAM access → 100 ns. An array's elements are contiguous →
the next element is already in the cache line loaded for the current element → 1 ns.

**First design consequence of the Disruptor:** use a contiguous array (ring buffer), not
a linked list. Every element access pre-loads the next elements for free.

### 3.3 The MESI Protocol — Why Sharing is Expensive

Each core has its own L1/L2 cache. When multiple cores access the same memory location,
their caches must stay consistent. This is enforced by the **MESI protocol**, where each
cache line is in one of four states:

| State | Meaning |
|-------|---------|
| **M** (Modified) | This core has the only up-to-date copy. RAM is stale. |
| **E** (Exclusive) | This core has the only copy. RAM matches. |
| **S** (Shared) | Multiple cores have a valid copy. |
| **I** (Invalid) | This core's copy is stale — must reload before use. |

**The critical rule:** when Core 0 **writes** to a cache line, every other core that has
that line is sent an invalidation message. Their copy moves from **S → I**.
The next time those cores read that address, they take a cache miss and reload from L3 or RAM.

```
Core 0 writes to variable X:
  → sends invalidation to all other cores holding the cache line
  → those cores must reload the cache line before their next read

Cost: ~40–100 ns for the round-trip invalidation + reload
```

At 6M events/sec, if your producer and consumer are sharing a cache line on every event,
that is 6M × 100 ns = 600 ms of cache invalidation overhead per second.
Your entire throughput budget is gone just from cache protocol traffic.

---

## 4. False Sharing — The Silent Throughput Killer

False sharing is what happens when **two threads write to different variables that happen
to be on the same cache line**.

```java
// Looks innocent. These two longs are 8 bytes each, placed adjacently in memory.
// Together they occupy one 64-byte cache line.
class Queue {
    long head;    // written by the consumer
    long tail;    // written by the producer
}
```

```
Cache line (64 bytes):
┌──────────────────┬───────────────────┬──────────────────────────┐
│  head (8 bytes)  │  tail (8 bytes)   │  ... (48 bytes padding)  │
└──────────────────┴───────────────────┴──────────────────────────┘
   consumer writes     producer writes
```

Even though `head` and `tail` are **logically independent**, they share a cache line.
Every time the producer writes `tail`, the consumer's L1 cache line (containing `head`)
is invalidated. Every time the consumer writes `head`, the producer's `tail` cache line
is invalidated.

The two threads are constantly invalidating each other's caches, even though they are
never actually contending for the same data. This is **false sharing** — the CPU wastes
coherence bandwidth on a non-existent conflict.

**This is measurable.** In benchmarks, false sharing on a hot cache line between two threads
can reduce throughput by 5–10×.

`ArrayBlockingQueue` has `head`, `tail`, and `count` as adjacent fields. Every operation
on the queue triggers this invalidation cycle.

**Design consequence:** every hot variable that is written by one thread must live on its
**own dedicated cache line**, padded so nothing else shares it.

---

## 5. Why Locks Are Expensive — The Full Picture

Most explanations stop at "locks cause context switches." The full story has three layers:

### Layer 1 — Memory Barriers

Every lock acquisition (`synchronized`, `ReentrantLock`) compiles down to a **memory fence**
instruction (e.g., `mfence` on x86, `dmb` on ARM). A memory fence does two things:

1. **Prevents reordering** — the CPU and compiler cannot move instructions past the fence.
2. **Flushes store buffers** — all pending writes are flushed to the cache coherence bus
   before proceeding.

This is what makes locks correct. It is also what makes them expensive: the store buffer
flush forces all pending writes to become visible to other cores — even writes to unrelated
variables — which means triggering cache line invalidations across the interconnect.

**You pay the coherence cost even when there is no contention.**

### Layer 2 — CAS Contention

`ReentrantLock` uses a `volatile int state` and CAS (`compareAndSet`) to acquire. Under
contention, multiple threads attempt CAS simultaneously. All but one fail and must retry
or park. Each failed CAS is a wasted cycle plus a cache coherence round-trip to read the
current state.

### Layer 3 — OS Scheduler Involvement

When a thread cannot acquire a lock, it calls `LockSupport.park()`, which invokes the OS
to deschedule the thread. When the lock is released, `LockSupport.unpark()` wakes it.
A single park/unpark round-trip takes **~1–10 µs** depending on OS scheduler granularity.

At 6M events/sec, each event has a budget of **167 ns** (1,000,000,000 ns / 6,000,000).
A single lock contention incident (park + unpark = 2 µs) consumes **12× the entire event budget**.

**Conclusion:** locks are not just slow. Under high load, they are categorically incompatible
with sub-microsecond latency targets because the OS scheduler operates at a granularity
that is coarser than your per-event budget.

---

## 6. Why Queues Are the Bottleneck (The LMAX Paper Insight)

The LMAX team's core observation was not about any specific queue implementation.
It was structural:

> Any queue that allows concurrent access must either use locks (expensive) or CAS
> (cheaper, but still requires a memory barrier and cache coherence round-trip).
> Under high throughput, the queue's coordination mechanism — not the business logic —
> becomes the bottleneck.

They measured this: with all business logic stripped out (just enqueue + dequeue), a
`LinkedBlockingQueue` throughput was ~5M ops/sec on their hardware. Their business logic
was only ~10% of the CPU budget — yet the queue was already near saturation.

The follow-on observation: **the queue also has a GC problem.**

`LinkedBlockingQueue` wraps each element in a `Node` object. At 6M events/sec:

```
6,000,000 events/sec × 1 Node object = 6M allocations/sec
Each Node: ~24–32 bytes → ~180 MB/sec of garbage
```

180 MB/sec of short-lived garbage puts continuous pressure on the young generation GC.
When a GC pause hits — even a 1ms "minor GC" — all 6M in-flight orders stall.
Deterministic latency is gone.

**The two problems to solve are therefore:**

1. Eliminate coordination cost (no locks, minimal memory barriers)
2. Eliminate allocation (no GC pressure at runtime)

The ring buffer solves both with a single design decision.

---

## 7. The Ring Buffer — Why This Shape Is the Answer

The ring buffer is a pre-allocated, fixed-size circular array. Every design decision
flows from the two constraints above.

### Constraint 1 — No allocation at runtime

Pre-allocate all event objects at startup. The ring buffer holds references to N objects,
created once. Producers do not create new objects — they write into the existing slot.

```
Startup:  [ OrderEvent@A | OrderEvent@B | OrderEvent@C | ... ]  ← allocated once
Runtime:  producer.setOrder(incomingOrder)  // mutates @A in place — zero allocation
```

GC has nothing to collect. Latency is deterministic.

### Constraint 2 — Minimize coordination cost

The key insight: **if we can guarantee that only one thread ever writes to a given slot at
a time, we need no lock on the slot itself.**

The ring buffer achieves this with **sequence numbers**:

- The producer owns a monotonically increasing sequence counter.
- Slot index = `sequence & (size - 1)` — every sequence maps to exactly one slot.
- The producer claims the next sequence, writes to that slot, then publishes the sequence.
- A consumer only reads a slot after the producer has published its sequence.
- The `volatile` write on publish / `volatile` read on consume provides the memory barrier.

There is no lock on any slot. The sequence number is both the index calculator and the
synchronization primitive. **One `volatile` write and one `volatile` read replace the entire
lock/unlock cycle.**

### Why circular?

A ring allows unbounded streaming with a bounded array. Slot `N` is reused when:
- The producer has moved N slots ahead, AND
- All consumers have consumed past that slot.

The consumer's sequence serves as a "safe to overwrite" marker. The producer checks the
slowest consumer's sequence before claiming a slot. No additional data structure needed.

### Why power-of-2 size?

`sequence % size` requires integer division — multiple CPU cycles.
`sequence & (size - 1)` requires bitwise AND — one CPU cycle.

At 100M ops/sec, this matters. More importantly, it is an example of the mechanical
sympathy philosophy applied at the arithmetic level.

---

## 8. Memory Ordering — What Makes This Safe

The ring buffer's lock-free safety rests entirely on the Java Memory Model.

### The guarantee needed

Producer writes data to slot N, then publishes sequence N.
Consumer must see all data written to slot N when it reads sequence N.

Without ordering guarantees, the CPU (or JIT compiler) could reorder the publish before
the data write — the consumer reads sequence N, reads the slot, and sees stale data.

### How volatile provides it

`volatile` on the sequence field inserts a **store-load memory barrier**:

```
// Producer
ringBuffer[N].data = value;         // ordinary write to slot
producerSequence.set(N);            // volatile write → store barrier here
                                    // all prior writes are flushed and visible

// Consumer
long available = producerSequence.get();  // volatile read → load barrier here
                                          // all subsequent reads see values
                                          // published before this sequence
Object data = ringBuffer[N].data;         // guaranteed to see value written above
```

The **happens-before** relationship (JMM §17.4.5): a volatile write to X happens-before
every subsequent volatile read of X. Everything the producer wrote *before* the volatile
write is visible to the consumer *after* the volatile read.

This is exactly one volatile write (publish) and one volatile read (waitFor) per event —
the minimum possible synchronization cost.

### How this compares to a lock

A `ReentrantLock` inserts **two** memory barriers (on acquire and on release), forces
a CAS, and potentially invokes the OS scheduler. The Disruptor's volatile pair inserts
**one** write barrier and **one** read barrier — no CAS, no OS.

---

## 9. Sequence Padding — Applying the Cache Line Theory

The `Sequence` object is read and written by every thread in the system on every event.
If producer sequence and consumer sequence share a cache line, every event triggers the
false sharing invalidation cycle described in Section 4.

The fix: pad every `Sequence` to occupy its own 64-byte cache line.

```java
// Left padding: 7 longs = 56 bytes
class LhsPadding { long p1, p2, p3, p4, p5, p6, p7; }

// The actual value: 1 long = 8 bytes
class Value extends LhsPadding { volatile long value; }

// Right padding: 7 longs = 56 bytes
class RhsPadding extends Value { long p9, p10, p11, p12, p13, p14, p15; }

public final class Sequence extends RhsPadding { ... }
// Total: 56 + 8 + 56 = 120 bytes
// The volatile long value is surrounded on both sides — guaranteed its own cache line.
```

This is the mechanical sympathy philosophy applied to memory layout:
*"know that the CPU loads 64 bytes at a time, and design your fields accordingly."*

---

## 10. Wait Strategies — Trading CPU for Latency

The Disruptor's final degree of freedom is what the consumer does when no events are available.
This is purely a hardware trade-off:

```
More CPU burned → lower latency (consumer detects new events sooner)
Less CPU burned → higher latency (consumer checks less frequently)
```

| Strategy | Mechanism | CPU | Latency | Use case |
|----------|-----------|-----|---------|----------|
| `BusySpinWaitStrategy` | Tight `while` loop | 100% of one core | Sub-µs | Dedicated core, HFT matching engine |
| `YieldingWaitStrategy` | 100 spins, then `Thread.yield()` | High | ~1 µs | Low-latency, core sharing acceptable |
| `SleepingWaitStrategy` | Spin → yield → `parkNanos(1)` | Low | ~10 µs | Good default, balanced |
| `BlockingWaitStrategy` | `Lock` + `Condition.await()` | Minimal | ~100 µs | Throughput not critical |
| `PhasedBackoffWaitStrategy` | Configurable spin + yield + fallback | Tunable | Tunable | Fine-grained production tuning |

`BusySpinWaitStrategy` works because the spinning thread **keeps the producer sequence
in its L1 cache**. It detects the volatile write within nanoseconds of the producer
publishing. `BlockingWaitStrategy` yields the core, allowing the OS to schedule other
work — but when a new event arrives, the consumer must be woken via `unpark()`, which
takes microseconds.

---

## 11. The Complete Mental Model

Every Disruptor design decision maps to one of three hardware constraints:

| Hardware Constraint | Problem It Causes | Disruptor Solution |
|--------------------|------------------|--------------------|
| Cache lines are 64 bytes | False sharing between producer/consumer sequences | Pad every `Sequence` to its own cache line |
| RAM access is 100× slower than L1 | Linked list traversal = cache miss per node | Pre-allocated contiguous array (ring buffer) |
| GC pauses break latency determinism | LinkedBlockingQueue = 180 MB/s of garbage at 6M/s | Pre-allocate event objects; mutate in place |
| Locks require OS scheduler (µs granularity) | Lock contention exceeds per-event budget | Coordinate via `volatile` sequence numbers only |
| Memory barriers flush the store buffer | Every lock acquire/release has barrier cost | One volatile write + one volatile read per event |
| Power-of-2 enables bitwise AND | Modulo for slot index costs division cycles | Ring buffer size constrained to power of 2 |

---

## 12. Where BlockingQueue Remains the Right Choice

The Disruptor has real costs:

- Complexity: ring buffer, sequence barriers, wait strategies, event factories — significant
  conceptual overhead vs. `new LinkedBlockingQueue<>()`.
- Pre-allocation: events must be fixed-size and mutable. Variable-size payloads (large JSON,
  file chunks) do not fit the flyweight model cleanly.
- Dedicated cores: `BusySpinWaitStrategy` burns 100% of a core. On a shared JVM or cloud
  instance, this is a non-starter.

**Use BlockingQueue when** throughput is below ~1M events/sec, or latency requirements are
in the millisecond range, or you cannot dedicate CPU cores to spinning consumers.

**Use Disruptor when** you have measured that the queue is the bottleneck, you need
sub-microsecond latency, you have a predictable event structure, and you can afford
dedicated cores.

---

## 13. Connecting Back to the Order Book

The Order Book in this repo uses `SingleThreadExecutor` per symbol, which internally holds
a `LinkedBlockingQueue<Runnable>`. Under normal interview load this is perfectly correct.

Under LMAX-scale load (millions of orders/sec per symbol), the `LinkedBlockingQueue` hits
all three problems: lock contention, GC pressure from `Runnable` wrappers, false sharing
on queue internals.

The theoretical progression is:

```
SingleThreadExecutor (LinkedBlockingQueue + lock)
  → ArrayBlockingQueue (lock, no node GC)
    → Disruptor (no lock, no GC, no false sharing)
```

Each step eliminates one hardware-level bottleneck. The Disruptor is the end of this
progression: there is no further hardware optimization available without going below the JVM
(CPU affinity, DPDK networking, kernel bypass).

---

## 14. Implementation Reference

For the code-level details (ring buffer API, EventHandler setup, producer publish, pipeline
wiring), see the implementation sections in this same file below — but read them with the
theoretical model in Sections 1–11 in mind. Every API choice has a reason rooted in
Sections 3–8.

```java
// The theory in three lines of code:
long sequence = ringBuffer.next();           // claim a slot — no lock, just sequence increment
ringBuffer.get(sequence).setOrder(order);    // write into pre-allocated object — no GC
ringBuffer.publish(sequence);               // volatile write — one memory barrier, consumer wakes
```

---

## 15. Key Takeaways

1. The Disruptor exists because **locks + linked queues are incompatible with sub-microsecond
   latency at millions of events/sec** — not because they are poorly implemented, but because
   they involve the OS scheduler and GC, which operate at coarser granularities than the
   per-event budget.

2. **Mechanical sympathy** is the design principle: understand cache lines, MESI protocol,
   memory barriers, and the OS scheduler, then build data structures that work with these
   constraints rather than against them.

3. The ring buffer is the theoretically optimal structure because it simultaneously solves
   cache locality (contiguous array), GC pressure (pre-allocation), and coordination cost
   (sequence numbers as the sole synchronization primitive).

4. **False sharing** is a real, measurable phenomenon. Padding a `Sequence` to its own
   cache line is not a micro-optimization superstition — it is a direct response to
   the MESI protocol's invalidation behaviour.

5. The `volatile` sequence pair provides the minimum-cost synchronization possible under
   the JMM — one store barrier and one load barrier, vs. two barriers + CAS + potential
   OS park/unpark for a lock.

6. **Know when not to use it.** The Disruptor is the right answer to a specific, measurable
   problem. `BlockingQueue` is correct for the other 95% of use cases.
