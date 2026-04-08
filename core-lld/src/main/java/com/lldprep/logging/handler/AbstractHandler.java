package com.lldprep.logging.handler;

import com.lldprep.logging.LogLevel;
import com.lldprep.logging.formatter.Formatter;
import com.lldprep.logging.formatter.PlainTextFormatter;
import com.lldprep.logging.model.LogRecord;

/**
 * Shared skeleton for all Handler implementations.
 * 
 * Pattern: Template Method
 * Why: The algorithm for handling a record is fixed:
 *   1. Check if this handler's level threshold is met
 *   2. Format the record
 *   3. Write the output        ← varies per subclass (console vs file)
 *   4. Forward to next handler
 * 
 * Subclasses only override write() — the rest of the algorithm is inherited.
 * This prevents code duplication and ensures level-checking is never forgotten.
 * 
 * Pattern: Strategy (via Formatter)
 * Why: AbstractHandler depends on the Formatter interface, not a concrete type.
 * ConsoleHandler and FileHandler both inherit this — swap PlainText↔JSON by
 * injecting a different Formatter at construction, no subclass change needed.
 */
public abstract class AbstractHandler implements Handler {

    private Handler next;
    private LogLevel level;
    private Formatter formatter;

    public AbstractHandler(LogLevel level, Formatter formatter) {
        this.level     = level;
        this.formatter = (formatter != null) ? formatter : new PlainTextFormatter();
    }

    /**
     * Template Method — fixed algorithm, subclass varies only write().
     */
    @Override
    public final void handle(LogRecord record) {
        if (record.getLevel().isAtLeast(this.level)) {
            String formatted = formatter.format(record);
            write(formatted, record);
        }
        // Always forward, regardless of whether THIS handler processed the record.
        // This lets a WARN-threshold ConsoleHandler coexist with a DEBUG FileHandler.
        if (next != null) {
            next.handle(record);
        }
    }

    /**
     * Writes the already-formatted output to this handler's destination.
     * Subclasses implement the actual I/O here.
     *
     * @param formatted the string produced by the Formatter
     * @param record    the original record (available if subclass needs raw fields)
     */
    protected abstract void write(String formatted, LogRecord record);

    @Override
    public Handler setNext(Handler next) {
        this.next = next;
        return next;
    }

    @Override
    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }
}
