package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Mac Checkbox.
 */
public class MacCheckbox implements Checkbox {
    private boolean checked = false;
    private String label;
    
    public MacCheckbox(String label) {
        this.label = label;
    }
    
    @Override
    public void render() {
        String symbol = checked ? "✓" : "○";
        System.out.println("[Mac] " + symbol + " " + label);
    }
    
    @Override
    public void toggle() {
        checked = !checked;
        System.out.println("[Mac] Checkbox " + label + " toggled with animation");
    }
    
    @Override
    public boolean isChecked() {
        return checked;
    }
}
