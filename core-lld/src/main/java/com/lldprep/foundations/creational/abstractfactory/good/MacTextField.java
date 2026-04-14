package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Mac TextField.
 */
public class MacTextField implements TextField {
    private String text = "";
    
    @Override
    public void render() {
        System.out.println("[Mac] ╭─────────────────────────╮");
        System.out.println("[Mac] │ " + (text.isEmpty() ? "                    " : text) + " │ ← Rounded TextField");
        System.out.println("[Mac] ╰─────────────────────────╯");
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
