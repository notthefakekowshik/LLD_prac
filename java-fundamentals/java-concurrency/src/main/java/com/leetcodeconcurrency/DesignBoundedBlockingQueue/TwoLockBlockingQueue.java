package com.leetcodeconcurrency.DesignBoundedBlockingQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TwoLockBlockingQueue {

    /**
     * Internal Node class for the Linked List.
     * We use a custom node to avoid the overhead of java.util.LinkedList.
     */
    private static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
            this.next = null;
        }
    }

    // Capacity and Size
    private final int capacity;
    // AtomicInteger is required because size is updated by both producer and consumer threads simultaneously.
    private final AtomicInteger current_size = new AtomicInteger(0);

    // Pointers for the Head and Tail of the queue
    // Head -> Removing items (Consumers)
    // Tail -> Adding items (Producers)
    private Node head;
    private Node tail;

    // --- LOCK 1: For Consumers (takes) ---
    private final Lock takeLock = new ReentrantLock();
    // Condition: Wait here if the queue is empty
    private final Condition isEmpty = takeLock.newCondition();

    // --- LOCK 2: For Producers (puts) ---
    private final Lock putLock = new ReentrantLock();
    // Condition: Wait here if the queue is full
    private final Condition isFull = putLock.newCondition();

    public TwoLockBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;

        // Initialize with a Dummy Node.
        // head and tail both point to this placeholder.
        // This ensures head and tail are never null, simplifying locking logic.
        head = new Node(0);
        tail = head;
    }

    // --------------------------------------------------------------------
    // PRODUCER LOGIC
    // --------------------------------------------------------------------
    public void enqueue(int element) throws InterruptedException {
        Node node = new Node(element);
        int c = -1; // Holds the size BEFORE we insert

        putLock.lock(); // Only lock the "Tail" end
        try {
            // Wait while queue is full
            while (current_size.get() == capacity) {
                isFull.await();
            }

            // Standard Linked List append to Tail
            tail.next = node;
            tail = node;

            // Atomically increment size.
            // getAndIncrement returns the OLD value (c) before adding 1.
            c = current_size.getAndIncrement();

            // CASCADING SIGNAL OPTIMIZATION:
            // If we just added an item, and there is STILL space left (c + 1 < capacity),
            // we wake up another producer immediately.
            // This prevents the "Thundering Herd" problem and speeds up filling the queue.
            if (c + 1 < capacity) {
                isFull.signal();
            }

        } finally {
            putLock.unlock();
        }

        // SIGNAL CONSUMER:
        // If c was 0, it means the queue WAS empty before this insertion.
        // Now it has 1 item, so we must wake up a waiting consumer.
        if (c == 0) {
            signalNotEmpty();
        }
    }

    // --------------------------------------------------------------------
    // CONSUMER LOGIC
    // --------------------------------------------------------------------
    public int dequeue() throws InterruptedException {
        int x;
        int c = -1; // Holds the size BEFORE we remove

        takeLock.lock(); // Only lock the "Head" end
        try {
            // Wait while queue is empty
            while (current_size.get() == 0) {
                isEmpty.await();
            }

            // Remove from Head.
            // Remember: 'head' points to a Dummy Node.
            // The actual first data item is head.next.
            Node first = head.next;

            // The old dummy becomes garbage.
            // The node we just retrieved ('first') becomes the NEW dummy node.
            // This is a standard trick in concurrent queues to separate head/tail access.
            head.next = head; // Help GC
            head = first;

            x = first.value; // Retrieve the value

            // Atomically decrement size.
            // getAndDecrement returns the OLD value (c) before subtracting 1.
            c = current_size.getAndDecrement();

            // CASCADING SIGNAL OPTIMIZATION:
            // If we removed an item and there are STILL items left (c > 1),
            // we wake up another consumer immediately.
            if (c > 1) {
                isEmpty.signal();
            }

        } finally {
            takeLock.unlock();
        }

        // SIGNAL PRODUCER:
        // If c was equal to capacity, it means the queue WAS full.
        // We just removed one, so now there is space. Wake up a waiting producer.
        if (c == capacity) {
            signalNotFull();
        }

        return x;
    }

    // --------------------------------------------------------------------
    // HELPER METHODS (Cross-Signaling)
    // --------------------------------------------------------------------

    // Signals a waiting consumer. Must acquire takeLock first.
    private void signalNotEmpty() {
        takeLock.lock();
        try {
            isEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    // Signals a waiting producer. Must acquire putLock first.
    private void signalNotFull() {
        putLock.lock();
        try {
            isFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    public int size() {
        return current_size.get();
    }
}
