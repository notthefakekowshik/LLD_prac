package com.lldprep.foundations.structural.decorator.good;

/** Concrete Component — a second base beverage. Any add-on stacks work identically. */
public class Espresso implements Beverage {

    @Override
    public String getDescription() {
        return "Espresso";
    }

    @Override
    public double getCost() {
        return 1.50;
    }
}
