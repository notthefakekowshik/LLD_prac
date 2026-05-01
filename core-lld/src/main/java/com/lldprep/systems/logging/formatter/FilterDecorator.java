package com.lldprep.systems.logging.formatter;

import com.lldprep.systems.logging.LogLevel;
import com.lldprep.systems.logging.model.LogRecord;

/**
 * Decorator Pattern: Conditionally formats based on log level or content.
 *
 * Why Decorator:
 * - Filters what gets formatted without changing the underlying formatter
 * - Can be combined with other decorators in any order
 * - Supports both level-based and custom predicate-based filtering
 *
 * Use Cases:
 * - Hide DEBUG logs in production console but keep in file
 * - Only format ERROR/FATAL with stack traces
 * - Filter out sensitive content (PII masking)
 *
 * Pattern Structure:
 * ```
 * Formatter (interface)
 *     ↑
 * FilterDecorator ──has── Formatter (wrappee)
 * ```
 *
 * Example:
 * ```java
 * // Only show WARN+ in console
 * Formatter consoleFmt = new FilterDecorator(
 *     new PlainTextFormatter(),
 *     LogLevel.WARN
 * );
 *
 * // Show all in file
 * Formatter fileFmt = new PlainTextFormatter();
 * ```
 */
public class FilterDecorator implements Formatter {

    private final Formatter wrappee;
    private final LogLevel minLevel;
    private final FilterPredicate predicate;

    /**
     * Functional interface for custom filtering logic.
     */
    @FunctionalInterface
    public interface FilterPredicate {
        boolean shouldFormat(LogRecord record);
    }

    /**
     * Creates a level-based filter.
     * Records below minLevel return empty string (effectively filtered).
     *
     * @param wrappee  the formatter to decorate
     * @param minLevel minimum level to allow through
     */
    public FilterDecorator(Formatter wrappee, LogLevel minLevel) {
        this(wrappee, minLevel, null);
    }

    /**
     * Creates a custom predicate-based filter.
     *
     * @param wrappee   the formatter to decorate
     * @param predicate custom filter logic
     */
    public FilterDecorator(Formatter wrappee, FilterPredicate predicate) {
        this(wrappee, LogLevel.DEBUG, predicate);
    }

    private FilterDecorator(Formatter wrappee, LogLevel minLevel, FilterPredicate predicate) {
        this.wrappee = wrappee;
        this.minLevel = minLevel;
        this.predicate = predicate;
    }

    @Override
    public String format(LogRecord record) {
        // Why: Check level threshold first
        if (!record.getLevel().isAtLeast(minLevel)) {
            return "";  // Filtered out - empty string
        }

        // Why: Apply custom predicate if provided
        if (predicate != null && !predicate.shouldFormat(record)) {
            return "";  // Filtered by predicate
        }

        // Why: Pass through to decorated formatter
        return wrappee.format(record);
    }

    /**
     * Creates a filter that excludes records matching a pattern.
     * Useful for excluding noisy log sources.
     *
     * @param wrappee     the formatter to decorate
     * @param excludePattern substring to exclude (case-insensitive)
     * @return filter decorator
     */
    public static FilterDecorator excludePattern(Formatter wrappee, String excludePattern) {
        return new FilterDecorator(wrappee, record ->
            !record.getMessage().toLowerCase().contains(excludePattern.toLowerCase())
        );
    }

    /**
     * Creates a filter that only includes records from specific logger names.
     *
     * @param wrappee      the formatter to decorate
     * @param loggerPrefix logger name prefix to include (e.g., "com.myapp.service")
     * @return filter decorator
     */
    public static FilterDecorator includeLogger(Formatter wrappee, String loggerPrefix) {
        return new FilterDecorator(wrappee, record ->
            record.getLoggerName().startsWith(loggerPrefix)
        );
    }

    @Override
    public String toString() {
        return "FilterDecorator{minLevel=" + minLevel + ", wrappee=" + wrappee + "}";
    }
}
