package com.lldprep.foundations.behavioral.command.good;

import java.util.Arrays;
import java.util.List;

/**
 * Composite command — executes a sequence of commands as a single unit.
 * Undo reverses them in reverse order (LIFO).
 *
 * This demonstrates the Composite + Command combination, useful for
 * "macro" operations like "Format document" = select all + apply style + deselect.
 */
public class MacroCommand implements Command {

    private final List<Command> commands;

    public MacroCommand(Command... commands) {
        this.commands = Arrays.asList(commands);
    }

    @Override
    public void execute() {
        for (Command cmd : commands) cmd.execute();
    }

    @Override
    public void undo() {
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }

    @Override
    public String description() {
        return "Macro[" + commands.stream()
                .map(Command::description)
                .reduce((a, b) -> a + " + " + b)
                .orElse("empty") + "]";
    }
}
