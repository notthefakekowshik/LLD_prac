package com.lldprep.logging.factory;

import com.lldprep.logging.LogLevel;
import com.lldprep.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches Logger instances by name.
 *
 * Pattern: Factory Method
 * Why: Callers never call new Logger() directly. The factory owns
 * construction, keeping Logger's constructor package-private if desired,
 * and ensuring consistent defaults are applied at one place.
 *
 * Pattern: Flyweight
 * Why: Two classes asking for getLogger("com.example.OrderService") get
 * the SAME Logger instance. This avoids duplicated handler chains and
 * ensures log level changes propagate everywhere the logger is used.
 *
 * Thread-safety: ConcurrentHashMap + computeIfAbsent guarantees that
 * at most one Logger is created per name even under concurrent access.
 */
public class LoggerFactory {

    private static final ConcurrentHashMap<String, Logger> cache = new ConcurrentHashMap<>();

    private LoggerFactory() {
    }

    /**
     * Returns the Logger for the given name, creating it with INFO level if absent.
     *
     * @param name logger name — conventionally the fully-qualified class name
     */
    public static Logger getLogger(String name) {
        return cache.computeIfAbsent(name, n -> new Logger(n, LogLevel.INFO));
    }

    /**
     * Returns the Logger for the given name, creating it with the specified
     * minimum level if absent.
     * If the logger already exists in cache, the existing instance is returned
     * (level of an existing logger is not overwritten).
     *
     * @param name  logger name
     * @param level minimum level for the logger
     */
    public static Logger getLogger(String name, LogLevel level) {
        return cache.computeIfAbsent(name, n -> new Logger(n, level));
    }

    /**
     * Convenience overload — uses the class's simple name as the logger name.
     *
     * @param clazz the class requesting the logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Returns the number of Logger instances currently cached.
     * Useful for diagnostics.
     */
    public static int getCachedLoggerCount() {
        return cache.size();
    }

    /**
     * Clears the logger cache.
     * Intended for use in tests only — do not call in production code.
     */
    public static void clearCache() {
        cache.clear();
    }
}
