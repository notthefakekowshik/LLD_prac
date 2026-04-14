package com.lldprep.foundations.behavioral.command.good;

/**
 * Receiver — holds the actual document state and performs the low-level operations.
 * Command objects call methods on this class but never expose it to the invoker.
 */
public class TextDocument {

    private final StringBuilder content = new StringBuilder();

    public void appendText(String text) {
        content.append(text);
    }

    public void deleteLastChars(int count) {
        int from = Math.max(0, content.length() - count);
        content.delete(from, content.length());
    }

    public String getContent() {
        return content.toString();
    }
}
