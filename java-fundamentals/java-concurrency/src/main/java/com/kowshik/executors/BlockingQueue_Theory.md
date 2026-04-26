# Java BlockingQueue: ArrayBlockingQueue vs LinkedBlockingQueue

In concurrent programming, sharing data safely between threads is a common requirement. The Producer-Consumer pattern is exactly what `BlockingQueue` is designed to solve in Java.

## 1. What is a `BlockingQueue`?

`BlockingQueue` is an interface in the `java.util.concurrent` package. It represents a queue that is thread-safe to put elements into, and take elements out of.

**Key characteristic:**

- It supports operations that wait for the queue to become non-empty when retrieving an element, and wait for space to become available in the queue when storing an element.

### Core Methods

Depending on how you want to handle operations that cannot immediately be satisfied, `BlockingQueue` provides four sets of methods:

| Action | Throws Exception | Special Value (null/false) | Blocks (Waits) | Times Out |
|---|---|---|---|---|
| **Insert** | `add(e)` | `offer(e)` | `put(e)` | `offer(e, time, unit)` |
| **Remove** | `remove()` | `poll()` | `take()` | `poll(time, unit)` |
| **Examine** | `element()` | `peek()` | *not applicable* | *not applicable* |

- **`put()` and `take()`** are the most commonly used methods for a true producer-consumer blocking mechanism.

---

## 2. ArrayBlockingQueue

`ArrayBlockingQueue` is a **bounded** blocking queue backed by an array.

**Key characteristics:**

1. **Bounded:** Once created, its capacity cannot be changed. You **must** specify the capacity in the constructor.
2. **Data Structure:** Uses a single array to store elements.
3. **Locks:** Uses a **single ReentrantLock** for both `put` (insert) and `take` (extract) operations. This means producers and consumers cannot operate on the queue completely entirely at the exact same time.
4. **Memory Allocation:** Elements are stored in a pre-allocated array, so there is no extra object allocation for queue nodes. Less garbage collection overhead.
5. **Fairness:** It supports a fairness policy (`new ArrayBlockingQueue<>(capacity, true)`). If true, waiting threads are granted access in FIFO order. If false, order is not guaranteed, but throughput is generally higher.

