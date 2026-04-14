package com.lldprep.foundations.oop.command.bad;

/**
 * Client - demonstrates the problematic implementation.
 * 
 * PROBLEMS WITH THIS APPROACH:
 * 1. Switch and Light are tightly coupled
 * 2. Cannot undo operations
 * 3. Cannot support different devices without modifying Switch
 * 4. Cannot queue operations
 * 5. Cannot log operations for auditing
 * 6. Cannot support macros (turn on multiple lights at once)
 */
public class BadSwitchDemo {
    
    public static void main(String[] args) {
        System.out.println("=== BAD: Direct Coupling (No Command Pattern) ===\n");
        
        // Create a light
        Light livingRoomLight = new Light("Living Room");
        
        // Create switch - tightly coupled to Light
        Switch switch1 = new Switch(livingRoomLight);
        
        // Turn on the light
        System.out.println("User flips switch UP:");
        switch1.flipUp();
        
        // Turn off the light
        System.out.println("\nUser flips switch DOWN:");
        switch1.flipDown();
        
        // PROBLEM: Cannot undo!
        System.out.println("\n--- PROBLEMS ---");
        System.out.println("1. Switch is hardcoded to work only with Light");
        System.out.println("2. Cannot undo last operation");
        System.out.println("3. To add a Fan, we need to modify Switch class");
        System.out.println("4. Cannot queue operations for later execution");
        System.out.println("5. Cannot log what operations were performed");
        
        // More problems: what if we want a Fan?
        // Switch doesn't work with Fan - we need a separate switch!
        System.out.println("\n--- MORE COUPLING PROBLEMS ---");
        System.out.println("If we add a Fan, we can't use the same Switch!");
        System.out.println("We'd need FanSwitch, TVSwitch, GarageDoorSwitch...");
    }
}
