package com.lldprep.foundations.behavioral.templatemethod.good;

import java.util.List;

/**
 * Abstract class defining the template method.
 *
 * The skeleton (fetch → validate → format → write → postProcess) is fixed here.
 * Subclasses override only the steps that vary — they cannot change the sequence.
 *
 * Key design choices:
 * - {@code export()} is {@code final} — the skeleton cannot be overridden.
 * - Abstract methods MUST be implemented by subclasses (varying steps).
 * - Hook methods have default implementations — subclasses override only if needed.
 */
public abstract class DataExporter {

    /** Template method — defines the invariant skeleton. Cannot be overridden. */
    public final void export(String destination) {
        List<String> data = fetchData();
        if (!validate(data)) {
            System.out.println("  [" + formatName() + "] Validation failed. Aborting export.");
            return;
        }
        String formatted = formatData(data);
        writeToDestination(formatted, destination);
        postExport(destination); // hook — optional step
    }

    /** Fetch the raw data — shared default that subclasses may override. */
    protected List<String> fetchData() {
        return List.of("Alice,30", "Bob,25", "Charlie,35");
    }

    /** Validation hook — override to add custom validation. Default: always passes. */
    protected boolean validate(List<String> data) {
        return data != null && !data.isEmpty();
    }

    /** Format the data — MUST be implemented by each subclass (the varying step). */
    protected abstract String formatData(List<String> data);

    /** Short name for display — MUST be implemented by subclass. */
    protected abstract String formatName();

    /** Write to destination — shared implementation. */
    protected void writeToDestination(String data, String destination) {
        System.out.println("  [" + formatName() + "] Writing to " + destination + ":\n" + data);
    }

    /** Post-export hook — subclasses override if they need cleanup/notification. Default: no-op. */
    protected void postExport(String destination) { }
}
