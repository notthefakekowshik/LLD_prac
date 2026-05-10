package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Product - Mercedes-AMG M15 E Performance Engine.
 * Used by: Mercedes AMG, McLaren, Williams, Aston Martin.
 */
public class MercedesEngine implements Engine {

    @Override
    public void start() {
        System.out.println("  [Mercedes ICE] M15 1.6L V6 turbo-hybrid firing up... VROOOOM!");
    }

    @Override
    public int getHorsepower() {
        return 1000;
    }

    @Override
    public String getSpecification() {
        return "Mercedes-AMG M15 E Performance | 1000hp | Split-turbo layout";
    }
}
