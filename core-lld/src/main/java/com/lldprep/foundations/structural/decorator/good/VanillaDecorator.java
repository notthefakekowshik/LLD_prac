package com.lldprep.foundations.structural.decorator.good;

/** Concrete Decorator — adds vanilla syrup. Adding a new add-on = one new class, nothing else changes. */
public class VanillaDecorator extends BeverageDecorator {

    public VanillaDecorator(Beverage wrapped) {
        super(wrapped);
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + " + Vanilla";
    }

    @Override
    public double getCost() {
        return wrapped.getCost() + 0.50;
    }
}
