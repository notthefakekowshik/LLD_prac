package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Windows Button.
 */
public class WindowsButton implements Button {
    private String label;
    private Runnable onClickAction;
    
    public WindowsButton(String label) {
        this.label = label;
    }
    
    @Override
    public void render() {
        System.out.println("[Windows] ┌─────────────┐");
        System.out.println("[Windows] │  " + label + "  │  ← Button");
        System.out.println("[Windows] └─────────────┘");
    }
    
    @Override
    public void onClick(Runnable action) {
        this.onClickAction = action;
    }
    
    public void click() {
        if (onClickAction != null) {
            System.out.println("[Windows] Button clicked: " + label);
            onClickAction.run();
        }
    }
}
