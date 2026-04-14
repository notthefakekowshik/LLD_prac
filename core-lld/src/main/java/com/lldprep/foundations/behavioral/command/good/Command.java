package com.lldprep.foundations.behavioral.command.good;

/**
 * Command interface — every operation is encapsulated as an object.
 * This makes operations first-class: they can be stored, queued, logged, or undone.
 */
public interface Command {
    void execute();
    void undo();
    String description(); // useful for audit logs and history display
}
