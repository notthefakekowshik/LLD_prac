package com.lldprep.logging.demo;

import com.lldprep.foundations.oop.abstractvsinterface.CreditCardProcessor;
import com.lldprep.foundations.oop.factory.bad.PaymentProcessor;
import com.lldprep.logging.LogLevel;
import com.lldprep.logging.Logger;
import com.lldprep.logging.factory.LoggerFactory;
import com.lldprep.logging.formatter.JsonFormatter;
import com.lldprep.logging.formatter.PlainTextFormatter;
import com.lldprep.logging.handler.ConsoleHandler;
import com.lldprep.logging.handler.FileHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Demonstrates all features of the Logging Framework.
 *
 * Sections:
 *   1. Basic logging — level filtering at Logger and Handler
 *   2. Handler chain — multiple handlers, each with different thresholds
 *   3. Formatter swap — PlainText vs JSON on the same handler
 *   4. File logging — writes to a temp file, reads back and prints
 *   5. Chain of Responsibility in action — propagation through the chain
 *   6. LoggerFactory Flyweight — same instance returned for same name
 *   7. Curveball: runtime level change
 *   8. Curveball: exception logging with stack trace
 */
public class LoggingFrameworkDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("===== LOGGING FRAMEWORK DEMO =====\n");

        demo1_BasicLevelFiltering();
        demo2_HandlerChain();
        demo3_FormatterSwap();
        demo4_FileLogging();
        demo5_ChainOfResponsibilityVisible();
        demo6_LoggerFactoryFlyweight();
        demo7_RuntimeLevelChange();
        demo8_ExceptionLogging();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo1_BasicLevelFiltering() {
        printHeader("1. Basic Level Filtering");

        Logger logger = new Logger("demo.Basic", LogLevel.WARN);
        logger.addHandler(new ConsoleHandler(LogLevel.DEBUG, new PlainTextFormatter()));

        System.out.println("Logger level = WARN. Calling debug/info/warn/error:\n");

        logger.debug("This DEBUG message is DROPPED by the Logger — never reaches handler");
        logger.info("This INFO message is also DROPPED by the Logger");
        logger.warn("This WARN message passes Logger threshold → handler processes it");
        logger.error("This ERROR message passes Logger threshold → handler processes it");

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo2_HandlerChain() {
        printHeader("2. Handler Chain — different thresholds per handler");

        Logger logger = new Logger("demo.Chain", LogLevel.DEBUG);

        ConsoleHandler debugHandler = new ConsoleHandler(LogLevel.DEBUG, new PlainTextFormatter());
        ConsoleHandler warnHandler  = new ConsoleHandler(LogLevel.WARN,  new PlainTextFormatter());

        // Wire the chain: debugHandler → warnHandler
        debugHandler.setNext(warnHandler);
        logger.addHandler(debugHandler);

        System.out.println("Logger=DEBUG. Chain: ConsoleHandler(DEBUG) → ConsoleHandler(WARN)\n");
        System.out.println("Logging INFO — expect 1 line (DEBUG handler prints, WARN handler skips):");
        logger.info("INFO event");

        System.out.println("\nLogging WARN — expect 2 lines (both handlers print):");
        logger.warn("WARN event");

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo3_FormatterSwap() {
        printHeader("3. Formatter Swap — Strategy pattern");

        Logger plainLogger = new Logger("demo.Plain", LogLevel.INFO);
        plainLogger.addHandler(new ConsoleHandler(LogLevel.INFO, new PlainTextFormatter()));

        Logger jsonLogger = new Logger("demo.Json", LogLevel.INFO);
        jsonLogger.addHandler(new ConsoleHandler(LogLevel.INFO, new JsonFormatter()));

        System.out.println("PlainTextFormatter output:");
        plainLogger.info("User login successful");

        System.out.println("\nJsonFormatter output (same event):");
        jsonLogger.info("User login successful");

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo4_FileLogging() throws IOException {
        printHeader("4. File Logging");

        String tempFile = System.getProperty("java.io.tmpdir") + "/lld-demo.log";
        System.out.println("Writing to: " + tempFile + "\n");

        FileHandler fileHandler = new FileHandler(tempFile, LogLevel.INFO, new PlainTextFormatter());
        Logger logger = new Logger("demo.File", LogLevel.INFO);
        logger.addHandler(fileHandler);

        logger.info("Application started");
        logger.warn("Low memory warning");
        logger.error("Failed to connect to database");

        fileHandler.close();

        System.out.println("File contents:");
        List<String> lines = Files.readAllLines(Paths.get(tempFile));
        lines.forEach(line -> System.out.println("  " + line));

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo5_ChainOfResponsibilityVisible() {
        printHeader("5. Chain of Responsibility — propagation trace");

        System.out.println("Chain: ConsoleHandler(ERROR) → ConsoleHandler(WARN) → ConsoleHandler(DEBUG)\n");
        System.out.println("Logging a WARN record — trace which handlers process it:\n");

        Logger logger = new Logger("demo.CoR", LogLevel.DEBUG);

        ConsoleHandler errorHandler = new ConsoleHandler(LogLevel.ERROR, new PlainTextFormatter()) {
            @Override protected void write(String formatted, com.lldprep.logging.model.LogRecord record) {
                System.out.println("[ErrorHandler] level=" + record.getLevel() + " >= ERROR? "
                    + record.getLevel().isAtLeast(LogLevel.ERROR) + " → SKIP, forward");
            }
        };
        ConsoleHandler warnHandler = new ConsoleHandler(LogLevel.WARN, new PlainTextFormatter()) {
            @Override protected void write(String formatted, com.lldprep.logging.model.LogRecord record) {
                System.out.println("[WarnHandler]  level=" + record.getLevel() + " >= WARN?  "
                    + record.getLevel().isAtLeast(LogLevel.WARN) + " → PROCESS");
                System.out.println("  Output: " + formatted);
            }
        };
        ConsoleHandler debugHandler = new ConsoleHandler(LogLevel.DEBUG, new PlainTextFormatter()) {
            @Override protected void write(String formatted, com.lldprep.logging.model.LogRecord record) {
                System.out.println("[DebugHandler] level=" + record.getLevel() + " >= DEBUG? "
                    + record.getLevel().isAtLeast(LogLevel.DEBUG) + " → PROCESS");
            }
        };

        errorHandler.setNext(warnHandler);
        warnHandler.setNext(debugHandler);
        logger.addHandler(errorHandler);

        logger.warn("Payment service timeout");

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo6_LoggerFactoryFlyweight() {
        printHeader("6. LoggerFactory — Flyweight cache");

        Logger a = LoggerFactory.getLogger("com.example.OrderService");
        Logger b = LoggerFactory.getLogger("com.example.OrderService");
        Logger c = LoggerFactory.getLogger("com.example.PaymentService");

        // This is created just for demo purpose.
        Logger d = LoggerFactory.getLogger(CreditCardProcessor.class);

        System.out.println("LoggerFactory.getLogger(\"OrderService\") called twice:");
        System.out.println("  a == b (same instance)? " + (a == b) + "  ← Flyweight: only one Logger created");
        System.out.println("  a == c (different name)? " + (a == c));
        System.out.println("  Cached logger count: " + LoggerFactory.getCachedLoggerCount());

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo7_RuntimeLevelChange() {
        printHeader("7. Curveball — Runtime level change");

        Logger logger = new Logger("demo.Runtime", LogLevel.INFO);
        logger.addHandler(new ConsoleHandler(LogLevel.DEBUG, new PlainTextFormatter()));

        System.out.println("Logger level = INFO. debug() call:");
        logger.debug("Debug message — should be DROPPED");

        System.out.println("Changing logger level to DEBUG at runtime...");
        logger.setLevel(LogLevel.DEBUG);

        System.out.println("Logger level = DEBUG. debug() call:");
        logger.debug("Debug message — should now APPEAR");

        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void demo8_ExceptionLogging() {
        printHeader("8. Curveball — Exception logging with stack trace");

        Logger logger = new Logger("demo.Exception", LogLevel.DEBUG);
        logger.addHandler(new ConsoleHandler(LogLevel.ERROR, new PlainTextFormatter()));

        System.out.println("PlainTextFormatter includes stack trace for Throwable:\n");

        try {
            riskyOperation();
        } catch (Exception e) {
            logger.error("Unhandled exception in riskyOperation", e);
        }

        System.out.println();
        System.out.println("JsonFormatter includes exception field:\n");

        Logger jsonLogger = new Logger("demo.Exception.Json", LogLevel.DEBUG);
        jsonLogger.addHandler(new ConsoleHandler(LogLevel.ERROR, new JsonFormatter()));

        try {
            riskyOperation();
        } catch (Exception e) {
            jsonLogger.error("Unhandled exception in riskyOperation", e);
        }
    }

    private static void riskyOperation() {
        throw new IllegalStateException("Something went terribly wrong");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void printHeader(String title) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.printf( "║  %-57s║%n", title);
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
    }
}
