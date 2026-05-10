package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Second Abstract Factory — creates families of chassis components (Cockpit + Suspension).
 *
 * DECOUPLED from PowerUnitFactory on purpose:
 * - PU supplier is decided by contract (Mercedes, Ferrari, Honda/RBPT)
 * - Chassis is designed IN-HOUSE by each team
 * - McLaren uses Mercedes PU but builds their own cockpit & suspension
 * - You CAN'T shove cockpit/suspension into PowerUnitFactory — they're independent families
 *
 * This shows that a real system often has MULTIPLE abstract factories, composed at the client level.
 */
public interface ChassisComponentFactory {
    Cockpit createCockpit();
    Suspension createSuspension();
}
