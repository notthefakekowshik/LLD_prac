package com.lldprep.systems.logging.demo;

import com.lldprep.systems.logging.LogLevel;
import com.lldprep.systems.logging.Logger;
import com.lldprep.systems.logging.factory.LoggerFactory;
import com.lldprep.systems.logging.formatter.*;
import com.lldprep.systems.logging.handler.ConsoleHandler;
import com.lldprep.systems.logging.handler.FileHandler;

import java.io.IOException;

/**
 * Demonstrates Decorator Pattern + Chain of Responsibility in the Logging Framework.
 *
 * DESIGN PATTERNS SHOWCASED:
 * =========================
 *
 * 1. Chain of Responsibility (Handlers)
 *    - Multiple handlers process the same log record
 *    - Each handler decides whether to process based on its level threshold
 *    - Handlers forward to next in chain
 *
 * 2. Decorator Pattern (Formatters)
 *    - ColorDecorator: Adds ANSI colors to any formatter
 *    - TimestampPrefixDecorator: Adds custom timestamp prefix
 *    - FilterDecorator: Conditionally formats based on level/content
 *    - Can be stacked: new ColorDecorator(new TimestampPrefixDecorator(new PlainTextFormatter()))
 *
 * 3. Strategy Pattern (Formatter interface)
 *    - ConsoleHandler and FileHandler use same code, different formatters
 *    - Swap formatters without changing handler code (OCP)
 *
 * 4. Template Method (AbstractHandler)
 *    - Fixed algorithm: check level → format → write → forward
 *    - Subclasses only override write()
 */
