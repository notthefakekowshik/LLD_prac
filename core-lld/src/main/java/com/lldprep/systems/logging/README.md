# Logging Framework

An extensible, multi-handler logging framework demonstrating **Chain of Responsibility**, **Decorator**, **Strategy**, and **Template Method** patterns with a clean, thread-safe architecture.

## Features

- **5 Severity Levels** — `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL` with ordered severity comparison
- **Handler Chain** — Chain multiple handlers via `setNext()`; each handler has its own independent level threshold
- **Pluggable Formatters** — Swap `PlainTextFormatter` ↔ `JsonFormatter` by injection (Strategy pattern)
- **Decorator Stack** — `ColorDecorator`, `TimestampPrefixDecorator`, `FilterDecorator` wrap any formatter
- **LoggerFactory** — Flyweight cache ensures same logger name returns same instance
- **Stop Propagation** — Any handler can halt the chain with `stopPropagation()` flag
- **Thread-Safe** — `Logger.log()` snapshots handler list atomically; volatile level for immediate visibility
- **Zero Allocation** — Records below `Logger`'s threshold are dropped before `LogRecord` allocation

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Chain of Responsibility** | `Handler` chain via `setNext()` | Each handler processes + forwards; Logger doesn't know handler count |
| **Template Method** | `AbstractHandler.handle()` skeleton | `checkLevel → format → write → forward`; subclasses only override `write()` |
| **Strategy** | `Formatter` interface | Console/File handlers closed for modification; swap formatters at runtime |
| **Decorator** | `ColorDecorator`, `TimestampPrefixDecorator`, `FilterDecorator` | Add behaviors to any `Formatter` without subclassing |
| **Factory Method** | `LoggerFactory.getLogger(name)` | Centralized creation + caching; callers never call `new Logger()` |
| **Flyweight** | Logger cache in `LoggerFactory` | Same name → same instance, reduced object creation |

## Quick Start

```bash
# Run demo
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.logging.demo.LoggingFrameworkDemo" -pl core-lld

# Run decorator demo
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.logging.demo.DecoratorPatternDemo" -pl core-lld
```

## Package Structure

```
com.lldprep.logging/
├── Logger.java                    # Entry point, holds handler chain + min level
├── LogLevel.java                  # enum (DEBUG < INFO < WARN < ERROR < FATAL)
├── model/
│   └── LogRecord.java             # Immutable log event: timestamp, level, loggerName, message, throwable
├── handler/
│   ├── Handler.java               # Interface: handle(record) + setNext/getNext/setLevel
│   ├── AbstractHandler.java       # Template Method: level check → format → write → forward
│   ├── ConsoleHandler.java        # Writes to stdout (INFO+) / stderr (WARN+)
│   └── FileHandler.java           # Writes to file via BufferedWriter
├── formatter/
│   ├── Formatter.java             # Interface: format(LogRecord) → String
│   ├── PlainTextFormatter.java    # [TIMESTAMP] [LEVEL] [LoggerName] message
│   ├── JsonFormatter.java         # {"timestamp":..., "level":..., "logger":..., "message":...}
│   ├── ColorDecorator.java        # Adds ANSI color codes based on severity
│   ├── TimestampPrefixDecorator.java  # Prepends "↳ " marker for certain levels
│   └── FilterDecorator.java       # Pass-through filter before delegation
├── factory/
│   └── LoggerFactory.java         # Factory Method + Flyweight cache
├── exception/
│   └── LoggerException.java       # Wraps IO errors from FileHandler
└── demo/
    ├── LoggingFrameworkDemo.java  # Full demo: multiple handlers, chaining, formatters
    └── DecoratorPatternDemo.java  # Demo: stacking Color + Timestamp decorators
```

## Example Usage

```java
Logger logger = LoggerFactory.getLogger("MyApp", LogLevel.DEBUG);

Handler console = new ConsoleHandler(LogLevel.INFO);
console.setFormatter(new ColorDecorator(new PlainTextFormatter()));

Handler file = new FileHandler(LogLevel.DEBUG, "app.log");
file.setFormatter(new JsonFormatter());

console.setNext(file);
logger.addHandler(console);

logger.info("Application started");
logger.debug("Cache hit for key user:42");      // only file handler (DEBUG level)
logger.error("Connection lost", new IOException());
```

## Handler Chain Flow

```
Logger.debug("msg")
  → Logger level check: DEBUG >= DEBUG ✓
  → create LogRecord
  → ConsoleHandler: DEBUG < INFO threshold ✗ (skip)
  → FileHandler: DEBUG >= DEBUG threshold ✓
      → JsonFormatter.format(record)
      → FileHandler.write(...)
```

## Extending the System

### Add New Handler (e.g., SlackHandler)

```java
public class SlackHandler extends AbstractHandler {
    public SlackHandler(LogLevel level, Formatter formatter, String webhookUrl) {
        super(level, formatter, true); // stopPropagation = true
    }

    @Override
    protected void write(String formatted, LogRecord record) {
        // POST to Slack webhook
    }
}
```
Zero changes to existing classes — OCP satisfied.

### Add New Formatter (e.g., CsvFormatter)

```java
public class CsvFormatter implements Formatter {
    public String format(LogRecord record) {
        return String.format("%s,%s,%s,%s%n",
            record.getTimestamp(), record.getLevel(), record.getLoggerName(), record.getMessage());
    }
}
```

### Add New Decorator

```java
public class MetadataDecorator extends Formatter {
    private final Formatter inner;
    public MetadataDecorator(Formatter inner) { this.inner = inner; }
    public String format(LogRecord record) {
        return "[host=" + hostname + "] " + inner.format(record);
    }
}
```

## Thread Safety

| Component | Strategy |
|-----------|----------|
| `Logger.log()` | Snapshots handler list; `volatile` level field |
| `Logger.addHandler()` | `synchronized` — creates new chain copy |
| `LogRecord` | Immutable — safe to pass through handler chain |
| `FileHandler.write()` | `synchronized` on writer |

## Documentation

- `DESIGN.md` — Full D.I.C.E. workflow (abbreviated version)
- `DESIGN_DICE.md` — Comprehensive D.I.C.E. workflow with class diagrams, relationships, and curveballs

---

**Completed:** 2026-05-01 | **Patterns:** Chain of Responsibility, Decorator, Strategy, Template Method, Factory Method, Flyweight
