package com.lldprep.foundations.behavioral.memento;

import com.lldprep.foundations.behavioral.memento.bad.HistoryManagerBad;
import com.lldprep.foundations.behavioral.memento.bad.TextEditorBad;
import com.lldprep.foundations.behavioral.memento.good.EditorHistory;
import com.lldprep.foundations.behavioral.memento.good.TextEditor;

/**
 * Demonstrates MEMENTO pattern: Save and restore object state without breaking encapsulation.
 *
 * <p><b>Problem:</b> You need to implement Undo/Redo. How do you save an object's internal
 * state without exposing it to the outside world?
 *
 * <p><b>Solution:</b> Memento pattern — three roles:
 * <ol>
 *   <li><b>Originator</b> (TextEditor): Creates and restores Mementos</li>
 *   <li><b>Memento</b> (EditorMemento): Immutable snapshot of state</li>
 *   <li><b>Caretaker</b> (EditorHistory): Stores Mementos, never modifies them</li>
 * </ol>
 */
public class MementoDemo {

    public static void main(String[] args) {
        System.out.println("===== MEMENTO PATTERN DEMO =====\n");
        System.out.println("Scenario: Text Editor with Undo/Redo functionality\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: Without Memento (violates encapsulation) ---");
        System.out.println("Problem: Editor must expose ALL internals via getters/setters.");
        System.out.println("HistoryManager must know implementation details of TextEditor.");

        TextEditorBad badEditor = new TextEditorBad();
        HistoryManagerBad badHistory = new HistoryManagerBad();

        badEditor.type("Hello");
        badHistory.save(badEditor);
        badEditor.printState();

        badEditor.type(" World");
        badHistory.save(badEditor);
        badEditor.printState();

        badHistory.undo(badEditor);
        badEditor.printState();

        System.out.println();

        // --- GOOD VERSION ---
        System.out.println("--- GOOD: With Memento (proper encapsulation) ---");
        System.out.println("Benefits:");
        System.out.println("  • Editor state is fully encapsulated");
        System.out.println("  • History knows NOTHING about Editor internals");
        System.out.println("  • New fields in Editor don't break History");

        TextEditor editor = new TextEditor();
        EditorHistory history = new EditorHistory();

        System.out.println("\n1. Type 'Hello' and save:");
        editor.type("Hello");
        history.save(editor);
        editor.printState();

        System.out.println("\n2. Type ' World' and save:");
        editor.type(" World");
        history.save(editor);
        editor.printState();

        System.out.println("\n3. Type '!!!' (not saved):");
        editor.type("!!!");
        editor.printState();

        System.out.println("\n4. Undo to last saved state:");
        history.undo(editor);
        editor.printState();

        System.out.println("\n5. Undo again to 'Hello':");
        history.undo(editor);
        editor.printState();

        System.out.println("\n6. Redo back to 'Hello World':");
        history.redo(editor);
        editor.printState();

        System.out.println("\n7. Type ' Everyone' (new branch, clears redo):");
        editor.type(" Everyone");
        history.save(editor);
        editor.printState();

        System.out.println("\n8. Try redo (should fail — new branch):");
        history.redo(editor);

        System.out.println("\n===== KEY INSIGHT =====");
        System.out.println("The Memento pattern lets you save/restore state while keeping");
        System.out.println("the Originator's implementation details private. The Caretaker");
        System.out.println("(History) treats Mementos as opaque tokens — it can't peek inside.");
        System.out.println("\nThis is how you'd implement Undo in a real text editor, IDE, or game.");
    }
}
