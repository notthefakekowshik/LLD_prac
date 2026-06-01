package com.lldprep.systems.moviebooking.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class SeatLockInfo {
    private final String userId;
    private final LocalDateTime lockedAt;
    private final LocalDateTime expiresAt;

    public SeatLockInfo(String userId, LocalDateTime lockedAt, LocalDateTime expiresAt) {
        this.userId = userId;
        this.lockedAt = lockedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SeatLockInfo other)) {
            return false;
        }
        return Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "Locked by " + userId + ", expires " + expiresAt.toLocalTime();
    }
}
