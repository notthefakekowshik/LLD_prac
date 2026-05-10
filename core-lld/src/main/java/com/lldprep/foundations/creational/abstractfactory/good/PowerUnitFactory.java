package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Abstract Factory - creates families of related F1 power unit components.
 *
 * GUARANTEES:
 * - All components from same manufacturer (no mixing Mercedes engine with Ferrari gearbox)
 * - Power unit consistency across the car
 *
 * REAL F1 CONTEXT:
 * - Mercedes supplies PU to: Mercedes AMG, McLaren, Williams, Aston Martin
 * - Ferrari supplies PU to:  Scuderia Ferrari, Haas, Sauber (Kick Sauber)
 * - Honda/RBPT supplies PU to: Red Bull Racing, AlphaTauri (RB)
 * Each PU is a FAMILY of: Engine (ICE) + ERS (Energy Recovery) + Gearbox
 */
public interface PowerUnitFactory {
    Engine createEngine();
    ERS createERS();
    Gearbox createGearbox();
}
