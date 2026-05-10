package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Ferrari 8-speed Gearbox.
 * Known for lightning-fast shift times.
 */
public class FerrariGearbox implements Gearbox {
    private int currentGear = 0;

    @Override
    public void shiftUp() {
        if (currentGear < 8) {
            currentGear++;
            System.out.println("  [Ferrari Gearbox] Lightning shift UP → Gear " + currentGear);
        }
    }

    @Override
    public void shiftDown() {
        if (currentGear > 1) {
            currentGear--;
            System.out.println("  [Ferrari Gearbox] Lightning shift DOWN → Gear " + currentGear);
        }
    }

    @Override
    public int getCurrentGear() {
        return currentGear;
    }

    @Override
    public String getSpecification() {
        return "Ferrari 8-speed | Fastest shift time on grid | Titanium internals";
    }
}
