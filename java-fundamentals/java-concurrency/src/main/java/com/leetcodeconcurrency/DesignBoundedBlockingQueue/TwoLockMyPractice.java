package com.leetcodeconcurrency.DesignBoundedBlockingQueue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Node<E> {
    E data;
    Node<E> next;
    Node(E data) {
        this.data = data;
        this.next = null;
    }
}

class BlockingQueue<E> {
    private final int capacity;
    private final Lock putLock = new ReentrantLock();
    private final Lock takeLock = new ReentrantLock();

    private final Condition isFull = putLock.newCondition();
    private final Condition isEmpty = takeLock.newCondition();

    private Node<E> head;
    private Node<E> tail;

    private final AtomicInteger currentAtomicCount;

    public BlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.capacity = capacity;
        this.currentAtomicCount = new AtomicInteger(0);
        Node<E> dummy = new Node<>(null);
        this.head = dummy;
        this.tail = dummy;
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException("Element cannot be null");

        boolean acquired = putLock.tryLock(timeout, unit);
        if (!acquired) return false;

        int oldCount;
        try {
            while (currentAtomicCount.get() == capacity) {
                isFull.await();
            }
            Node<E> newNode = new Node<>(e);
            head.next = newNode;   // attach after current head
            head = newNode;        // advance head to the new node

            oldCount = currentAtomicCount.incrementAndGet();
            if (oldCount + 1 < capacity) {
                isFull.signal();
            }
        } finally {
            putLock.unlock();
        }

        if (oldCount == 1) {
            signalNotEmpty();
        }
        return true;
    }

    /*
        [dummy, null]

        put(1)
        [dummy, null] -> [1, null]
        tail              head

        get()
        tail.next is [1, null]
        item is 1
        firstReal.data = null will set the node like this [null, null]
        tail = firstReal will land tail where the head is in this case.
     */

    /*
         firstReal.data = null; -> This is kind of hard to get on first thought. but think of it
            // if you dont null it, the chains grows and grows and grows.
            // tail might have traversed forward 100 times.
     */
    public E take() throws InterruptedException {
        E item;
        int oldCount;

        takeLock.lock();
        try {
            while (currentAtomicCount.get() == 0) {
                isEmpty.await();
            }
            Node<E> firstReal = tail.next;
            item = firstReal.data;
            firstReal.data = null;   // help GC
            tail = firstReal;        // dummy advances to what was just removed

            oldCount = currentAtomicCount.decrementAndGet();
            if (oldCount >= 1) {
                isEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }

        // ofcourse you can put signalNotFull() without if condition. If conditions is just for improvement.
        if (oldCount < capacity) {
            signalNotFull();
        }
        return item;
    }

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
}

public class TwoLockMyPractice {

    public static void main(String[] args) {

    }
}
