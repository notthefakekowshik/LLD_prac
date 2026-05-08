package com.lldprep.foundations.behavioral.memento.good;

/**
 * MEMENTO: Immutable snapshot of editor state.
 *
 * <p>The Memento stores the internal state of the Originator (TextEditor).
 * It is immutable and only the Originator that created it can access its contents.
 *
 * <p>Key insight: Memento is a VALUE OBJECT — it has no behavior, only state.
 * It is passed between Originator and Caretaker (History) opaquely.
 */
public final class EditorMemento {

    // All fields are private and final — immutable snapshot
    private final String content;
    private final int cursorPosition;
    private final String selectedText;
    private final long timestamp;

    // Package-private constructor — only TextEditor (same package) can create
    EditorMemento(String content, int cursorPosition, String selectedText) {
        this.content = content;
        this.cursorPosition = cursorPosition;
        this.selectedText = selectedText;
        this.timestamp = System.currentTimeMillis();
    }

    // Package-private getters — only TextEditor can read state
    String getContent() { return content; }
    int getCursorPosition() { return cursorPosition; }
    String getSelectedText() { return selectedText; }

    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("Memento[content='%s', cursor=%d]", content, cursorPosition);
    }
}
