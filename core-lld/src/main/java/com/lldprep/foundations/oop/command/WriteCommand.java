package com.lldprep.foundations.oop.command;

/**
 * ConcreteCommand — types text into the document.
 * undo() removes exactly what was typed.
 */
public class WriteCommand implements Command {

    private final TextDocument document;
    private final String text;

    public WriteCommand(TextDocument document, String text) {
        this.document = document;
        this.text = text;
    }

    @Override
    public void execute() {
        document.write(text);
    }

    @Override
    public void undo() {
        document.delete(text.length());
    }
}
