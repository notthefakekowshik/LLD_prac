package com.lldprep.systems.logging.handler;

import com.lldprep.systems.logging.LogLevel;
import com.lldprep.systems.logging.exception.LoggerException;
import com.lldprep.systems.logging.formatter.Formatter;
import com.lldprep.systems.logging.model.LogRecord;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes formatted log records to a file.
 * 
 * Opens the file in append mode — safe to use across restarts.
 * 
 * Thread-safety: write() is synchronized on this instance so concurrent
 * threads don't interleave lines in the output file.
 * 
 * Lifecycle: call close() when the application shuts down to flush
 * the BufferedWriter and release the file handle.
 */
public class FileHandler extends AbstractHandler {

    private final String filePath;
    private final BufferedWriter writer;

    public FileHandler(String filePath) {
        super(LogLevel.DEBUG, null);
        this.filePath = filePath;
        this.writer   = openWriter(filePath);
    }

    public FileHandler(String filePath, LogLevel level) {
        super(level, null);
        this.filePath = filePath;
        this.writer   = openWriter(filePath);
    }

    public FileHandler(String filePath, LogLevel level, Formatter formatter) {
        super(level, formatter);
        this.filePath = filePath;
        this.writer   = openWriter(filePath);
    }

    private BufferedWriter openWriter(String path) {
        try {
            return new BufferedWriter(new FileWriter(path, true));
        } catch (IOException e) {
            throw new LoggerException("Cannot open log file: " + path, e);
        }
    }

    @Override
    protected synchronized void write(String formatted, LogRecord record) {
        try {
            writer.write(formatted);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to write to " + filePath + ": " + e.getMessage());
        }
    }

    /**
     * Closes the underlying file writer.
     * Should be called on application shutdown.
     */
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to close log file: " + e.getMessage());
        }
    }

    public String getFilePath() {
        return filePath;
    }
}
