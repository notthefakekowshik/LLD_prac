package com.lldprep.foundations.oop.command;

/**
 * Receiver — the actual document that holds the text.
 * Commands delegate all real work to this class.
 */
public class TextDocument {

    private StringBuilder content = new StringBuilder();

    public void write(String text) {
        content.append(text);
        System.out.println("[Document] wrote: \"" + text + "\"  →  content: \"" + content + "\"");
    }

    public void delete(int charCount) {
        int start = Math.max(0, content.length() - charCount);
        String removed = content.substring(start);
        content.delete(start, content.length());
        System.out.println("[Document] deleted: \"" + removed + "\"  →  content: \"" + content + "\"");
    }

    public String getContent() {
        return content.toString();
    }
}
