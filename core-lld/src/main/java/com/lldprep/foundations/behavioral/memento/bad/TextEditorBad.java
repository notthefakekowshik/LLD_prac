package com.lldprep.foundations.behavioral.memento.bad;

/**
 * BAD: Text editor without Memento pattern — trying to implement undo with copy-paste.
 *
 * <p>Problem: To implement undo, we need to save state. Without Memento:
 * <ul>
 *   <li>Editor must expose ALL internal state (content, cursor, selection) publicly</li>
 *   <li>Undo logic leaks into the client (HistoryManager)</li>
 *   <li>Violates encapsulation — client knows implementation details</li>
 *   <li>Any change to Editor's internals breaks HistoryManager</li>
 * </ul>
 */
public class TextEditorBad {

    private String content = "";
    private int cursorPosition = 0;
    private String selectedText = "";

    // BAD: Had to expose internal state for "save" functionality
    // Any field added here requires changes everywhere that saves state
    public String getContent() { return content; }
    public int getCursorPosition() { return cursorPosition; }
    public String getSelectedText() { return selectedText; }

    // BAD: Had to add setters so HistoryManager can "restore"
    // Violates encapsulation — anyone can reset editor state arbitrarily
    public void setContent(String content) { this.content = content; }
    public void setCursorPosition(int pos) { this.cursorPosition = pos; }
    public void setSelectedText(String text) { this.selectedText = text; }

    public void type(String text) {
        if (selectedText.isEmpty()) {
            // Insert at cursor
            content = content.substring(0, cursorPosition) + text + content.substring(cursorPosition);
        } else {
            // Replace selection
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

    public void printState() {
        System.out.println("  Content: '" + content + "' | Cursor: " + cursorPosition + " | Selection: '" + selectedText + "'");
    }
}