public class DecoratorPatternDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║   Logging Framework - Decorator Pattern + Chain of Responsibility ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝\n");

        // Scenario 1: Basic color decorator
        demonstrateColorDecorator();

        // Scenario 2: Stacked decorators
        demonstrateStackedDecorators();

        // Scenario 3: Filter decorator (conditional formatting)
        demonstrateFilterDecorator();

        // Scenario 4: Real-world production setup
        demonstrateProductionSetup();

        System.out.println("\n✅ All decorator scenarios completed!");
    }

    /**
     * SCENARIO 1: Color Decorator
     *
     * Wraps PlainTextFormatter with ANSI color codes.
     * Same log output, but with visual severity indication.
     */
    static void demonstrateColorDecorator() {
        System.out.println("SCENARIO 1: Color Decorator");
        System.out.println("═══════════════════════════");
        System.out.println("Pattern: Decorator wraps PlainTextFormatter with ANSI colors\n");

        Logger logger = LoggerFactory.getLogger("ColorDemo", LogLevel.DEBUG);

        // Why: Plain formatter (no color)
        Formatter plain = new PlainTextFormatter();
        logger.addHandler(new ConsoleHandler(LogLevel.DEBUG, plain));

        System.out.println("--- Plain Output ---");
        logger.debug("This is a debug message");
        logger.info("This is an info message");
        logger.warn("This is a warning");
        logger.error("This is an error!");

        // Why: Color decorator (wrapped)
        Logger colorLogger = LoggerFactory.getLogger("ColorDemo2", LogLevel.DEBUG);
        Formatter colored = new ColorDecorator(new PlainTextFormatter(), true);
        colorLogger.addHandler(new ConsoleHandler(LogLevel.DEBUG, colored));

        System.out.println("\n--- Colored Output (same messages) ---");
        colorLogger.debug("This is a debug message");
        colorLogger.info("This is an info message");
        colorLogger.warn("This is a warning");
        colorLogger.error("This is an error!");

        System.out.println("\nKey Point: ColorDecorator added behavior WITHOUT changing PlainTextFormatter");
        System.out.println("(OCP: Open for extension, closed for modification)\n");
        System.out.println("─".repeat(70) + "\n");
    }

    /**
     * SCENARIO 2: Stacked Decorators
     *
     * Decorators can wrap other decorators:
     * ColorDecorator → TimestampPrefixDecorator → JsonFormatter
     *
     * Result: JSON output with timestamp prefix AND colors
     */
    static void demonstrateStackedDecorators() {
        System.out.println("SCENARIO 2: Stacked Decorators");
        System.out.println("══════════════════════════════");
        System.out.println("Stack: ColorDecorator → TimestampPrefixDecorator → JsonFormatter\n");

        Logger logger = LoggerFactory.getLogger("StackedDemo", LogLevel.DEBUG);

        // Why: Stack multiple decorators - each adds a layer of behavior
        Formatter base = new JsonFormatter();
        Formatter withTimestamp = new TimestampPrefixDecorator(base);
        Formatter withColorAndTimestamp = new ColorDecorator(withTimestamp, true);

        // Why: Also show reverse order for comparison
        Formatter base2 = new PlainTextFormatter();
        Formatter withColorFirst = new ColorDecorator(base2, true);
        Formatter withTimestampAfterColor = new TimestampPrefixDecorator(withColorFirst);

        System.out.println("--- Timestamp → Color (JSON base) ---");
        logger.addHandler(new ConsoleHandler(LogLevel.DEBUG, withColorAndTimestamp));
        logger.info("Stacked: timestamp prefix then color wrapper");

        System.out.println("\n--- Color → Timestamp (Plain base) ---");
        logger.addHandler(new ConsoleHandler(LogLevel.DEBUG, withTimestampAfterColor));
        logger.warn("Stacked: color then timestamp prefix");

        System.out.println("\nKey Point: Decorators are composable - order matters!");
        System.out.println("Final output = color(timestamp(json(record))) or timestamp(color(plain(record)))\n");
        System.out.println("─".repeat(70) + "\n");
    }

    /**
     * SCENARIO 3: Filter Decorator
     *
     * Conditionally formats based on level or content.
     * Useful for: hiding DEBUG in console, showing all in file.
     */
    static void demonstrateFilterDecorator() {
        System.out.println("SCENARIO 3: Filter Decorator");
        System.out.println("════════════════════════════");
        System.out.println("Pattern: Conditionally format based on level/content\n");

        Logger logger = LoggerFactory.getLogger("FilterDemo", LogLevel.DEBUG);

        // Why: Console only shows WARN+ (filtered)
        Formatter consoleFmt = new FilterDecorator(
            new ColorDecorator(new PlainTextFormatter()),
            LogLevel.WARN
        );

        // Why: File shows ALL levels (unfiltered)
        Formatter fileFmt = new PlainTextFormatter();

        logger.addHandler(new ConsoleHandler(LogLevel.DEBUG, consoleFmt));
        logger.addHandler(new FileHandler("/tmp/all-logs.log", LogLevel.DEBUG, fileFmt));

        System.out.println("--- Console Output (WARN+ only) ---");
        System.out.println("(DEBUG and INFO filtered out from console view)\n");

        logger.debug("Debug: detailed diagnostics");
        logger.info("Info: normal operation");
        logger.warn("Warning: something might be wrong");
        logger.error("Error: definitely wrong!");

        System.out.println("\n--- Custom Filter (exclude 'noise' patterns) ---");

        // Why: Filter out messages containing "noise"
        Logger filteredLogger = LoggerFactory.getLogger("FilteredDemo", LogLevel.DEBUG);
        Formatter noiseFilter = FilterDecorator.excludePattern(
            new PlainTextFormatter(),
            "heartbeat"  // Exclude heartbeat/ping messages
        );
        filteredLogger.addHandler(new ConsoleHandler(LogLevel.DEBUG, noiseFilter));

        filteredLogger.info("User login successful");      // Shows
        filteredLogger.info("Heartbeat received from node1"); // Filtered (contains "heartbeat")
        filteredLogger.info("Data processing complete");      // Shows
        filteredLogger.debug("Heartbeat check passed");   // Filtered

        System.out.println("\nKey Point: FilterDecorator enables conditional formatting");
        System.out.println("Same Logger, different views based on handler configuration\n");
        System.out.println("─".repeat(70) + "\n");
    }

    /**
     * SCENARIO 4: Production Setup
     *
     * Real-world configuration:
     * - Console: Colored, WARN+, no debug spam
     * - File: Plain, ALL levels, JSON for log aggregation
     * - Error File: ERROR/FATAL only, immediate flush
     */
    static void demonstrateProductionSetup() throws IOException {
        System.out.println("SCENARIO 4: Production-Ready Setup");
        System.out.println("══════════════════════════════════");
        System.out.println("Chain of Responsibility + Decorators in real configuration\n");

        Logger appLogger = LoggerFactory.getLogger("ProductionApp", LogLevel.DEBUG);

        // Why: Handler 1 - Console with colors, only WARN+
        Formatter consoleFormatter = new FilterDecorator(
            new ColorDecorator(new PlainTextFormatter()),
            LogLevel.WARN
        );
        appLogger.addHandler(new ConsoleHandler(LogLevel.WARN, consoleFormatter));

        // Why: Handler 2 - File with all logs in JSON (for ELK/Splunk)
        Formatter jsonFormatter = new JsonFormatter();
        appLogger.addHandler(new FileHandler("/tmp/app.json.log", LogLevel.DEBUG, jsonFormatter));

        // Why: Handler 3 - Separate error file with timestamp prefix
        Formatter errorFormatter = new TimestampPrefixDecorator(
            new PlainTextFormatter(),
            "yyyy-MM-dd HH:mm:ss.SSS",
            java.time.ZoneId.systemDefault()
        );
        appLogger.addHandler(new FileHandler("/tmp/errors.log", LogLevel.ERROR, errorFormatter));

        System.out.println("Configuration:");
        System.out.println("  [Console]  ColorDecorator + FilterDecorator(WARN+) → PlainText");
        System.out.println("  [File]     JsonFormatter (all levels)");
        System.out.println("  [Errors]   TimestampPrefixDecorator → PlainText (ERROR+)");
        System.out.println();

        System.out.println("--- Simulating Application Logs ---");
        appLogger.debug("Database connection pool initialized: 10 connections");
        appLogger.info("User authentication service started on port 8080");
        appLogger.info("Cache warmed up with 1500 entries");
        appLogger.warn("Slow query detected: SELECT * FROM large_table (took 2.3s)");
        appLogger.info("Request processed: GET /api/users/123");
        appLogger.error("Database connection failed: connection timeout after 30s");
        appLogger.info("Retrying database connection...");
        appLogger.warn("Retry attempt 1/3 failed");
        appLogger.fatal("System shutting down: unable to recover database connection");

        System.out.println("\nWhat you see:");
        System.out.println("  Console: Only WARN, ERROR, FATAL in color");
        System.out.println("  app.json.log: ALL levels in JSON format (for parsing)");
        System.out.println("  errors.log: ERROR and FATAL with custom timestamp format");

        System.out.println("\nKey Points:");
        System.out.println("  ✓ Chain of Responsibility: 3 handlers process each record");
        System.out.println("  ✓ Decorators add behavior without modifying core classes");
        System.out.println("  ✓ OCP: New formatters/handlers require ZERO changes to existing code");
        System.out.println("  ✓ Thread-safe: Multiple threads can log concurrently");
        System.out.println("  ✓ Zero external dependencies (pure Java implementation)");

        System.out.println("\nLog files written to:");
        System.out.println("  /tmp/all-logs.log");
        System.out.println("  /tmp/app.json.log");
        System.out.println("  /tmp/errors.log");
    }
}
