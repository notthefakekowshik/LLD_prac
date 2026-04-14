package com.lldprep.foundations.behavioral.command.bad;

/**
 * BAD: Operations are invoked directly on the editor — no history, no undo, no queuing.
 *
 * Problems:
 * 1. No undo/redo — there's no record of what happened.
 * 2. Operations cannot be queued, logged, or replayed.
 * 3. To add undo, you'd need to modify this class directly (OCP violation).
 * 4. Cannot batch operations (macro commands).
 */
public class TextEditorBad {

    private StringBuilder content = new StringBuilder();

    public void type(String text) {
        content.append(text);
        System.out.println("Content: " + content);
    }

    public void delete(int chars) {
        if (chars > content.length()) chars = content.length();
        content.delete(content.length() - chars, content.length());
        System.out.println("Content: " + content);
    }
    // No undo. No history. No way to replay. Every operation is fire-and-forget.
}
