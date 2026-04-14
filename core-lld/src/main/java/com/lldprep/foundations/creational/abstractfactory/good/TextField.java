package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Product - TextField interface.
 */
public interface TextField {
    void render();
    void setText(String text);
    String getText();
}
