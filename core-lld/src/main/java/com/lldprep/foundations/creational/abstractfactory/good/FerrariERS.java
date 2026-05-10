package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Ferrari ERS.
 * Known for aggressive energy deployment strategy — strongest on straights.
 */
public class FerrariERS implements ERS {

    @Override
    public void deploy() {
        System.out.println("  [Ferrari ERS] Deploying 163hp boost — aggressive battery deployment strategy!");
    }

    @Override
    public void harvest() {
        System.out.println("  [Ferrari ERS] Harvesting energy — optimized for maximum straight-line speed");
    }

    @Override
    public int getBoostHP() {
        return 163;
    }

    @Override
    public String getSpecification() {
        return "Ferrari ERS | 163hp boost | Aggressive deploy mode | High energy density battery";
    }
}
