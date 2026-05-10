package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Ferrari 066/12 Engine.
 * Used by: Scuderia Ferrari, Haas, Sauber (Kick Sauber).
 */
public class FerrariEngine implements Engine {

    @Override
    public void start() {
        System.out.println("  [Ferrari ICE] 066/12 1.6L V6 turbo-hybrid igniting... signature Ferrari SCREAM!");
    }

    @Override
    public int getHorsepower() {
        return 1005;
    }

    @Override
    public String getSpecification() {
        return "Ferrari 066/12 | 1005hp | Compact turbo packaging | Best top-end power";
    }
}
