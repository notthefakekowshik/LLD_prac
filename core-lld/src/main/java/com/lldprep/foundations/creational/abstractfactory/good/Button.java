package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Product - Button interface.
 */
public interface Button {
    void render();
    void onClick(Runnable action);
}
