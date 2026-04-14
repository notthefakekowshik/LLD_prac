package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Client code - works with any GUIFactory.
 * 
 * BENEFIT: Same code works on Windows, Mac, Linux without changes.
 * Just pass different factory at initialization.
 */
public class Application {
    private Button button;
    private Checkbox checkbox;
    private TextField textField;
    
    public Application(GUIFactory factory) {
        // Factory ensures all components are from same family
        this.button = factory.createButton();
        this.checkbox = factory.createCheckbox();
        this.textField = factory.createTextField();
    }
    
    public void renderUI() {
        System.out.println("=== Rendering Application UI ===");
        button.render();
        checkbox.render();
        textField.render();
        System.out.println("==================================\n");
    }
    
    public void userInteraction() {
        checkbox.toggle();
        textField.setText("Hello from user!");
        textField.render();
    }
}
