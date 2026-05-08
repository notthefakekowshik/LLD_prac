package com.lldprep.foundations.behavioral.memento.good;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CARETAKER: Stores Mementos but NEVER examines or modifies them.
 *
 * <p>The Caretaker is responsible for the Memento's lifecycle:
 * <ul>
 *   <li>Saving: Asks Originator to create Memento, stores it</li>
 *   <li>Undo/Redo: Retrieves Memento, passes it back to Originator</li>
 * </ul>
 *
 * <p>Key constraint: Caretaker treats Mementos as opaque objects.
 * It cannot access the internal state — that would violate encapsulation.
 */
public class EditorHistory {

    private final Deque<EditorMemento> undoStack = new ArrayDeque<>();
    private final Deque<EditorMemento> redoStack = new ArrayDeque<>();
    private final int maxHistorySize;

    public EditorHistory() {
        this(100); // Default: keep last 100 states
    }

    public EditorHistory(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * Saves current state. Clears redo stack (new branch in history).
     */
    public void save(TextEditor editor) {
        // Remove oldest if at capacity
        if (undoStack.size() >= maxHistorySize) {
            undoStack.removeLast();
        }

        undoStack.push(editor.save());
        redoStack.clear(); // New action invalidates redo history

        System.out.println("  [SAVED state, undo stack size: " + undoStack.size() + "]");
    }

    /**
     * Undoes last action by restoring previous state.
     */
    public void undo(TextEditor editor) {
        if (undoStack.isEmpty()) {
            System.out.println("  [NOTHING TO UNDO]");
            return;
        }

        // Current state becomes redoable
        redoStack.push(undoStack.pop());

        // Restore previous state (if any), otherwise blank
        if (!undoStack.isEmpty()) {
            editor.restore(undoStack.peek());
        } else {
            editor.restore(new EditorMemento("", 0, ""));
        }

        System.out.println("  [UNDO, undo: " + undoStack.size() + ", redo: " + redoStack.size() + "]");
    }

    /**
     * Redoes previously undone action.
     */
    public void redo(TextEditor editor) {
        if (redoStack.isEmpty()) {
            System.out.println("  [NOTHING TO REDO]");
            return;
        }

        EditorMemento memento = redoStack.pop();
        undoStack.push(memento);
        editor.restore(memento);

        System.out.println("  [REDO, undo: " + undoStack.size() + ", redo: " + redoStack.size() + "]");
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
