# Threads Theory

The foundation. Everything else in concurrency builds on a correct mental model of threads and memory.

---

## 1. Process vs Thread

| | Process | Thread |
|---|---|---|
| Memory | Isolated address space | Shares heap with sibling threads |
| Communication | IPC (pipes, sockets) — slow | Shared memory — fast but dangerous |
| Creation cost | High (fork, exec) | Low (JVM manages) |
| Fault isolation | Process crash is contained | Thread crash can kill the JVM |

### Java Memory Model (JMM) — Physical Layout

```
JVM Process
├── Heap (shared by ALL threads)
│   └── Objects, static fields, array elements
├── Thread 1 Stack (private)
│   └── Local vars, method params, call frames
├── Thread 2 Stack (private)
│   └── ...
└── CPU Registers / L1 Cache (per-core, private)
    └── Threads may cache variables here — source of visibility problems
```

The visibility problem: Thread A writes `x = 5` to its CPU cache. Thread B reads `x` from main memory and still sees `0`. This is why synchronization exists.

---

## 2. Four Ways to Create a Thread

```java
// 1. Extend Thread (avoid — ties up your inheritance slot)
class MyThread extends Thread {
    public void run() { System.out.println("Extending Thread"); }
}
new MyThread().start();

// 2. Implement Runnable (preferred for no-return tasks)
Runnable r = () -> System.out.println("Runnable lambda");
new Thread(r).start();

// 3. Callable — when you need a return value or checked exception
Callable<Integer> c = () -> 42;
Future<Integer> f = Executors.newSingleThreadExecutor().submit(c);
System.out.println(f.get()); // 42

// 4. Named daemon thread via builder (Java 19+)
Thread.ofVirtual().name("worker").start(() -> System.out.println("Virtual"));
```

**Why prefer Runnable?** Java has no multiple inheritance. `extends Thread` blocks you from extending anything else.
**When to use Callable?** When the task produces a result or throws a checked exception.

---

## 3. Thread Lifecycle — 6 States

```
NEW ──start()──> RUNNABLE ──CPU scheduled──> [running]
                    │                             │
              monitor lock              Object.wait() / Thread.join()
              contended                            │
                    │                             ▼
                 BLOCKED              WAITING / TIMED_WAITING
                    │                             │
              lock acquired            notify() / timeout / interrupt()
                    └──────────────────────────> RUNNABLE
                                                  │
                                          run() returns
                                                  ▼
                                            TERMINATED
```

| State | Cause | Exit Cause |
|---|---|---|
| NEW | `new Thread(...)` called | `start()` |
| RUNNABLE | `start()` called; may or may not be on CPU | Run completes, or blocks |
| BLOCKED | Waiting for `synchronized` lock | Lock acquired |
| WAITING | `Object.wait()`, `Thread.join()`, `LockSupport.park()` | `notify()`, join complete, `unpark()` |
| TIMED_WAITING | `Thread.sleep(ms)`, `wait(ms)`, `join(ms)` | Timeout or interrupt |
| TERMINATED | `run()` returned or exception escaped | — |

**Critical**: BLOCKED vs WAITING — BLOCKED means waiting for a **monitor lock** (synchronized). WAITING means waiting for a **signal** (notify/unpark). They look similar but are distinct states visible in thread dumps.

---

## 4. Key Thread Methods

```java
Thread t = new Thread(task);
t.setName("worker-1");
t.setDaemon(true);       // Must call BEFORE start()
t.setPriority(Thread.NORM_PRIORITY); // 1–10, OS may ignore it
t.start();

t.join();                // Current thread blocks until t finishes
t.join(5000);            // Wait at most 5 seconds
t.interrupt();           // Sets interrupt flag; sleeping/waiting threads get InterruptedException

// Inside the thread:
Thread.currentThread().isInterrupted();   // Check flag (does NOT clear it)
Thread.interrupted();                      // Check AND clear the flag
Thread.sleep(1000);      // TIMED_WAITING — ALWAYS restore interrupt status in catch:
try { Thread.sleep(1000); }
catch (InterruptedException e) { Thread.currentThread().interrupt(); }
```

---

## 5. ThreadLocal — Thread-Confined State

```java
// Each thread gets its own independent copy
ThreadLocal<SimpleDateFormat> formatter =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// Safe to use without synchronization:
formatter.get().format(new Date());

// ALWAYS remove when done (critical in thread pools — threads are reused)
formatter.remove();
```

**Memory Leak Pattern:** `ThreadLocal` keys are `WeakReference`. If the `ThreadLocal` object itself is GC'd, the key becomes null — but the **value** remains on the thread's `ThreadLocalMap` until the thread is GC'd or you call `remove()`. In thread pools, threads live forever, so values accumulate. **Always call `remove()` in a `finally` block.**

**InheritableThreadLocal:** Child threads inherit the parent thread's values at creation time. Changes after creation are NOT inherited.

---

## 6. Daemon Threads

```java
Thread bg = new Thread(() -> { while(true) doBackgroundWork(); });
bg.setDaemon(true); // JVM exits when ONLY daemon threads remain
bg.start();
```

Use for: background cleanup, GC (which is itself a daemon thread), heartbeat monitors.
Do NOT use for: tasks that must complete before JVM shutdown — use shutdown hooks instead.

---

## 7. Interview Q&A

**Q: What happens if you call `run()` instead of `start()`?**
A: `run()` executes synchronously on the CURRENT thread — no new thread is created. The `run()` method is just a regular method call.

**Q: Can you start a thread twice?**
A: No. `IllegalThreadStateException` is thrown. A terminated thread cannot be restarted.

**Q: What is the difference between `sleep()` and `wait()`?**
A: `sleep()` holds the monitor lock; the thread stays in TIMED_WAITING and nobody else can enter the synchronized block. `wait()` RELEASES the monitor lock and enters WAITING — other threads can acquire the lock.

**Q: Two threads call the same `synchronized` method on the same object — what happens?**
A: One gets the intrinsic lock and proceeds. The other enters BLOCKED state until the first releases the lock.

**Q: Why must `Object.wait()` be inside a `synchronized` block?**
A: `wait()` releases the monitor, so you must own it first. The JVM enforces this — calling `wait()` without the lock throws `IllegalMonitorStateException`.

**Q: What is a spurious wakeup?**
A: A thread in `wait()` can wake up without `notify()` being called — this is allowed by the JVM/OS spec. This is why you ALWAYS check the condition in a `while` loop, never `if`.
```java
while (!conditionMet()) {  // NOT: if (!conditionMet())
    lock.wait();
}
```