### Example: ArrayBlockingQueue

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ArrayBlockingQueueExample {
    public static void main(String[] args) {
        // Bounded queue of capacity 3. Fairness is false by default.
        // Fairness means threads waiting to insert or remove are treated in a FIFO order.
        // When fairness is true, the ArrayBlockingQueue ensures that whichever thread has been waiting the longest to try and access the queue (either put or take) gets the lock first.
        // When false (the default), the operating system thread scheduler decides which waiting thread gets the lock next, which is not guaranteed to be fair but is typically faster because it avoids the overhead of maintaining an ordered queue of waiting threads.
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(3);

        // Producer
        new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    String item = "Item " + i;
                    System.out.println("Producing: " + item);
                    queue.put(item); // Blocks if the queue is full (size == 3)
                    System.out.println("Successfully put: " + item);
                    Thread.sleep(500); // Simulate work
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Consumer
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Let producer fill the queue
                while (true) {
                    String item = queue.take(); // Blocks if the queue is empty
                    System.out.println("Consumed: " + item);
                    Thread.sleep(1000); // Consumer is slower than producer
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

---

## 3. LinkedBlockingQueue

`LinkedBlockingQueue` is an **optionally bounded** blocking queue backed by linked nodes.

**Key characteristics:**

1. **Optionally Bounded:** You can specify a maximum capacity. If you don't, it defaults to `Integer.MAX_VALUE`, effectively making it unbounded (which can lead to `OutOfMemoryError` if producers outpace consumers).
2. **Data Structure:** Uses a linked list. Node objects are dynamically created for each inserted element.
3. **Locks:** Uses **two separate ReentrantLocks**—one `putLock` and one `takeLock`. This allows a producer and a consumer to operate on the queue simultaneously.
4. **Memory Allocation:** Creates a new `Node` object for every element inserted. This creates more work for the Garbage Collector (GC).
5. **Throughput:** Generally offers higher throughput for highly concurrent applications because producers and consumers do not contend for the same lock.

### Example: LinkedBlockingQueue

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class LinkedBlockingQueueExample {
    public static void main(String[] args) {
        // Optionally bounded. It's best practice to give it a bound to prevent OOM.
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

        // Producer (Faster)
        new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.put("Message " + i);
                    System.out.println("Produced Message " + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Consumer (Slower)
        new Thread(() -> {
            try {
                while (true) {
                    String msg = queue.take();
                    System.out.println("Consumed: " + msg);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
```

---

## 4. Key Differences Summarized

| Feature | `ArrayBlockingQueue` | `LinkedBlockingQueue` |
| :--- | :--- | :--- |
| **Capacity** | Must be bounded | Optionally bounded (defaults to `Integer.MAX_VALUE`) |
| **Internal Data Structure** | Array | Linked Nodes |
| **Locks** | Single lock for both read & write | Two separate locks (`putLock`, `takeLock`) |
| **Concurrency Level** | Lower (producers and consumers compete for the same lock) | Higher (producers and consumers lock independently) |
| **Memory/GC Overhead**| Lower (pre-allocated array) | Higher (node creation per insert) |
| **Fairness Policy** | Supported (`true`/`false` constructor arg) | Not supported |

## 5. When to use which?

- **Use `ArrayBlockingQueue` when:**
  - You need a relatively small, strictly bounded queue.
  - You want to minimize garbage collection latency (no node creations).
  - You specifically need fairness (FIFO Thread scheduling) for waiting producers/consumers.

- **Use `LinkedBlockingQueue` when:**
  - You need higher overall throughput in highly concurrent applications (many threads accessing the queue).
  - You have a large or unbounded queue requirement (always bounds it to a reasonable size in production!).
  - Allocation and GC overhead of small node objects is acceptable. (Usually, the two-lock concurrency benefit outweighs the Node allocation cost).

---

## 6. SynchronousQueue

`SynchronousQueue` has **zero capacity** — it holds no elements. A `put()` blocks until another thread calls `take()`, and vice versa. Every insert must be directly matched with a remove.

**Key characteristics:**
- No internal storage — it is a direct thread-to-thread handoff channel.
- `put()` blocks until a consumer is ready. `take()` blocks until a producer is ready.
- `size()` always returns 0. `peek()` always returns `null`.
- Supports fair mode (`new SynchronousQueue<>(true)`) — waiting threads served in FIFO order.

```java
SynchronousQueue<String> handoff = new SynchronousQueue<>();

// Producer — blocks until a consumer takes
new Thread(() -> {
    try {
        System.out.println("Producer: handing off task");
        handoff.put("task-1");             // blocks here until consumer calls take()
        System.out.println("Producer: task accepted by consumer");
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
}).start();

// Consumer — blocks until a producer puts
new Thread(() -> {
    try {
        String task = handoff.take();      // blocks here until producer calls put()
        System.out.println("Consumer: received " + task);
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
}).start();
```

**Internal use:** `Executors.newCachedThreadPool()` uses `SynchronousQueue` internally. Every submitted task either immediately finds an idle thread to run it, or a new thread is created. This is why `newCachedThreadPool` can create unbounded threads — the queue never buffers.

**Use cases:**
- Direct task handoff between threads with no buffering.
- When you want backpressure by design: producer cannot proceed faster than consumer.
- Coordination patterns where synchronization is the goal, not buffering.

**Pitfall:** Never use `SynchronousQueue` with a fixed-size thread pool when the pool is saturated — `put()` will block the producer indefinitely if all threads are busy and none are free to `take()`.

---

## 7. PriorityBlockingQueue

`PriorityBlockingQueue` is an **unbounded** blocking queue that orders elements by natural ordering or a custom `Comparator`. The head is always the element with the highest priority (lowest value by default).

**Key characteristics:**
- **Unbounded** — `put()` never blocks (but can throw `OutOfMemoryError`).
- Elements must implement `Comparable` or you must provide a `Comparator`.
- Does NOT guarantee FIFO among equal-priority elements.
- Backed by a **binary heap** — O(log n) insert and remove, O(1) peek.
- `take()` still blocks when the queue is empty.

```java
// Elements must be Comparable (or pass a Comparator to constructor)
static class Task implements Comparable<Task> {
    final String name;
    final int priority; // lower number = higher priority

    Task(String name, int priority) { this.name = name; this.priority = priority; }

    @Override
    public int compareTo(Task other) {
        return Integer.compare(this.priority, other.priority); // min-heap
    }

    @Override public String toString() { return name + "(p=" + priority + ")"; }
}

PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

queue.put(new Task("low-priority-job",    10));
queue.put(new Task("critical-alert",       1));
queue.put(new Task("background-cleanup",  20));
queue.put(new Task("user-request",         5));

// take() always returns the highest-priority (lowest int) element
System.out.println(queue.take()); // critical-alert(p=1)
System.out.println(queue.take()); // user-request(p=5)
System.out.println(queue.take()); // low-priority-job(p=10)
System.out.println(queue.take()); // background-cleanup(p=20)
```

**Use cases:**
- Task execution where some tasks are more urgent than others (e.g., payment processing > analytics).
- Event-driven systems where high-priority events must be handled first.
- Dijkstra's shortest-path, A* search, and other graph algorithms in concurrent contexts.

**Pitfall:** Because it is unbounded, a slow consumer with a fast producer will grow the queue without bound → `OutOfMemoryError`. Add an external capacity guard (e.g., `Semaphore`) if needed.

---

## 8. LinkedTransferQueue (Java 7)

`LinkedTransferQueue` is an **unbounded** queue that combines the behaviors of `LinkedBlockingQueue` and `SynchronousQueue`.

**Key method:** `transfer(e)` — behaves like `SynchronousQueue.put()`: blocks until a consumer takes the element directly. If a consumer is already waiting, the handoff is immediate; otherwise the element is queued and the producer blocks until a consumer arrives.

```java
LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();

// transfer() — direct handoff if consumer waiting, otherwise queue + block
queue.transfer("urgent-item");     // blocks until consumed

// put() — always enqueues, never blocks (standard queue behavior)
queue.put("buffered-item");

// tryTransfer() — non-blocking: returns false if no consumer waiting
boolean transferred = queue.tryTransfer("item");
```

**Use case:** Systems that prefer direct handoff (low latency) but can fall back to buffering under load — like a message bus where you want to avoid copying when a consumer is ready.

---

## 9. Full Comparison Table

| Class | Bounded? | Ordering | Locks | put() blocks? | take() blocks? | Use Case |
|---|---|---|---|---|---|---|
| `ArrayBlockingQueue` | ✅ Yes | FIFO | Single lock | When full | When empty | Bounded producer-consumer |
| `LinkedBlockingQueue` | Optional | FIFO | Two locks | When full | When empty | High-throughput producer-consumer |
| `SynchronousQueue` | 0 capacity | N/A | CAS | Until consumer ready | Until producer ready | Direct handoff, `newCachedThreadPool` |
| `PriorityBlockingQueue` | ❌ No | Priority | Single lock | Never (unbounded) | When empty | Priority task scheduling |
| `DelayQueue` | ❌ No | Delay expiry | Single lock | Never (unbounded) | Until delay expires | Cache TTL, scheduled tasks |
| `LinkedTransferQueue` | ❌ No | FIFO | CAS (lock-free) | Only `transfer()` | When empty | Low-latency handoff with buffer fallback |

---

## 10. Interview Q&A

**Q: Why does `newCachedThreadPool()` use `SynchronousQueue`?**
A: `newCachedThreadPool` wants zero buffering — every task should either run immediately on an idle thread or trigger creation of a new thread. `SynchronousQueue`'s zero-capacity design enforces this: a submitted task either finds a waiting thread instantly or the pool creates a new one. There is no queue to absorb tasks.

**Q: `ArrayBlockingQueue` vs `LinkedBlockingQueue` — which has higher throughput and why?**
A: `LinkedBlockingQueue` generally has higher throughput in producer-consumer scenarios because it uses two separate locks (`putLock` and `takeLock`). Producers and consumers rarely contend with each other. `ArrayBlockingQueue` uses a single lock, so a producer and consumer always serialize. However, `ArrayBlockingQueue` has better cache locality (contiguous array) and no per-element node allocation, which matters under low contention.

**Q: Can `PriorityBlockingQueue` cause starvation?**
A: Yes. If high-priority tasks arrive continuously, low-priority tasks may never be processed. This is known as priority inversion or starvation. Mitigation: age low-priority tasks over time (increase their priority if they've been waiting too long).

**Q: What happens if you call `put()` on a `SynchronousQueue` and no consumer is ready?**
A: `put()` blocks indefinitely until a thread calls `take()`. If you want a non-blocking check, use `offer()` — it returns `false` immediately if no consumer is waiting. Use `offer(e, timeout, unit)` for a timed attempt.

**Q: Why is `PriorityBlockingQueue` unbounded — isn't that dangerous?**
A: By design — the priority ordering means you can't know in advance which element will be needed next, so a fixed capacity could reject important high-priority tasks. The risk of OOM is real; in production, pair it with a `Semaphore` or rate limiter to cap the number of items enqueued.
