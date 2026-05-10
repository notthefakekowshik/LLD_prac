package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Mercedes 8-speed seamless-shift Gearbox.
 */
public class MercedesGearbox implements Gearbox {
    private int currentGear = 0;

    @Override
    public void shiftUp() {
        if (currentGear < 8) {
            currentGear++;
            System.out.println("  [Mercedes Gearbox] Seamless shift UP → Gear " + currentGear);
        }
    }

    @Override
    public void shiftDown() {
        if (currentGear > 1) {
            currentGear--;
            System.out.println("  [Mercedes Gearbox] Seamless shift DOWN → Gear " + currentGear);
        }
    }

    @Override
    public int getCurrentGear() {
        return currentGear;
    }

    @Override
    public String getSpecification() {
        return "Mercedes 8-speed seamless-shift | Carbon fibre casing | Longitudinal mount";
    }
}
