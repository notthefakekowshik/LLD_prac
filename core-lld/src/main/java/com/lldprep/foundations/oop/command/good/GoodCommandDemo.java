package com.lldprep.foundations.oop.command.good;

/**
 * Client - demonstrates the proper Command Pattern implementation.
 * 
 * BENEFITS DEMONSTRATED:
 * 1. Decoupling: RemoteControl doesn't know about Light
 * 2. Undo: Can undo any operation
 * 3. Extensibility: Easy to add new commands/devices
 * 4. Macros: Can group commands together
 * 5. Queuing: Could easily add command queue functionality
 */
public class GoodCommandDemo {
    
    public static void main(String[] args) {
        System.out.println("=== GOOD: Command Pattern Implementation ===\n");
        
        // Create receivers (lights)
        Light livingRoomLight = new Light("Living Room");
        Light kitchenLight = new Light("Kitchen");
        
        // Create commands for living room
        Command livingRoomLightOn = new LightOnCommand(livingRoomLight);
        Command livingRoomLightOff = new LightOffCommand(livingRoomLight);
        
        // Create commands for kitchen
        Command kitchenLightOn = new LightOnCommand(kitchenLight);
        Command kitchenLightOff = new LightOffCommand(kitchenLight);
        
        // Create invoker (remote control with 2 slots)
        RemoteControl remote = new RemoteControl(2);
        
        // Configure remote
        remote.setCommand(0, livingRoomLightOn, livingRoomLightOff);  // Slot 0: Living Room
        remote.setCommand(1, kitchenLightOn, kitchenLightOff);          // Slot 1: Kitchen
        
        remote.showCommands();
        
        // Turn on living room light
        System.out.println("User turns ON Living Room light:");
        remote.pressOnButton(0);
        
        // Turn on kitchen light
        System.out.println("\nUser turns ON Kitchen light:");
        remote.pressOnButton(1);
        
        // Undo last operation (kitchen light)
        System.out.println("\nUser presses UNDO:");
        remote.pressUndoButton();
        
        // Undo again (living room light)
        System.out.println("\nUser presses UNDO again:");
        remote.pressUndoButton();
        
        // Demonstrate Macro Command - "Party Mode"
        System.out.println("\n--- MACRO COMMAND (Party Mode) ---");
        MacroCommand partyMode = new MacroCommand();
        partyMode.addCommand(livingRoomLightOn);
        partyMode.addCommand(kitchenLightOn);
        
        System.out.println("Activating Party Mode (both lights on):");
        partyMode.execute();
        
        System.out.println("\nUndo Party Mode:");
        partyMode.undo();
        
        // Demonstrate flexibility - same remote, different devices!
        System.out.println("\n--- EXTENSIBILITY ---");
        System.out.println("The same RemoteControl can work with:");
        System.out.println("- Lights (LightOnCommand, LightOffCommand)");
        System.out.println("- Fans (FanOnCommand, FanOffCommand)");
        System.out.println("- TVs (TVOnCommand, TVOffCommand)");
        System.out.println("- Garage Doors (GarageDoorOpenCommand, GarageDoorCloseCommand)");
        System.out.println("Without any changes to RemoteControl class!");
    }
}
