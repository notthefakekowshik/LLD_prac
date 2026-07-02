package com.kowshik.locks;

import java.util.concurrent.locks.StampedLock;

class SharedCoordinate {

    private final StampedLock sl = new StampedLock();
    private double x;
    private double y;

    private static final int OPTIMISTIC_RETRY_LIMIT = 3;

    // ----------------------------------------------------------------
    // WRITE
    // ----------------------------------------------------------------
    public void move(double newX, double newY) {
        long stamp = sl.writeLock();
        try {
            x = newX;
            y = newY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }

    // ----------------------------------------------------------------
    // READ — optimistic with capped retries, fallback to pessimistic
    // ----------------------------------------------------------------
    public double[] getPosition() {
        int attempts = 0;

        // --- Phase 1: try optimistic reads up to limit ---
        while (attempts < OPTIMISTIC_RETRY_LIMIT) {
            long stamp = sl.tryOptimisticRead();   // no lock acquired

            // snapshot shared state — ALL reads must happen after stamp capture
            double snapX = x;
            double snapY = y;

            if (sl.validate(stamp)) {
                return new double[]{snapX, snapY}; // clean read, no write happened
            }

            attempts++;
            // validate failed → a write happened mid-read → retry
        }

        // --- Phase 2: optimistic kept failing → take real read lock ---
        long stamp = sl.readLock();
        try {
            return new double[]{x, y};
        } finally {
            sl.unlockRead(stamp);
        }
    }
}

public class HybridStampedLock {
    public static void main(String[] args) {

    }
}
