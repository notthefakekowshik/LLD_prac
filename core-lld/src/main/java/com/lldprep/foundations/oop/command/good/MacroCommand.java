package com.lldprep.foundations.oop.command.good;

import java.util.ArrayList;
import java.util.List;

/**
 * Macro Command - executes multiple commands as one.
 * Demonstrates the power of the Command Pattern.
 * Can combine multiple commands (e.g., "Party Mode" - turn on all lights).
 */
public class MacroCommand implements Command {
    private List<Command> commands;

    public MacroCommand() {
        this.commands = new ArrayList<>();
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void execute() {
        System.out.println("[Macro] Executing " + commands.size() + " commands:");
        for (Command command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        System.out.println("[Macro] Undoing " + commands.size() + " commands (in reverse):");
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}
