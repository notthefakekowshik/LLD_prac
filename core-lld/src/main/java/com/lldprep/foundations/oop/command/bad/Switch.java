package com.lldprep.foundations.oop.command.bad;

/**
 * BAD: Switch is tightly coupled to Light.
 * It directly calls Light methods instead of using commands.
 * 
 * VIOLATIONS:
 * 1. No Command interface - Switch knows about Light directly
 * 2. No encapsulation of requests as objects
 * 3. Cannot support undo functionality
 * 4. Cannot queue or log operations
 * 5. Cannot support macros (multiple operations)
 * 6. Hard to add new devices (Fan, TV, etc.) - requires modifying Switch
 */
public class Switch {
    private Light light;

    public Switch(Light light) {
        this.light = light;
    }

    // Direct coupling - Switch knows it's operating a Light
    public void flipUp() {
        light.turnOn();
    }

    public void flipDown() {
        light.turnOff();
    }

    // No undo support possible - we don't store previous state or operations
}
