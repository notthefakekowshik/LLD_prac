package com.lldprep.foundations.oop.command.good;

/**
 * ConcreteCommand - turns a light OFF.
 * Encapsulates the request to turn off a light.
 */
public class LightOffCommand implements Command {
    private Light light;
    private boolean wasOn; // Store previous state for undo

    public LightOffCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        wasOn = light.isOn(); // Save state before executing
        light.turnOff();
    }

    @Override
    public void undo() {
        if (wasOn) {
            // Only turn on if it was on before
            light.turnOn();
        }
    }
}
