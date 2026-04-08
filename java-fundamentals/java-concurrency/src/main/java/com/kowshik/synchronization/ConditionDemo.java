package com.kowshik.synchronization;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Condition Demo - Inter-thread Communication Tutorial
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. What is a Condition in Java Concurrency?
 *    - Factors out the Object monitor methods (wait, notify, notifyAll) into distinct objects.
 *    - Used in conjunction with a Lock (typically ReentrantLock).
 *    - Allows a thread to suspend execution ('await') until a specific condition is met.
 *
 * 2. Why use Condition over Object.wait()/notify()?
 *    - Multiple Wait Sets: A single Lock can have multiple Condition objects.
 *      (e.g., 'notFull' and 'notEmpty' for a bounded buffer).
 *    - Object.wait() forces all threads to wait on the same monitor.
 *    - Condition.signal() allows waking up specific threads waiting on a specific condition,
 *      reducing contention and context switching (thundering herd problem).
 *    - Better readability and control.
 *
 * 3. Key Methods:
 *    - await(): Causes the current thread to wait until signalled or interrupted. Releases the lock.
 *    - signal(): Wakes up one waiting thread.
 *    - signalAll(): Wakes up all waiting threads.
 *
 * 4. Common Interview Questions:
 *    - How to implement a Producer-Consumer problem using Locks and Conditions?
 *    - Difference between signal() and signalAll()?
 *    - What happens to the lock when await() is called? (It is released atomically).
 *    - Why does await() need to be called inside a loop? (To handle spurious wakeups).
 *
 * 5. Best Practices:
 *    - Always check the condition in a while loop (while(!condition) await()).
 *    - Always wrap the critical section in try-finally to ensure the lock is released.
 *    - Use meaningful names for Condition objects (e.g., notFull, notEmpty).
 */
public class ConditionDemo {

    // Shared resource acting as a bounded buffer
    private static class BoundedBuffer {
        private final Queue<Integer> queue = new LinkedList<>();
        private final int capacity;

        // Lock to protect shared state
        private final Lock lock = new ReentrantLock();

        // Condition: Queue is not full (Producers wait on this)
        private final Condition notFull = lock.newCondition();

        // Condition: Queue is not empty (Consumers wait on this)
        private final Condition notEmpty = lock.newCondition();

        public BoundedBuffer(int capacity) {
            this.capacity = capacity;
        }

        public void produce(int item) throws InterruptedException {
            lock.lock();
            try {
                // 1. Wait while the buffer is full
                // Standard idiom: wait loop to handle spurious wakeups
                while (queue.size() == capacity) {
                    System.out.println("Queue is FULL. Producer waiting...");
                    notFull.await(); // Releases lock and waits
                }

                // 2. Perform the action
                queue.add(item);
                System.out.println("Produced: " + item + " | Queue size: " + queue.size());

                // 3. Signal waiting consumers
                // Signal 'notEmpty' because we just added an item
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }

        public int consume() throws InterruptedException {
            lock.lock();
            try {
                // 1. Wait while the buffer is empty
                while (queue.isEmpty()) {
                    System.out.println("Queue is EMPTY. Consumer waiting...");
                    notEmpty.await(); // Releases lock and waits
                }

                // 2. Perform the action
                int item = queue.poll();
                System.out.println("Consumed: " + item + " | Queue size: " + queue.size());

                // 3. Signal waiting producers
                // Signal 'notFull' because we just removed an item
                notFull.signal();

                return item;
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        // Create a buffer with capacity 5
        BoundedBuffer buffer = new BoundedBuffer(5);

        // Create Producer Thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.produce(i);
                    Thread.sleep(200); // Simulate work
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Create Consumer Thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.consume();
                    Thread.sleep(500); // Consume slower to force full buffer
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
            System.out.println("Demo Completed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class ConditionDemoWithExecutor {
    public static void main(String[] args) throws InterruptedException {
        BetterBlockingQueue<Integer> betterBlockingQueue = new BetterBlockingQueue<>(5);

        try (ExecutorService producers = Executors.newFixedThreadPool(10)) {
            producers.submit(() -> {
                for (int i = 0; i < 200; i++) {
                    try {
                        betterBlockingQueue.put(i);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            producers.submit(() -> {
                for (int i = 0; i < 10; i++) {
                    try {
                        betterBlockingQueue.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }
}

class BetterBlockingQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;

    // 1. The Lock (replacing 'synchronized')
    private final Lock lock = new ReentrantLock();

    // 2. The Conditions (The separate waiting rooms)
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public BetterBlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        lock.lock(); // Explicitly acquire the lock
        try {
            // If full, go to the "notFull" waiting room
            while (queue.size() == capacity) {
                System.out.println("!!!! QUEUE IS FULL !!!!!");
                notFull.await();
            }

            queue.add(item);
            System.out.println("PRODUCED : " + item);
            // KEY MOMENT: We ONLY wake up consumers!
            // We signal the 'notEmpty' room.
            // Producers stuck in 'notFull' stay asleep (no contention).
            notEmpty.signal();

        } finally {
            lock.unlock(); // Always unlock in finally
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            // If empty, go to the "notEmpty" waiting room
            while (queue.isEmpty()) {
                System.out.println("!!!! QUEUE IS EMPTY !!!!!");
                notEmpty.await();
            }

            T item = queue.remove();
            System.out.println("Consumed : " + item);
            // KEY MOMENT: We ONLY wake up producers!
            // We signal the 'notFull' room.
            notFull.signal();

            return item;
        } finally {
            lock.unlock();
        }
    }
}