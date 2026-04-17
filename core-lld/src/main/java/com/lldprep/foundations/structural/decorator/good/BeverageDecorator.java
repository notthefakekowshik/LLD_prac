package com.lldprep.foundations.structural.decorator.good;

/**
 * Abstract Decorator — wraps a Beverage and delegates to it.
 * Concrete decorators extend this and add their own behaviour before/after delegation.
 */
public abstract class BeverageDecorator implements Beverage {

    protected final Beverage wrapped;

    protected BeverageDecorator(Beverage wrapped) {
        this.wrapped = wrapped;
    }
}
