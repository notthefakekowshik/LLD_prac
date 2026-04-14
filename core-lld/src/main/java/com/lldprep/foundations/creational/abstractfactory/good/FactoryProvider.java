package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Factory Provider - creates the right factory based on environment.
 * 
 * Centralizes the decision of which factory to use.
 */
public class FactoryProvider {
    
    public enum Platform {
        WINDOWS, MAC, LINUX
    }
    
    public static GUIFactory getFactory(Platform platform) {
        return switch (platform) {
            case WINDOWS -> new WindowsGUIFactory();
            case MAC -> new MacGUIFactory();
            case LINUX -> new LinuxGUIFactory(); // Can add easily
            default -> throw new IllegalArgumentException("Unknown platform: " + platform);
        };
    }
    
    public static GUIFactory getFactoryFromOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getFactory(Platform.WINDOWS);
        } else if (os.contains("mac")) {
            return getFactory(Platform.MAC);
        } else {
            return getFactory(Platform.LINUX);
        }
    }
}

// Placeholder for Linux factory
class LinuxGUIFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton("Linux Button"); // Reuse or create LinuxButton
    }
    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox("Linux Checkbox");
    }
    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }
}
