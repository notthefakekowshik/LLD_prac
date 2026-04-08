# Coordination Theory

High-level synchronization utilities for coordinating groups of threads. Use these before reaching for raw `wait/notify`.

---

## 1. CountDownLatch

A one-shot countdown. N threads count down; another thread waits for zero.

```java
CountDownLatch latch = new CountDownLatch(3); // Expect 3 signals

// Worker threads (any thread can countDown — not just the ones that await)
new Thread(() -> { doWork(); latch.countDown(); }).start();
new Thread(() -> { doWork(); latch.countDown(); }).start();
new Thread(() -> { doWork(); latch.countDown(); }).start();

// Coordinator waits:
latch.await();             // Blocks until count reaches 0
latch.await(5, SECONDS);   // With timeout — returns false if timed out
```

**Cannot be reset or reused.** Once the count reaches 0, all current and future `await()` calls return immediately.

**Classic patterns:**
- **Starting gun**: `CountDownLatch(1)` — one thread releases all workers simultaneously
  ```java
  CountDownLatch startGun = new CountDownLatch(1);
  for (int i = 0; i < 10; i++) {
      new Thread(() -> { startGun.await(); race(); }).start();
  }
  startGun.countDown(); // All 10 start at once
  ```
- **Service initialization wait**: main waits for N services to signal "ready"
- **Test synchronization**: ensure N threads start before asserting results

---

## 2. CyclicBarrier

A reusable rendezvous point. N threads must all arrive before any can proceed.

```java
int parties = 3;
CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
    // Optional barrier action — runs in the LAST arriving thread
    System.out.println("All parties arrived — stage complete!");
});

// All 3 threads must call await() before any proceeds
Thread t1 = new Thread(() -> { prepare(); barrier.await(); execute(); });
Thread t2 = new Thread(() -> { prepare(); barrier.await(); execute(); });
Thread t3 = new Thread(() -> { prepare(); barrier.await(); execute(); });
```

**Auto-resets after all parties arrive** — ready for the next round (hence "cyclic").

**BrokenBarrierException:** If one thread is interrupted or times out while waiting, the barrier is "broken". All other waiting threads immediately get `BrokenBarrierException`. The barrier is no longer usable — create a new one.

```java
try {
    barrier.await(5, SECONDS);
} catch (BrokenBarrierException e) {
    // Barrier was broken — someone else failed
} catch (TimeoutException e) {
    // This thread timed out — and just broke the barrier for everyone else
}
```

**Use case:** Multi-phase parallel computation (MapReduce, ETL pipelines). Each phase: all workers process their chunk → barrier → all move to next phase.

---

## 3. Semaphore

A counting semaphore. Limits the number of threads accessing a shared resource concurrently.

```java
Semaphore semaphore = new Semaphore(3); // Max 3 concurrent threads

// In each thread:
semaphore.acquire();       // Blocks if 0 permits available; decrements count
try {
    useResource();
} finally {
    semaphore.release();   // Increments count — wakes a waiting thread
}

// Non-blocking:
if (semaphore.tryAcquire()) { ... }
semaphore.tryAcquire(500, MILLISECONDS);

// Acquire multiple permits at once:
semaphore.acquire(3);
semaphore.release(3);
```

**Any thread can release** — unlike a lock, the releasing thread does NOT have to be the one that acquired. This makes Semaphore useful as a signaling mechanism.

**Semaphore(1):** Binary semaphore. Functionally similar to a mutex, but with the "any thread can release" property — which makes it dangerous as a lock (use `ReentrantLock` instead) but useful for producer/consumer signaling.

**Use cases:**
- Connection pool (limit N simultaneous DB connections)
- Rate limiting (N requests per window)
- Throttling parallel downloads

---

## 4. Phaser (Java 7)

The most flexible coordination primitive. Combines CountDownLatch + CyclicBarrier, with dynamic party registration.

```java
Phaser phaser = new Phaser(1); // Register "main" thread as 1 party

for (int i = 0; i < 3; i++) {
    phaser.register();          // Dynamically add party for this worker
    new Thread(() -> {
        doPhase1Work();
        phaser.arriveAndAwaitAdvance(); // Like barrier.await() for phase 0 → 1
        doPhase2Work();
        phaser.arriveAndDeregister();   // Done — remove from future phases
    }).start();
}

phaser.arriveAndDeregister(); // Deregister main thread
```

