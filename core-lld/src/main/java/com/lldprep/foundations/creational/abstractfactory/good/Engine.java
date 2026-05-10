package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Product - Engine (Internal Combustion Engine).
 * The heart of the F1 power unit — 1.6L V6 turbo-hybrid.
 */
public interface Engine {
    void start();
    int getHorsepower();
    String getSpecification();
}
