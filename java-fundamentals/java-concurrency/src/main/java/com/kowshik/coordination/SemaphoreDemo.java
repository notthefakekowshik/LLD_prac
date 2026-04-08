package com.kowshik.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Semaphore Demo - Resource Pool Management Tutorial
 *
 * Semaphore maintains a set of permits. Threads acquire permits before
 * accessing a resource and release them when done.
 *
 * Key Points:
 * - Controls access to a limited number of resources
 * - acquire() gets a permit (blocks if none available)
 * - release() returns a permit
 * - tryAcquire() non-blocking attempt
 * - Fair vs non-fair modes
 *
 * Use Cases:
 * - Database connection pools
 * - Rate limiting
 * - Resource throttling
 * - Limited parking spots
 */
public class SemaphoreDemo {

    private static final Logger logger = LoggerFactory.getLogger(SemaphoreDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== Semaphore Demo ===\n");

        // Demo 1: Connection pool
        connectionPoolDemo();

        Thread.sleep(2000);

        // Demo 2: Parking lot
        parkingLotDemo();

        Thread.sleep(2000);

        // Demo 3: Rate limiting
        rateLimitingDemo();
    }

    /**
     * Demo 1: Database connection pool with limited connections
     */
    private static void connectionPoolDemo() throws InterruptedException {
        logger.info("Demo 1: Database Connection Pool (max 3 connections)");

        Semaphore connectionPool = new Semaphore(3); // Only 3 connections available
        ExecutorService executor = Executors.newFixedThreadPool(6);

        for (int i = 1; i <= 6; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    logger.info("User {} requesting database connection...", userId);
                    connectionPool.acquire(); // Wait for available connection

                    logger.info("User {} acquired connection! Available: {}",
                            userId, connectionPool.availablePermits());

                    // Simulate database query
                    Thread.sleep((long) (Math.random() * 2000));

                    logger.info("User {} releasing connection", userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    connectionPool.release(); // Return connection to pool
                    logger.info("Connection released. Available: {}", connectionPool.availablePermits());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        logger.info("");
    }

    /**
     * Demo 2: Parking lot with limited spots
     */
    private static void parkingLotDemo() throws InterruptedException {
        logger.info("Demo 2: Parking Lot (4 spots available)");

        Semaphore parkingSpots = new Semaphore(4, true); // Fair semaphore
        ExecutorService executor = Executors.newFixedThreadPool(7);

        for (int i = 1; i <= 7; i++) {
            final int carId = i;
            executor.submit(() -> {
                try {
                    logger.info("Car {} looking for parking spot...", carId);

                    if (parkingSpots.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                        logger.info("Car {} PARKED! Spots available: {}",
                                carId, parkingSpots.availablePermits());

                        // Car stays parked for a while
                        Thread.sleep((long) (Math.random() * 2000));

                        logger.info("Car {} leaving parking spot", carId);
                        parkingSpots.release();
                    } else {
                        logger.info("Car {} couldn't find spot, driving away", carId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        logger.info("");
    }

    /**
     * Demo 3: Rate limiting API calls
     */
    private static void rateLimitingDemo() throws InterruptedException {
        logger.info("Demo 3: Rate Limiting (max 2 concurrent API calls)");

        RateLimiter rateLimiter = new RateLimiter(2);

        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (int i = 1; i <= 5; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    rateLimiter.makeApiCall(requestId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Rate limiter implementation using Semaphore
     */
    static class RateLimiter {
        private final Semaphore semaphore;

        public RateLimiter(int maxConcurrentCalls) {
            this.semaphore = new Semaphore(maxConcurrentCalls);
        }

        public void makeApiCall(int requestId) throws InterruptedException {
            logger.info("Request {} waiting for rate limit...", requestId);

            semaphore.acquire();
            try {
                logger.info("Request {} executing API call. Permits available: {}",
                        requestId, semaphore.availablePermits());

                // Simulate API call
                Thread.sleep(1500);

                logger.info("Request {} completed", requestId);
            } finally {
                semaphore.release();
            }
        }
    }

    /**
     * Advanced: Bounded resource pool with multiple permits
     */
    static class BoundedResourcePool<T> {
        private final Semaphore semaphore;
        private final T[] resources;
        private final boolean[] available;

        @SuppressWarnings("unchecked")
        public BoundedResourcePool(int size) {
            this.semaphore = new Semaphore(size, true);
            this.resources = (T[]) new Object[size];
            this.available = new boolean[size];
            for (int i = 0; i < size; i++) {
                available[i] = true;
            }
        }

        public T acquire() throws InterruptedException {
            semaphore.acquire();
            return getNextAvailableResource();
        }

        public void release(T resource) {
            if (markAsAvailable(resource)) {
                semaphore.release();
            }
        }

        private synchronized T getNextAvailableResource() {
            for (int i = 0; i < available.length; i++) {
                if (available[i]) {
                    available[i] = false;
                    return resources[i];
                }
            }
            return null;
        }

        private synchronized boolean markAsAvailable(T resource) {
            for (int i = 0; i < resources.length; i++) {
                if (resources[i] == resource) {
                    available[i] = true;
                    return true;
                }
            }
            return false;
        }
    }
}
