package com.lldprep.foundations.oop.command.good;

/**
 * ConcreteCommand - turns a light ON.
 * Encapsulates the request to turn on a light.
 */
public class LightOnCommand implements Command {
    private Light light;
    private boolean wasOn; // Store previous state for undo

    public LightOnCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        wasOn = light.isOn(); // Save state before executing
        light.turnOn();
    }

    @Override
    public void undo() {
        if (!wasOn) {
            // Only turn off if it was off before
            light.turnOff();
        }
    }
}
