package com.lldprep.foundations.oop.command;

/**
 * Client — wires everything together.
 *
 * Pattern roles
 * ─────────────
 * Command          →  Command (interface)
 * ConcreteCommand  →  WriteCommand
 * Receiver         →  TextDocument
 * Invoker          →  TextEditor
 * Client           →  this class
 */
public class CommandPatternDemo {

    public static void main(String[] args) {

        TextDocument document = new TextDocument();   // Receiver
        TextEditor   editor   = new TextEditor();     // Invoker

        // User types "Hello, "
        editor.executeCommand(new WriteCommand(document, "Hello, "));

        // User types "World!"
        editor.executeCommand(new WriteCommand(document, "World!"));

        // User hits Ctrl+Z  →  "World!" disappears
        editor.undo();

        // User types "Java!" instead
        editor.executeCommand(new WriteCommand(document, "Java!"));

        // User hits Ctrl+Z twice  →  both writes undone
        editor.undo();
        editor.undo();

        // Nothing left to undo
        editor.undo();
    }
}
