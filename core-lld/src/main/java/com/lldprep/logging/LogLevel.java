package com.lldprep.logging;

/**
 * Severity levels for log records, ordered from least to most severe.
 * 
 * Each level carries an integer severity so comparisons are O(1):
 *   isAtLeast(other) = this.severity >= other.severity
 * 
 * Usage in thresholds:
 *   Logger set to INFO → drops DEBUG, passes INFO/WARN/ERROR/FATAL
 *   Handler set to WARN → drops DEBUG/INFO, passes WARN/ERROR/FATAL
 */
public enum LogLevel {

    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    /**
     * Returns true if this level is at least as severe as the given threshold.
     * Used by Logger and AbstractHandler for level filtering.
     */
    public boolean isAtLeast(LogLevel threshold) {
        return this.severity >= threshold.severity;
    }
}
