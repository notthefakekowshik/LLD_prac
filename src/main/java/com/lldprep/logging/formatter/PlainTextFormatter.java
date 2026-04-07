package com.lldprep.logging.formatter;

import com.lldprep.logging.model.LogRecord;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formats a LogRecord as a human-readable plain-text line.
 * 
 * Output format:
 *   2026-04-07T10:15:30.123Z [WARN ] [com.example.MyClass] Something went wrong
 *   Caused by: java.lang.NullPointerException: null value
 *       at com.example.MyClass.doThing(MyClass.java:42)
 */
public class PlainTextFormatter implements Formatter {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append(DATE_FMT.format(record.getTimestamp()))
          .append(" [").append(String.format("%-5s", record.getLevel().name())).append("]")
          .append(" [").append(record.getLoggerName()).append("]")
          .append(" ").append(record.getMessage());

        if (record.getThrowable() != null) {
            sb.append(System.lineSeparator())
              .append("Caused by: ").append(record.getThrowable().toString());
            for (StackTraceElement element : record.getThrowable().getStackTrace()) {
                sb.append(System.lineSeparator()).append("    at ").append(element);
            }
        }

        return sb.toString();
    }
}
