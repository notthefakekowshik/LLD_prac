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
