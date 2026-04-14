package com.lldprep.foundations.behavioral.command.good;

/** Concrete command: deletes N characters from the end. Undo re-types the deleted text. */
public class DeleteCommand implements Command {

    private final TextDocument document;
    private final int charCount;
    private String deletedText = ""; // saved for undo

    public DeleteCommand(TextDocument document, int charCount) {
        this.document = document;
        this.charCount = charCount;
    }

    @Override
    public void execute() {
        String content = document.getContent();
        int from = Math.max(0, content.length() - charCount);
        deletedText = content.substring(from); // save before deleting
        document.deleteLastChars(charCount);
    }

    @Override
    public void undo() {
        document.appendText(deletedText); // restore deleted text
    }

    @Override
    public String description() { return "Delete " + charCount + " chars"; }
}
