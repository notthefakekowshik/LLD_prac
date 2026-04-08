package com.leetcodeconcurrency.DesignBoundedBlockingQueue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
    Disadvantages of this :
    1. this uses one common lock for both producers and consumers, it's a bottleneck.
    2. we can use two different locks for producers and consumers, but one instance of array deque can't be used with two different locks. They produce non deterministic results.
    3. see @TwoLockBlockingQueue.java
 */
class BruteForceBlockingQueue {

    Deque<Integer> blockingQueue;
    int capacity;

    Lock lock = new ReentrantLock();
    Condition isFull = lock.newCondition();
    Condition isEmpty = lock.newCondition();

    public BruteForceBlockingQueue(int capacity) {
        this.blockingQueue = new ArrayDeque<>(capacity);
        this.capacity = capacity;
    }

    public void enqueue(int element) throws InterruptedException {
        lock.lock();
        try {
            while (this.blockingQueue.size() == capacity) {
                isFull.await();
            }

            blockingQueue.add(element);

            isEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public int dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (this.blockingQueue.isEmpty()) {
                isEmpty.await();
            }
            int currentInteger = blockingQueue.poll();

            isFull.signal();
            return currentInteger;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return this.blockingQueue.size();
        } finally {
            lock.unlock();
        }
    }
}