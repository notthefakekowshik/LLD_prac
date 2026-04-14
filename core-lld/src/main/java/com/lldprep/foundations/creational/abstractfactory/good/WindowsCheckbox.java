package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Windows Checkbox.
 */
public class WindowsCheckbox implements Checkbox {
    private boolean checked = false;
    private String label;
    
    public WindowsCheckbox(String label) {
        this.label = label;
    }
    
    @Override
    public void render() {
        String box = checked ? "[X]" : "[ ]";
        System.out.println("[Windows] " + box + " " + label);
    }
    
    @Override
    public void toggle() {
        checked = !checked;
        System.out.println("[Windows] Checkbox " + label + " is now " + (checked ? "checked" : "unchecked"));
    }
    
    @Override
    public boolean isChecked() {
        return checked;
    }
}
