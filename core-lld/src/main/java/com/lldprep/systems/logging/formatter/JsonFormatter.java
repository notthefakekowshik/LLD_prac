package com.lldprep.systems.logging.formatter;

import com.lldprep.systems.logging.model.LogRecord;

/**
 * Formats a LogRecord as a single-line JSON object.
 * No external JSON library — built manually to keep zero dependencies.
 * 
 * Output example:
 *   {"timestamp":"2026-04-07T10:15:30.123Z","level":"WARN","logger":"com.example.MyClass","message":"Something went wrong"}
 *   {"timestamp":"...","level":"ERROR","logger":"...","message":"...","exception":"NullPointerException: null value"}
 */
public class JsonFormatter implements Formatter {

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder("{");

        appendField(sb, "timestamp", record.getTimestamp().toString(), true);
        appendField(sb, "level",     record.getLevel().name(),         false);
        appendField(sb, "logger",    record.getLoggerName(),           false);
        appendField(sb, "message",   escape(record.getMessage()),      false);

        if (record.getThrowable() != null) {
            appendField(sb, "exception", escape(record.getThrowable().toString()), false);
        }

        sb.append("}");
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value, boolean first) {
        if (!first) {
            sb.append(",");
        }
        sb.append("\"").append(key).append("\":\"").append(value).append("\"");
    }

    /**
     * Escapes characters that would break JSON string values.
     */
    private String escape(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
