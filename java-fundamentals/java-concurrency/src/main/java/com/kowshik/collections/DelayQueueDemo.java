package com.kowshik.collections;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

        /**
         * Creates a cache entry that expires after the specified TTL.
         *
         * @param ttlMillis time-to-live in milliseconds from now
         */
        public CacheEntry(String key, String value, long ttlMillis) {
            this.key = key;
            this.value = value;
            this.expiryTimeMillis = System.currentTimeMillis() + ttlMillis;
        }

        /**
         * Private constructor for factory methods.
         */
        private CacheEntry(String key, String value, long expiryTimeMillis, boolean unused) {
            this.key = key;
            this.value = value;
            this.expiryTimeMillis = expiryTimeMillis;
        }

        /**
         * Creates a cache entry that expires at the specified absolute time (epoch millis).
         *
         * @param expiryTimeMillis absolute time in milliseconds since epoch when this entry expires
         */
        public static CacheEntry expiresAt(String key, String value, long expiryTimeMillis) {
            return new CacheEntry(key, value, expiryTimeMillis, true);
        }

        /**
         * Creates a cache entry that expires at the specified wall-clock time today.
         * Example: expiresAt("session-D", "user-4", LocalTime.of(10, 30, 0)) expires at 10:30:00 today.
         *
         * @param expiryTime the wall-clock time when this entry should expire
         */
        public static CacheEntry expiresAt(String key, String value, LocalTime expiryTime) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            ZonedDateTime expiryDateTime = now.with(expiryTime);

            // If the time has already passed today, schedule for tomorrow
            if (expiryDateTime.isBefore(now)) {
                expiryDateTime = expiryDateTime.plusDays(1);
            }

            long expiryTimeMillis = expiryDateTime.toInstant().toEpochMilli();
            return new CacheEntry(key, value, expiryTimeMillis, true);
        }

        /**
         * Creates a cache entry that expires at the specified date and time.
         * Example: expiresAt("session-E", "user-5", LocalDateTime.of(2025, 4, 15, 10, 30))
         * expires on April 15, 2025 at 10:30:00.
         *
         * @param expiryDateTime the date and time when this entry should expire
         */
        public static CacheEntry expiresAt(String key, String value, LocalDateTime expiryDateTime) {
            long expiryTimeMillis = expiryDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return new CacheEntry(key, value, expiryTimeMillis, true);
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
        // Step 2: Add elements with different TTLs or absolute expiry times
        // ───────────────────────────────────────────────────────────────

        // Using TTL (relative time) - expires in 2 seconds from now
        expiryQueue.put(new CacheEntry("session-A", "user-1", 2000));

        // Using TTL (relative time) - expires in 4 seconds from now
        expiryQueue.put(new CacheEntry("session-B", "user-2", 4000));

        // Using absolute expiry time - expires 6 seconds from now (epoch millis)
        long futureTime = System.currentTimeMillis() + 6000;
        expiryQueue.put(CacheEntry.expiresAt("session-C", "user-3", futureTime));

        // Using wall-clock time - expires at 14:30:00 today (or tomorrow if already passed)
        expiryQueue.put(CacheEntry.expiresAt("session-D", "user-4", LocalTime.of(14, 30, 0)));

        // Using specific date and time - expires on 2025-04-15 at 10:30:00
        expiryQueue.put(CacheEntry.expiresAt("session-E", "user-5", LocalDateTime.of(2025, 4, 15, 10, 30)));

        System.out.println("Added 3 cache entries:");
        System.out.println("  - session-A: TTL 2s (relative)");
        System.out.println("  - session-B: TTL 4s (relative)");
        System.out.println("  - session-C: expires at " + futureTime + " (absolute)");
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
