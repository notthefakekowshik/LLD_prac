package com.lldprep.foundations.structural.decorator.good;

/** Concrete Component — base beverage with no add-ons. */
public class Coffee implements Beverage {

    @Override
    public String getDescription() {
        return "Coffee";
    }

    @Override
    public double getCost() {
        return 1.00;
    }
}
