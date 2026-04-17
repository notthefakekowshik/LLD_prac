package com.lldprep.foundations.structural.decorator.good;

/**
 * Component interface — both concrete beverages and decorators implement this.
 * This is what makes decorators transparent to the client.
 */
public interface Beverage {
    String getDescription();
    double getCost();
}
