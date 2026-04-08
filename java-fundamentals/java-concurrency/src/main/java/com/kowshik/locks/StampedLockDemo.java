package com.kowshik.locks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * StampedLock Demo - Advanced Locking with Optimistic Reads
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. What is StampedLock?
 * - A lock with three modes: writing, reading, and optimistic reading.
 * - It returns a "stamp" (a long value) that is used to validate locks or
 * unlock.
 * - Not reentrant. A thread cannot acquire the same lock twice.
 *
 * 2. StampedLock vs. ReadWriteLock:
 * - StampedLock offers an "optimistic read" mode, which is very fast but
 * doesn't block writers.
 * - Writer Starvation Prevention: In ReadWriteLock, a continuous stream of
 * readers can indefinitely block a waiting writer (writer starvation).
 * StampedLock's optimistic reads avoid this completely because they do not
 * acquire an actual lock and thus never block writers.
 * - StampedLock's design can offer significantly better throughput and prevent
 * writer starvation in read-heavy scenarios.
 * - StampedLock is more complex to use correctly.
 *
 * 3. Three Modes of Operation:
 * - `writeLock()`: Exclusive lock. Blocks all other threads. Returns a stamp
 * for unlocking.
 * - `readLock()`: Non-exclusive lock. Blocks writers but allows other readers.
 * Returns a stamp for unlocking.
 * - `tryOptimisticRead()`: Returns a non-zero stamp if no write lock is held.
 * This is not a real lock. You must validate it with `validate(stamp)` after
 * reading.
 *
 * 4. Optimistic Reading Flow:
 * a. Call `tryOptimisticRead()` to get a stamp.
 * b. Read the shared variables.
 * c. Call `validate(stamp)` to check if a write happened while you were
 * reading.
 * d. If validation fails, it means the data is dirty. You must then acquire a
 * full read lock (`readLock()`) and read the data again.
 *
 * 5. Best Practices:
 * - Use optimistic reads for very short, read-only operations where contention
 * is expected to be low.
 * - Always check the stamp from `tryOptimisticRead()` and `validate()` it.
 * - The stamp is required for unlocking, so don't lose it.
 * - StampedLock is not reentrant. Be careful not to cause deadlocks.
 *
 * 6. Common Pitfalls:
 * - Forgetting to validate an optimistic read stamp.
 * - Using the result of an optimistic read when validation fails.
 * - Trying to acquire a lock reentrantly.
 */
public class StampedLockDemo {
    private static final Logger log = LoggerFactory.getLogger(StampedLockDemo.class);

    private double x, y;
    private final StampedLock lock = new StampedLock();

    /**
     * Updates the coordinates. This is a write operation.
     */
    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();
        try {
            x += deltaX;
            y += deltaY;
            log.info("Moved to ({}, {})", x, y);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Reads the distance from origin using a pessimistic read lock.
     */
    public double distanceFromOrigin() {
        long stamp = lock.readLock();
        try {
            double currentX = x;
            double currentY = y;
            // Simulate some work
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Math.sqrt(currentX * currentX + currentY * currentY);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Reads the distance from origin using an optimistic read.
     * Falls back to a pessimistic read lock if the optimistic read fails.
     */
    public double optimisticReadDistance() {
        long stamp = lock.tryOptimisticRead();
        double currentX = x;
        double currentY = y;

        if (!lock.validate(stamp)) {
            log.warn("Optimistic read failed. Falling back to pessimistic read lock.");
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    public static void main(String[] args) throws InterruptedException {
        StampedLockDemo demo = new StampedLockDemo();
        ThreadFactory writerThreadFactory = r -> new Thread(r, "writer-thread");

        ThreadFactory readerThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "reader-thread-" + threadNumber.getAndIncrement());
            }
        };

        ExecutorService writerExecutor = Executors.newSingleThreadExecutor(writerThreadFactory);
        ExecutorService readerExecutor = Executors.newFixedThreadPool(4, readerThreadFactory);

        log.info("=== StampedLock Demo ===");

        // Start a writer thread in its own pool
        writerExecutor.submit(() -> {
            for (int i = 0; i < 5; i++) {
                demo.move(1, 1);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Start reader threads in a separate pool
        for (int i = 0; i < 4; i++) {
            readerExecutor.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    double distance = demo.optimisticReadDistance();
                    log.info("Reader (optimistic): Distance = {}", distance);
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        writerExecutor.shutdown();
        readerExecutor.shutdown();
        if (!writerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            writerExecutor.shutdownNow();
        }
        if (!readerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            readerExecutor.shutdownNow();
        }
    }
}
