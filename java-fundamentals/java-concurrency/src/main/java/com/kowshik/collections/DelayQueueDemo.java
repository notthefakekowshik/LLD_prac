package com.kowshik.collections;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates the usage of {@link java.util.concurrent.DelayQueue}.
 * <p>
 * <b>What is DelayQueue?</b>
 * <br>
 * A {@code DelayQueue} is an unbounded blocking queue of elements that implement
 * the {@link java.util.concurrent.Delayed} interface. An element can only be taken
 * from the queue when its delay has expired. The head of the queue is the element
 * whose delay expired furthest in the past. If no delay has expired, there is no head
 * and {@code poll()} returns {@code null}.
 * </p>
 *
 * <p>
 * <b>How it works internally:</b>
 * <ul>
 *   <li>Backed by a {@link java.util.PriorityQueue} — elements are ordered by their
 *       remaining delay (shortest delay = highest priority).</li>
 *   <li>Uses a single {@link java.util.concurrent.locks.ReentrantLock} for thread safety.</li>
 *   <li>A "leader" thread pattern is used: only one thread (the leader) waits with a
 *       timed wait for the head element's delay to expire. Other threads wait indefinitely
 *       until signaled. This minimizes unnecessary timed waits across threads.</li>
 *   <li>{@code take()} blocks until the head element's delay expires.</li>
 *   <li>{@code poll()} returns {@code null} immediately if no expired element is available.</li>
 *   <li>{@code put()} never blocks (unbounded queue), but inserting an element with a shorter
 *       delay than the current head wakes up the leader thread.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>When / Why to use DelayQueue:</b>
 * <ul>
 *   <li><b>Task Scheduling:</b> Execute tasks after a specified delay — this is exactly how
 *       {@code ScheduledThreadPoolExecutor} works internally.</li>
 *   <li><b>Cache Expiry:</b> Entries are added with a TTL. A background thread calls
 *       {@code take()}, and expired entries are evicted automatically.</li>
 *   <li><b>Rate Limiting / Retry with Backoff:</b> Failed requests are re-enqueued with an
 *       increasing delay. The consumer only processes them once the backoff period elapses.</li>
 *   <li><b>Session Timeout:</b> User sessions are added with a timeout delay. A reaper thread
 *       calls {@code take()} to clean up expired sessions.</li>
 *   <li><b>Order Timeout:</b> E-commerce: unpaid orders are enqueued with a 30-min delay.
 *       When they expire, the consumer cancels the order and releases inventory.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Key constraints:</b>
 * <ul>
 *   <li>Elements MUST implement {@link java.util.concurrent.Delayed}.</li>
 *   <li>The queue is unbounded — it never blocks on {@code put()}.</li>
 *   <li>Null elements are not permitted.</li>
 *   <li>Not fair — no guarantee of FIFO among elements with the same delay.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Demo below:</b> Simulates a cache expiry system where cache entries are added with
 * different TTLs. A background consumer thread picks up expired entries and evicts them.
 * </p>
 */
public class DelayQueueDemo {

    // ───────────────────────────────────────────────────────────────
    // Step 1: Create a class that implements Delayed
    // ───────────────────────────────────────────────────────────────
    static class CacheEntry implements Delayed {
        private final String key;
        private final String value;
        private final long expiryTimeMillis; // absolute time when this entry expires

        public CacheEntry(String key, String value, long ttlMillis) {
            this.key = key;
            this.value = value;
            this.expiryTimeMillis = System.currentTimeMillis() + ttlMillis;
        }

        /**
         * Returns the remaining delay. A negative or zero value means the element
         * has expired and is eligible to be taken from the queue.
         */
        @Override
        public long getDelay(TimeUnit unit) {
            long remainingMillis = expiryTimeMillis - System.currentTimeMillis();
            return unit.convert(remainingMillis, TimeUnit.MILLISECONDS);
        }

        /**
         * DelayQueue uses this to maintain priority ordering (shortest delay = head).
         */
        @Override
        public int compareTo(Delayed other) {
            long diff = this.getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(diff, 0);
        }

        @Override
        public String toString() {
            return "CacheEntry{key='" + key + "', value='" + value + "'}";
        }
    }

    public static void main(String[] args) throws InterruptedException {

        DelayQueue<CacheEntry> expiryQueue = new DelayQueue<>();

        // ───────────────────────────────────────────────────────────────
        // Step 2: Add elements with different TTLs
        // ───────────────────────────────────────────────────────────────
        expiryQueue.put(new CacheEntry("session-A", "user-1", 2000));  // expires in 2s
        expiryQueue.put(new CacheEntry("session-B", "user-2", 4000));  // expires in 4s
        expiryQueue.put(new CacheEntry("session-C", "user-3", 6000));  // expires in 6s

        System.out.println("Added 3 cache entries with TTLs: 2s, 4s, 6s");
        System.out.println("Queue size: " + expiryQueue.size());
        System.out.println();

        // ───────────────────────────────────────────────────────────────
        // Step 3: Consumer thread — blocks on take() until delay expires
        // ───────────────────────────────────────────────────────────────
        Thread reaperThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // take() blocks until an element's delay expires
                    CacheEntry expired = expiryQueue.take();
                    System.out.println("[" + System.currentTimeMillis() / 1000 % 100 + "s] "
                            + "Expired: " + expired);
                }
            } catch (InterruptedException e) {
                System.out.println("Reaper thread interrupted, shutting down.");
                Thread.currentThread().interrupt();
            }
        }, "cache-reaper");

        reaperThread.setDaemon(true);
        reaperThread.start();

        // ───────────────────────────────────────────────────────────────
        // Step 4: poll() — non-blocking check (returns null if nothing expired yet)
        // ───────────────────────────────────────────────────────────────
        CacheEntry immediate = expiryQueue.poll();
        System.out.println("poll() immediately after adding (nothing expired yet): " + immediate);
        System.out.println();

        // Wait long enough for all entries to expire
        Thread.sleep(7000);

        System.out.println("All entries expired and evicted. Queue size: " + expiryQueue.size());
    }
}