**Key methods:**
```java
phaser.register()                // Add 1 party
phaser.bulkRegister(n)           // Add n parties
phaser.arrive()                  // Signal arrival, DON'T wait
phaser.arriveAndAwaitAdvance()   // Signal + wait for all others (like barrier)
phaser.arriveAndDeregister()     // Signal + remove from future phases
phaser.awaitAdvance(phase)       // Wait for phase N to complete (without arriving)
```

**Termination:** Override `onAdvance()`:
```java
Phaser phaser = new Phaser(parties) {
    protected boolean onAdvance(int phase, int registeredParties) {
        return phase >= 3 || registeredParties == 0; // Terminate after 4 phases
    }
};
// phaser.isTerminated() → true when onAdvance returns true
```

**Tiered phasers:** Chain phasers for tree-structured parallelism:
```java
Phaser root = new Phaser();
Phaser child1 = new Phaser(root, 4);
Phaser child2 = new Phaser(root, 4);
// child1 and child2 report their phase completions up to root
```

---

## 5. Exchanger

A synchronization point where exactly **two** threads swap objects.

```java
Exchanger<DataBuffer> exchanger = new Exchanger<>();

// Thread 1 (producer):
DataBuffer full = fillBuffer();
DataBuffer empty = exchanger.exchange(full);  // Blocks until Thread 2 arrives
fillBuffer(empty);                             // Now has the emptied buffer

// Thread 2 (consumer):
DataBuffer empty = new DataBuffer();
DataBuffer full = exchanger.exchange(empty);   // Gets the full buffer
consume(full);
```

**Blocks until both threads call `exchange()`**, then both get the other's object.

**Use case:** Double-buffering pipeline — producer fills a buffer while consumer drains the previous one. They swap at each cycle with zero copying.

---

## 6. Decision Matrix

| Scenario | Best Tool |
|---|---|
| Wait for N services to initialize | `CountDownLatch` |
| Simultaneous start for N threads | `CountDownLatch(1)` as starting gun |
| Multi-phase batch computation | `CyclicBarrier` |
| Limit N concurrent accesses to a resource | `Semaphore` |
| Dynamic worker registration; varying phase counts | `Phaser` |
| Two threads swap data at a synchronization point | `Exchanger` |

### CountDownLatch vs CyclicBarrier

| | CountDownLatch | CyclicBarrier |
|---|---|---|
| Reusable? | No | Yes |
| All threads must wait? | No — `countDown()` and `await()` are separate | Yes — `await()` = countDown + wait |
| Barrier action? | No | Yes |
| Thread count changes? | No | No (use Phaser) |
| Broken state? | N/A | Yes — `BrokenBarrierException` |

---

## 7. Interview Q&A

**Q: When would you use `Phaser` over `CyclicBarrier`?**
A: When the number of participating threads changes between phases (dynamic registration/deregistration), or when you need termination logic (`onAdvance`), or when you need tiered synchronization trees. `CyclicBarrier` is simpler for fixed-party scenarios.

**Q: Can a non-waiting thread call `countDown()`?**
A: Yes — `countDown()` and `await()` are completely independent. Any thread can decrement the counter; only the thread that calls `await()` blocks.

**Q: What happens if one thread in a `CyclicBarrier` throws an exception or is interrupted?**
A: The barrier enters "broken" state. ALL threads currently waiting on `await()` get `BrokenBarrierException` immediately. Future `await()` calls also throw `BrokenBarrierException`. The only recovery is to create a new `CyclicBarrier`.

**Q: What is the difference between `Semaphore` and `Lock`?**
A: A `Lock` must be released by the same thread that acquired it. A `Semaphore` can be released by any thread — it's a signaling mechanism, not a mutual exclusion lock. `Semaphore(1)` ≠ mutex because any thread can release it.

**Q: How does `Exchanger.exchange()` work under the hood?**
A: The first arriving thread stores its value and parks. The second arriving thread reads the stored value, deposits its own value, and unparks the first thread. Both threads then return from `exchange()` with the other's value. It uses a slot-based, CAS-driven pairing algorithm.
