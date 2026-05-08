package com.lldprep.foundations.behavioral.memento.bad;

import java.util.ArrayList;
import java.util.List;

/**
 * BAD: HistoryManager has to manually extract and restore editor state.
 * It knows ALL internal details of TextEditor — tight coupling.
 */
public class HistoryManagerBad {
    private final List<EditorStateSnapshot> history = new ArrayList<>();
    private int currentIndex = -1;

    // BAD: We had to create this snapshot class that mirrors TextEditor's internals
    private static class EditorStateSnapshot {
        String content;
        int cursorPosition;
        String selectedText;
    }

    public void save(TextEditorBad editor) {
        // Remove any redo states
        while (history.size() > currentIndex + 1) {
            history.remove(history.size() - 1);
        }

        // BAD: Extracting internal state manually — breaks if Editor adds new fields
        EditorStateSnapshot snapshot = new EditorStateSnapshot();
        snapshot.content = editor.getContent();
        snapshot.cursorPosition = editor.getCursorPosition();
        snapshot.selectedText = editor.getSelectedText();

        history.add(snapshot);
        currentIndex++;
    }

    public void undo(TextEditorBad editor) {
        if (currentIndex > 0) {
            currentIndex--;
            restore(editor, history.get(currentIndex));
        }
    }

    public void redo(TextEditorBad editor) {
        if (currentIndex < history.size() - 1) {
            currentIndex++;
            restore(editor, history.get(currentIndex));
        }
    }

    private void restore(TextEditorBad editor, EditorStateSnapshot snapshot) {
        // BAD: Manually setting internal state — violates encapsulation
        editor.setContent(snapshot.content);
        editor.setCursorPosition(snapshot.cursorPosition);
        editor.setSelectedText(snapshot.selectedText);
    }
}
