package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Mac Button.
 */
public class MacButton implements Button {
    private String label;
    private Runnable onClickAction;
    
    public MacButton(String label) {
        this.label = label;
    }
    
    @Override
    public void render() {
        System.out.println("[Mac] ╭─────────────╮");
        System.out.println("[Mac] │  " + label + "  │  ← Rounded Button");
        System.out.println("[Mac] ╰─────────────╯");
    }
    
    @Override
    public void onClick(Runnable action) {
        this.onClickAction = action;
    }
    
    public void click() {
        if (onClickAction != null) {
            System.out.println("[Mac] Smooth button click animation: " + label);
            onClickAction.run();
        }
    }
}
