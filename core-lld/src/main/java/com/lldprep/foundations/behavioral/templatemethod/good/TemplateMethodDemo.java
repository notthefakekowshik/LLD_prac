package com.lldprep.foundations.behavioral.templatemethod.good;

/**
 * Template Method Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * Multiple classes share the same algorithm structure (skeleton), but differ in specific steps.
 * Without Template Method, you copy-paste the skeleton into every class. Changing a shared step
 * means editing every single class — a maintenance nightmare.
 *
 * <p><b>How it works:</b><br>
 * - The abstract class defines {@code export()} as a {@code final} template method — the skeleton.<br>
 * - Abstract methods (like {@code formatData()}) MUST be implemented by subclasses.<br>
 * - Hook methods (like {@code postExport()}) have default no-op implementations — subclasses
 *   override them only when needed.<br>
 * - The sequence of steps is enforced by the parent — subclasses cannot reorder them.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>Multiple classes share the same algorithm skeleton with minor variations in specific steps.</li>
 *   <li>You want to enforce an invariant process (steps must always run in a fixed order).</li>
 *   <li>You want to avoid duplicating common steps across subclasses.</li>
 * </ul>
 *
 * <p><b>Template Method vs Strategy (critical distinction):</b><br>
 * - {@code Template Method}: skeleton is fixed via <b>inheritance</b>. Steps vary in subclasses.<br>
 * - {@code Strategy}: the whole algorithm is swappable via <b>composition</b>. More flexible.<br>
 * - Rule: if the skeleton itself may also need to vary, prefer Strategy. Use Template Method
 *   only when the skeleton is truly invariant.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Basic template with abstract steps (CSV, JSON)</li>
 *   <li>Post-export hook override (JSON sends notification)</li>
 *   <li>Validation hook override (XML rejects oversized datasets)</li>
 * </ol>
 */
public class TemplateMethodDemo {

    public static void main(String[] args) {
        demo1_CsvAndJsonExport();
        demo2_HookOverride();
        demo3_ValidationHook();
    }

    // -------------------------------------------------------------------------

    private static void demo1_CsvAndJsonExport() {
        section("Demo 1: CSV and JSON export — same skeleton, different format step");

        DataExporter csv  = new CSVExporter();
        DataExporter json = new JSONExporter();

        csv.export("/tmp/data.csv");
        System.out.println();
        json.export("/tmp/data.json");
    }

    private static void demo2_HookOverride() {
        section("Demo 2: JSON post-export hook — notifies downstream system");
        // JSONExporter overrides postExport() — no changes needed in DataExporter or CSVExporter
        new JSONExporter().export("/reports/data.json");
    }

    private static void demo3_ValidationHook() {
        section("Demo 3: XML validation hook — rejects if too many records");
        // XMLExporter overrides validate() to enforce a max-record limit
        // Default fetchData() returns 3 records — XML allows max 2, so it aborts
        new XMLExporter().export("/exports/data.xml");
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
