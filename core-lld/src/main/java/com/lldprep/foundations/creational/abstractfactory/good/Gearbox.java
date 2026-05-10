package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Product - Gearbox.
 * 8-speed seamless-shift gearbox, mandatory in current F1 regs.
 */
public interface Gearbox {
    void shiftUp();
    void shiftDown();
    int getCurrentGear();
    String getSpecification();
}
