package com.lldprep.foundations.creational.abstractfactory.bad;

/**
 * BAD: Platform-specific UI creation with scattered if-else.
 * 
 * PROBLEMS:
 * 1. Code duplication - if-else repeated for every UI component
 * 2. Violates Single Responsibility - UI logic mixed with platform detection
 * 3. Error-prone - easy to mix components from different platforms
 * 4. Hard to add new platform - modify every creation point
 */
public class UIBad {
    private String platform; // "WINDOWS", "MAC", "LINUX"
    
    public UIBad(String platform) {
        this.platform = platform;
    }
    
    // Every component creation needs if-else - duplicated everywhere!
    public void renderButton() {
        if (platform.equals("WINDOWS")) {
            System.out.println("Rendering Windows Button");
        } else if (platform.equals("MAC")) {
            System.out.println("Rendering Mac Button");
        } else if (platform.equals("LINUX")) {
            System.out.println("Rendering Linux Button");
        }
    }
    
    public void renderCheckbox() {
        if (platform.equals("WINDOWS")) {
            System.out.println("Rendering Windows Checkbox");
        } else if (platform.equals("MAC")) {
            System.out.println("Rendering Mac Checkbox");
        } else if (platform.equals("LINUX")) {
            System.out.println("Rendering Linux Checkbox");
        }
    }
    
    public void renderTextField() {
        // Duplicated pattern again...
        if (platform.equals("WINDOWS")) {
            System.out.println("Rendering Windows TextField");
        } else if (platform.equals("MAC")) {
            System.out.println("Rendering Mac TextField");
        }
    }
    // Add 10 more components? Write 10 more if-else blocks!
}
