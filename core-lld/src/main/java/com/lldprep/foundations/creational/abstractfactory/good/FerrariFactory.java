package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Factory - Scuderia Ferrari Power Unit Division.
 * Supplies power units to: Scuderia Ferrari, Haas, Sauber (Kick Sauber).
 *
 * GUARANTEE: All components (Engine + ERS + Gearbox) are Ferrari.
 */
public class FerrariFactory implements PowerUnitFactory {

    @Override
    public Engine createEngine() {
        return new FerrariEngine();
    }

    @Override
    public ERS createERS() {
        return new FerrariERS();
    }

    @Override
    public Gearbox createGearbox() {
        return new FerrariGearbox();
    }
}
