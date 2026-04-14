package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Factory - creates Mac UI components.
 */
public class MacGUIFactory implements GUIFactory {
    
    @Override
    public Button createButton() {
        return new MacButton("Mac Button");
    }
    
    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox("Mac Checkbox");
    }
    
    @Override
    public TextField createTextField() {
        return new MacTextField();
    }
}
