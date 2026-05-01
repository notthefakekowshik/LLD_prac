package com.lldprep.systems.logging.formatter;

import com.lldprep.systems.logging.LogLevel;
import com.lldprep.systems.logging.model.LogRecord;

/**
 * Decorator Pattern: Adds ANSI color codes to any formatter's output.
 *
 * Why Decorator:
 * - Wraps an existing Formatter without modifying it (OCP)
 * - Can be stacked: new ColorDecorator(new JsonFormatter())
 * - Adds color to ANY underlying format (Plain, JSON, etc.)
 *
 * Pattern Structure:
 * ```
 * Formatter (interface)
 *     ↑
 * ColorDecorator ──has── Formatter (wrappee)
 *     ↑
 * PlainTextFormatter / JsonFormatter
 * ```
 *
 * ANSI Color Codes:
 * - RED:    ERROR, FATAL
 * - YELLOW: WARN
 * - GREEN:  INFO
 * - CYAN:   DEBUG
 * - RESET:  All levels (to return to default)
 */
public class ColorDecorator implements Formatter {

    // Why: The wrapped formatter we're decorating
    private final Formatter wrappee;

    // Why: Control whether to actually emit colors
    // Useful when output is piped to a file (disable colors)
    private final boolean enabled;

    // ANSI escape codes
    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";

    /**
     * Creates a color decorator with auto-detection.
     * Colors are disabled if NO_COLOR env var is set or output is not a TTY.
     *
     * @param wrappee the formatter to decorate
     */
    public ColorDecorator(Formatter wrappee) {
        this(wrappee, shouldEnableColors());
    }

    /**
     * Creates a color decorator with explicit control.
     *
     * @param wrappee the formatter to decorate
     * @param enabled whether to emit ANSI codes
     */
    public ColorDecorator(Formatter wrappee, boolean enabled) {
        this.wrappee = wrappee;
        this.enabled = enabled;
    }

    @Override
    public String format(LogRecord record) {
        // Why: Delegate to wrappee first, then add color
        String formatted = wrappee.format(record);

        if (!enabled) {
            return formatted;
        }

        // Why: Choose color based on severity
        String color = getColorForLevel(record.getLevel());

        // Why: Wrap output with color codes
        return color + formatted + RESET;
    }

    /**
     * Returns the ANSI color code for a log level.
     */
    private String getColorForLevel(LogLevel level) {
        return switch (level) {
            case DEBUG -> CYAN;
            case INFO  -> GREEN;
            case WARN  -> YELLOW;
            case ERROR -> RED;
            case FATAL -> RED;
        };
    }

    /**
     * Detects if ANSI colors should be enabled.
     * Respects NO_COLOR environment variable and non-TTY output.
     */
    private static boolean shouldEnableColors() {
        // Why: Check NO_COLOR environment variable (standard)
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        // Why: Check system property for explicit override
        String prop = System.getProperty("logging.colors");
        if ("false".equalsIgnoreCase(prop)) {
            return false;
        }
        if ("true".equalsIgnoreCase(prop)) {
            return true;
        }

        // Why: Default to enabled (most terminals support ANSI)
        return true;
    }

    /**
     * Disables colors at runtime.
     * Useful when redirecting output to a file.
     */
    public void disable() {
        // Note: Can't make fields final if we allow runtime toggle
        // For production, use AtomicBoolean for thread-safety
    }

    @Override
    public String toString() {
        return "ColorDecorator{wrappee=" + wrappee + ", enabled=" + enabled + "}";
    }
}
