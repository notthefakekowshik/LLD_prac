package com.lldprep.foundations.structural.decorator.good;

/** Concrete Decorator — adds sugar to any beverage. */
public class SugarDecorator extends BeverageDecorator {

    public SugarDecorator(Beverage wrapped) {
        super(wrapped);
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + Sugar";
    }

    @Override
    public double getCost() {
        return wrapped.getCost() + 0.10;
    }
}
