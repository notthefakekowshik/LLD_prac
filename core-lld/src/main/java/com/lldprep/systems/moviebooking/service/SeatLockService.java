package com.lldprep.systems.moviebooking.service;

import com.lldprep.systems.moviebooking.model.SeatLockInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-show seat locking with time expiry.
 * 
 * CONCURRENCY MODEL (Pessimistic Per-Show Lock):
 * Each show gets its own lock map and its own synchronization object.
 * All check-and-lock operations for a show are serialized via
 * synchronized(showLock) — this guarantees no two threads can grab
 * overlapping seats because the check phase is atomic with the lock phase.
 *
 * Lock duration is microseconds (HashMap put operations), so contention
 * on the show-level lock is negligible even with 100s of concurrent users.
 */
public class SeatLockService {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SeatLockInfo>> showLocks;
    private final ConcurrentHashMap<String, Object> showLockObjects;
    private final long lockDurationSeconds;

    public SeatLockService(long lockDurationSeconds) {
        this.showLocks = new ConcurrentHashMap<>();
        this.showLockObjects = new ConcurrentHashMap<>();
        this.lockDurationSeconds = lockDurationSeconds;
    }

    /**
     * Atomically checks ALL seats are free, then locks them.
     * Returns false if any seat is already locked by another user (and not expired).
     */
    public boolean lockSeats(String showId, String userId, List<String> seatIds) {
        Object lock = showLockObjects.computeIfAbsent(showId, k -> new Object());
        ConcurrentHashMap<String, SeatLockInfo> locks = showLocks.computeIfAbsent(
            showId, k -> new ConcurrentHashMap<>()
        );

        synchronized (lock) {
            // Phase 1: Validate — all seats must be free (or expired)
            for (String seatId : seatIds) {
                SeatLockInfo existing = locks.get(seatId);
                if (existing != null && !existing.isExpired() && !existing.getUserId().equals(userId)) {
                    return false;
                }
            }

            // Phase 2: Lock — now safe, no other thread can interleave
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusSeconds(lockDurationSeconds);
            for (String seatId : seatIds) {
                locks.put(seatId, new SeatLockInfo(userId, now, expiresAt));
            }
            return true;
        }
    }

    public void unlockSeats(String showId, String userId) {
        ConcurrentHashMap<String, SeatLockInfo> locks = showLocks.get(showId);
        if (locks == null) {
            return;
        }

        Object lock = showLockObjects.get(showId);
        if (lock == null) {
            return;
        }

        synchronized (lock) {
            List<String> toRemove = new ArrayList<>();
            for (var entry : locks.entrySet()) {
                if (entry.getValue().getUserId().equals(userId)) {
                    toRemove.add(entry.getKey());
                }
            }
            for (String seatId : toRemove) {
                locks.remove(seatId);
            }
        }
    }

    public boolean isLockedByUser(String showId, String seatId, String userId) {
        ConcurrentHashMap<String, SeatLockInfo> locks = showLocks.get(showId);
        if (locks == null) {
            return false;
        }
        SeatLockInfo info = locks.get(seatId);
        if (info == null) {
            return false;
        }
        return info.getUserId().equals(userId) && !info.isExpired();
    }

    public List<String> getLockedSeats(String showId, String userId) {
        List<String> result = new ArrayList<>();
        ConcurrentHashMap<String, SeatLockInfo> locks = showLocks.get(showId);
        if (locks == null) {
            return result;
        }
        for (var entry : locks.entrySet()) {
            SeatLockInfo info = entry.getValue();
            if (info.getUserId().equals(userId) && !info.isExpired()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public boolean isSeatLocked(String showId, String seatId) {
        ConcurrentHashMap<String, SeatLockInfo> locks = showLocks.get(showId);
        if (locks == null) {
            return false;
        }
        SeatLockInfo info = locks.get(seatId);
        return info != null && !info.isExpired();
    }

    public int releaseExpiredLocks(String showId) {
        ConcurrentHashMap<String, SeatLockInfo> locks = showLocks.get(showId);
        if (locks == null) {
            return 0;
        }

        Object lock = showLockObjects.get(showId);
        if (lock == null) {
            return 0;
        }

        synchronized (lock) {
            List<String> expired = new ArrayList<>();
            for (var entry : locks.entrySet()) {
                if (entry.getValue().isExpired()) {
                    expired.add(entry.getKey());
                }
            }
            for (String seatId : expired) {
                locks.remove(seatId);
            }
            return expired.size();
        }
    }
}
