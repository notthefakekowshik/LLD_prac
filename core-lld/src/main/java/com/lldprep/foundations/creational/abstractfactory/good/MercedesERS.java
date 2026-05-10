package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Mercedes ERS (Energy Recovery System).
 * MGU-K (kinetic) + MGU-H (heat) — Mercedes pioneered the split-turbo MGU-H layout.
 */
public class MercedesERS implements ERS {

    @Override
    public void deploy() {
        System.out.println("  [Mercedes ERS] Deploying 160hp boost via MGU-K... battery at optimal temp");
    }

    @Override
    public void harvest() {
        System.out.println("  [Mercedes ERS] Harvesting energy — MGU-H recovering turbo heat + MGU-K on braking");
    }

    @Override
    public int getBoostHP() {
        return 160;
    }

    @Override
    public String getSpecification() {
        return "Mercedes ERS | 160hp boost | Split-turbo MGU-H layout | 4MJ battery";
    }
}
