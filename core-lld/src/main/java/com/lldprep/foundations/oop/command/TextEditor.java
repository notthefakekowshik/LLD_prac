package com.lldprep.foundations.oop.command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Invoker — the editor toolbar/keyboard shortcut handler.
 * Runs commands and maintains a history stack for Ctrl+Z (undo).
 * Has zero knowledge of TextDocument internals.
 */
public class TextEditor {

    private final Deque<Command> history = new ArrayDeque<>();

    public void executeCommand(Command command) {
        command.execute();
        history.push(command);
    }

    public void undo() {
        if (history.isEmpty()) {
            System.out.println("[Editor] Nothing to undo.");
            return;
        }
        System.out.println("[Editor] Undoing last action...");
        history.pop().undo();
    }
}
