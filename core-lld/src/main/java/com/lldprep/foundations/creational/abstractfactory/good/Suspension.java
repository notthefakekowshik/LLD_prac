package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Product - Suspension system.
 * Each team designs their own — pushrod, pullrod, or innovative variants.
 * Independent of PU supplier.
 */
public interface Suspension {
    void adjustForCircuit(String circuitType);
    String getSpecification();
}
