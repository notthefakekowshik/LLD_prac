package com.lldprep.foundations.behavioral.memento.good;

/**
 * ORIGINATOR: The object whose state needs to be saved and restored.
 *
 * <p>The TextEditor creates Mementos to save its internal state and uses
 * Mementos to restore to a previous state. All state access is encapsulated —
 * no external object can see or modify the internal state directly.
 */
public class TextEditor {

    private String content = "";
    private int cursorPosition = 0;
    private String selectedText = "";

    /**
     * Creates a Memento containing a snapshot of current state.
     * This is the ONLY way external code can "save" editor state.
     */
    public EditorMemento save() {
        return new EditorMemento(content, cursorPosition, selectedText);
    }

    /**
     * Restores state from a Memento.
     * The Caretaker (History) cannot examine or modify the Memento —
     * it just passes the opaque object back to us.
     */
    public void restore(EditorMemento memento) {
        this.content = memento.getContent();
        this.cursorPosition = memento.getCursorPosition();
        this.selectedText = memento.getSelectedText();
        System.out.println("  [RESTORED to state from " + memento.getTimestamp() + "]");
    }

    // ===== Editor Operations =====

    public void type(String text) {
        if (selectedText.isEmpty()) {
            content = content.substring(0, cursorPosition) + text +
                      content.substring(cursorPosition);
        } else {
            content = content.substring(0, cursorPosition) + text +
                      content.substring(cursorPosition + selectedText.length());
            selectedText = "";
        }
        cursorPosition += text.length();
    }

    public void select(int start, int end) {
        if (start >= 0 && end <= content.length() && start < end) {
            selectedText = content.substring(start, end);
            cursorPosition = end;
        }
    }

    public void delete() {
        if (!selectedText.isEmpty()) {
            content = content.substring(0, cursorPosition - selectedText.length()) +
                      content.substring(cursorPosition);
            cursorPosition -= selectedText.length();
            selectedText = "";
        } else if (cursorPosition > 0) {
            content = content.substring(0, cursorPosition - 1) +
                      content.substring(cursorPosition);
            cursorPosition--;
        }
    }

    // ===== Accessors (read-only) =====

    public String getContent() { return content; }
    public int getCursorPosition() { return cursorPosition; }
    public String getSelectedText() { return selectedText; }

    public void printState() {
        System.out.println("  Content: '" + content + "' | Cursor: " + cursorPosition +
                          " | Selection: '" + selectedText + "'");
    }
}
