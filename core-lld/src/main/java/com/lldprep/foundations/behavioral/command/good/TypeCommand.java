package com.lldprep.foundations.behavioral.command.good;

/** Concrete command: types text into the document. Undo deletes what was typed. */
public class TypeCommand implements Command {

    private final TextDocument document;
    private final String text;

    public TypeCommand(TextDocument document, String text) {
        this.document = document;
        this.text = text;
    }

    @Override
    public void execute() {
        document.appendText(text);
    }

    @Override
    public void undo() {
        document.deleteLastChars(text.length());
    }

    @Override
    public String description() { return "Type: \"" + text + "\""; }
}
