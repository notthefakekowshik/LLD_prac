package com.lldprep.foundations.oop.command.good;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Invoker - Remote control that executes commands.
 * 
 * BENEFITS:
 * 1. Decoupled from Receiver (Light) - only knows about Command interface
 * 2. Supports undo functionality via command history
 * 3. Can work with any Command (LightOn, LightOff, FanOn, etc.)
 * 4. Can queue commands for later execution
 * 5. Can log all operations
 */
public class RemoteControl {
    private Command[] onCommands;
    private Command[] offCommands;
    private Deque<Command> commandHistory;

    public RemoteControl(int slots) {
        onCommands = new Command[slots];
        offCommands = new Command[slots];
        commandHistory = new ArrayDeque<>();
        
        // Initialize with NoOp commands
        Command noOp = new NoOpCommand();
        for (int i = 0; i < slots; i++) {
            onCommands[i] = noOp;
            offCommands[i] = noOp;
        }
    }

    public void setCommand(int slot, Command onCommand, Command offCommand) {
        onCommands[slot] = onCommand;
        offCommands[slot] = offCommand;
    }

    public void pressOnButton(int slot) {
        System.out.println("[Remote] Button ON pressed at slot " + slot);
        onCommands[slot].execute();
        commandHistory.push(onCommands[slot]);
    }

    public void pressOffButton(int slot) {
        System.out.println("[Remote] Button OFF pressed at slot " + slot);
        offCommands[slot].execute();
        commandHistory.push(offCommands[slot]);
    }

    public void pressUndoButton() {
        if (commandHistory.isEmpty()) {
            System.out.println("[Remote] Nothing to undo.");
            return;
        }
        System.out.println("[Remote] UNDO pressed - undoing last action...");
        Command lastCommand = commandHistory.pop();
        lastCommand.undo();
    }

    public void showCommands() {
        System.out.println("\n--- Remote Control Configuration ---");
        for (int i = 0; i < onCommands.length; i++) {
            System.out.println("[Slot " + i + "] ON: " + onCommands[i].getClass().getSimpleName() + 
                             ", OFF: " + offCommands[i].getClass().getSimpleName());
        }
        System.out.println("-----------------------------------\n");
    }
}
