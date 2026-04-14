package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Factory - creates Windows UI components.
 */
public class WindowsGUIFactory implements GUIFactory {
    
    @Override
    public Button createButton() {
        return new WindowsButton("Windows Button");
    }
    
    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox("Windows Checkbox");
    }
    
    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }
}
