package com.lldprep.foundations.oop.command.good;

/**
 * Null Object Pattern - does nothing.
 * Used to initialize slots that don't have commands yet.
 */
public class NoOpCommand implements Command {
    @Override
    public void execute() {
        // Do nothing
    }

    @Override
    public void undo() {
        // Do nothing
    }
}
