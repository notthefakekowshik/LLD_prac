package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Windows TextField.
 */
public class WindowsTextField implements TextField {
    private String text = "";
    
    @Override
    public void render() {
        System.out.println("[Windows] ┌─────────────────────────┐");
        System.out.println("[Windows] │ " + (text.isEmpty() ? "                    " : text) + " │ ← TextField");
        System.out.println("[Windows] └─────────────────────────┘");
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String getText() {
        return text;
    }
}
