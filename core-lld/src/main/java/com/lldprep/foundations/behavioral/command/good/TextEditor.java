package com.lldprep.foundations.behavioral.command.good;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Invoker — knows nothing about what commands do. It just executes them and maintains history.
 *
 * Key point: The invoker holds two stacks — one for undo, one for redo.
 * This is the standard pattern for undo/redo in any real application.
 */
public class TextEditor {

    private final TextDocument document;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public TextEditor(TextDocument document) {
        this.document = document;
    }

    /** Execute a command, record it in undo history, and clear redo stack. */
    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // New action clears redo history
        printState("execute: " + command.description());
    }

    /** Undo the last command and push it onto the redo stack. */
    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("  [Nothing to undo]");
            return;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        printState("undo: " + command.description());
    }

    /** Redo the last undone command. */
    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("  [Nothing to redo]");
            return;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        printState("redo: " + command.description());
    }

    public String getContent() { return document.getContent(); }

    private void printState(String action) {
        System.out.printf("  [%-40s] Content: \"%s\"%n", action, document.getContent());
    }
}
