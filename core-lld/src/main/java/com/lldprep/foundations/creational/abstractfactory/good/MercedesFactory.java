package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Factory - Mercedes-AMG High Performance Powertrains.
 * Supplies power units to: Mercedes AMG, McLaren, Williams, Aston Martin.
 *
 * GUARANTEE: All components (Engine + ERS + Gearbox) are Mercedes.
 * McLaren can't accidentally get a Mercedes engine with a Ferrari gearbox.
 */
public class MercedesFactory implements PowerUnitFactory {

    @Override
    public Engine createEngine() {
        return new MercedesEngine();
    }

    @Override
    public ERS createERS() {
        return new MercedesERS();
    }

    @Override
    public Gearbox createGearbox() {
        return new MercedesGearbox();
    }
}
