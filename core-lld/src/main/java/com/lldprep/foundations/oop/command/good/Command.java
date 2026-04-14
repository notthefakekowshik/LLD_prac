package com.lldprep.foundations.oop.command.good;

/**
 * Command Interface - declares execute() and undo() operations.
 * This is the key to decoupling the Invoker from the Receiver.
 */
public interface Command {
    void execute();
    void undo();
}
