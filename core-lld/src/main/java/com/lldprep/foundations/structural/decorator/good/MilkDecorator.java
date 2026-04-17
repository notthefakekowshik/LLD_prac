package com.lldprep.foundations.structural.decorator.good;

/** Concrete Decorator — adds milk to any beverage. */
public class MilkDecorator extends BeverageDecorator {

    public MilkDecorator(Beverage wrapped) {
        super(wrapped);
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + Milk";
    }

    @Override
    public double getCost() {
        return wrapped.getCost() + 0.25;
    }
}
