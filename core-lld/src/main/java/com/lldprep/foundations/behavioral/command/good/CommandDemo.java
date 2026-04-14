package com.lldprep.foundations.behavioral.command.good;

/**
 * Command Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * You need to treat operations as first-class objects — so they can be stored in a history,
 * queued for later execution, logged for auditing, or undone. Without Command, operations
 * are just method calls that fire and forget — there's nothing to undo or replay.
 *
 * <p><b>How it works:</b><br>
 * - {@code Command} interface: every operation implements {@code execute()} and {@code undo()}.<br>
 * - {@code Receiver} (TextDocument): holds the actual state and business logic.<br>
 * - {@code Invoker} (TextEditor): executes commands and manages the undo/redo stacks.<br>
 * - The invoker never knows what a command does internally — total decoupling.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>You need undo/redo (text editors, drawing apps, game moves).</li>
 *   <li>You need to queue operations (task schedulers, job queues).</li>
 *   <li>You need an audit log (ATM transactions, database write-ahead log).</li>
 *   <li>You need macro/batch operations (run multiple commands as one unit).</li>
 * </ul>
 *
 * <p><b>Key roles:</b>
 * <ul>
 *   <li>{@code Command} — the operation encapsulated as an object</li>
 *   <li>{@code TextDocument} — Receiver: where the real work happens</li>
 *   <li>{@code TextEditor} — Invoker: triggers commands without knowing their internals</li>
 *   <li>{@code MacroCommand} — Composite command: many commands acting as one</li>
 * </ul>
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Basic execute + undo</li>
 *   <li>Full undo/redo stack</li>
 *   <li>MacroCommand (composite of commands)</li>
 * </ol>
 */
public class CommandDemo {

    public static void main(String[] args) {
        demo1_BasicUndoRedo();
        demo2_MultipleUndos();
        demo3_MacroCommand();
    }

    // -------------------------------------------------------------------------

    private static void demo1_BasicUndoRedo() {
        section("Demo 1: Basic execute / undo / redo");

        TextDocument doc = new TextDocument();
        TextEditor editor = new TextEditor(doc);

        editor.execute(new TypeCommand(doc, "Hello"));
        editor.execute(new TypeCommand(doc, " World"));
        editor.undo();   // removes " World"
        editor.redo();   // puts " World" back
        editor.undo();   // removes " World" again
        editor.undo();   // removes "Hello"
        editor.undo();   // nothing to undo
    }

    private static void demo2_MultipleUndos() {
        section("Demo 2: Delete command with undo (saves deleted text)");

        TextDocument doc = new TextDocument();
        TextEditor editor = new TextEditor(doc);

        editor.execute(new TypeCommand(doc, "Hello World"));
        editor.execute(new DeleteCommand(doc, 5)); // deletes "World"
        editor.undo(); // restores "World"
        editor.undo(); // removes "Hello World"
    }

    private static void demo3_MacroCommand() {
        section("Demo 3: MacroCommand — batch of commands undone as one unit");

        TextDocument doc = new TextDocument();
        TextEditor editor = new TextEditor(doc);

        // A macro that types a greeting template in one shot
        MacroCommand greetingMacro = new MacroCommand(
                new TypeCommand(doc, "Dear Customer,\n"),
                new TypeCommand(doc, "Thank you for your order.\n"),
                new TypeCommand(doc, "Regards, Support Team")
        );

        editor.execute(greetingMacro); // all 3 type commands run
        editor.undo();                  // all 3 undone in reverse — document is empty again
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
