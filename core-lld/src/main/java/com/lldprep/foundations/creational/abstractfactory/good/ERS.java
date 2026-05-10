package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Product - ERS (Energy Recovery System).
 * Harvests kinetic (MGU-K) and heat (MGU-H) energy.
 * Provides ~160hp boost per lap.
 */
public interface ERS {
    void deploy();
    void harvest();
    int getBoostHP();
    String getSpecification();
}
