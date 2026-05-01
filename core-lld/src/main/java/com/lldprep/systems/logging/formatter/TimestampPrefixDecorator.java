package com.lldprep.systems.logging.formatter;

import com.lldprep.systems.logging.model.LogRecord;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Decorator Pattern: Prepends a custom timestamp to any formatter's output.
 *
 * Why Decorator:
 * - Adds timestamp prefix without modifying existing formatters (OCP)
 * - Can wrap PlainText, JSON, or any other formatter
 * - Configurable timestamp format
 *
 * Use Cases:
 * - Adding millisecond-precision timestamps to JSON output
 * - Custom date formats for log aggregation systems
 * - Prefixing all log lines with UTC timestamps
 *
 * Pattern Structure:
 * ```
 * Formatter (interface)
 *     ↑
 * TimestampPrefixDecorator ──has── Formatter (wrappee)
 * ```
 *
 * Example Output:
 * - Wraps PlainText:  [2026-04-07T10:15:30.123Z] 2026-04-07T10:15:30.123Z [INFO ] ...
 * - Wraps Json:       [2026-04-07T10:15:30.123Z] {"timestamp":"...","level":"INFO",...}
 */
public class TimestampPrefixDecorator implements Formatter {

    private final Formatter wrappee;
    private final DateTimeFormatter timestampFormat;
    private final String prefixTemplate;
    private final ZoneId zoneId;

    /**
     * Creates a decorator with ISO-8601 UTC timestamp prefix.
     *
     * @param wrappee the formatter to decorate
     */
    public TimestampPrefixDecorator(Formatter wrappee) {
        this(wrappee, "ISO8601", ZoneId.of("UTC"));
    }

    /**
     * Creates a decorator with custom timestamp format.
     *
     * @param wrappee the formatter to decorate
     * @param pattern DateTimeFormatter pattern (e.g., "yyyy-MM-dd HH:mm:ss")
     * @param zoneId  timezone for timestamps
     */
    public TimestampPrefixDecorator(Formatter wrappee, String pattern, ZoneId zoneId) {
        this.wrappee = wrappee;
        this.zoneId = zoneId;
        this.timestampFormat = DateTimeFormatter.ofPattern(pattern).withZone(zoneId);
        this.prefixTemplate = "[%s] ";  // [timestamp] original_output
    }

    @Override
    public String format(LogRecord record) {
        // Why: Build timestamp prefix
        String timestamp = timestampFormat.format(Instant.now());
        String prefix = String.format(prefixTemplate, timestamp);

        // Why: Get wrapped formatter's output
        String formatted = wrappee.format(record);

        // Why: Handle multi-line output (prefix each line)
        if (formatted.contains(System.lineSeparator())) {
            String[] lines = formatted.split(System.lineSeparator());
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                if (i > 0) {
                    result.append(System.lineSeparator());
                }
                // Why: Only prefix first line; indent continuation lines
                if (i == 0) {
                    result.append(prefix).append(lines[i]);
                } else {
                    result.append(" ".repeat(prefix.length())).append(lines[i]);
                }
            }
            return result.toString();
        }

        // Why: Single line - simple concatenation
        return prefix + formatted;
    }

    /**
     * Returns the zone ID used for timestamps.
     */
    public ZoneId getZoneId() {
        return zoneId;
    }

    @Override
    public String toString() {
        return "TimestampPrefixDecorator{wrappee=" + wrappee + ", zone=" + zoneId + "}";
    }
}
