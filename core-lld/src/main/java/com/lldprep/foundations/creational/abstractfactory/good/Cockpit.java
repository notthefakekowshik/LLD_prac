package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Product - Cockpit (survival cell + monocoque).
 * Designed and manufactured in-house by each team — independent of PU supplier.
 */
public interface Cockpit {
    void fitDriver();
    String getSpecification();
}
