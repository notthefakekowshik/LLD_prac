package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Client code - an F1 car composed from TWO independent abstract factories.
 *
 * WHY TWO FACTORIES?
 * - PowerUnitFactory: Engine + ERS + Gearbox — supplied by PU manufacturer (Mercedes, Ferrari, Honda)
 * - ChassisComponentFactory: Cockpit + Suspension — built in-house by each team
 *
 * These are DECOUPLED because:
 * - McLaren uses a Mercedes PU but builds their OWN cockpit & suspension
 * - Haas uses a Ferrari PU but designs their OWN chassis
 * - The PU supplier has NOTHING to do with chassis design
 *
 * PATTERN INSIGHT:
 * Real systems often compose MULTIPLE abstract factories — not everything belongs in one family.
 * If components don't need to be consistent with each other, they belong in SEPARATE factories.
 */
public class F1Car {
    private final String teamName;
    // From PowerUnitFactory (PU supplier — e.g., Mercedes)
    private final Engine engine;
    private final ERS ers;
    private final Gearbox gearbox;
    // From ChassisComponentFactory (in-house by team — e.g., McLaren)
    private final Cockpit cockpit;
    private final Suspension suspension;

    public F1Car(String teamName, PowerUnitFactory puFactory, ChassisComponentFactory chassisFactory) {
        this.teamName = teamName;
        // PU components — all from same manufacturer (guaranteed by PowerUnitFactory)
        this.engine = puFactory.createEngine();
        this.ers = puFactory.createERS();
        this.gearbox = puFactory.createGearbox();
        // Chassis components — team's own design (independent of PU)
        this.cockpit = chassisFactory.createCockpit();
        this.suspension = chassisFactory.createSuspension();
    }

    public void showSpecifications() {
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("  " + teamName + " — Full Car Specifications");
        System.out.println("╠═══════════════════ POWER UNIT (supplier) ═════════════════════════╣");
        System.out.println("  Engine:     " + engine.getSpecification());
        System.out.println("  ERS:        " + ers.getSpecification());
        System.out.println("  Gearbox:    " + gearbox.getSpecification());
        System.out.println("  PU Total:   " + (engine.getHorsepower() + ers.getBoostHP()) + "hp combined");
        System.out.println("╠═══════════════════ CHASSIS (in-house) ════════════════════════════╣");
        System.out.println("  Cockpit:    " + cockpit.getSpecification());
        System.out.println("  Suspension: " + suspension.getSpecification());
        System.out.println("╚════════════════════════════════════════════════════════════════════╝\n");
    }

    public void simulateLap(String circuitType) {
        System.out.println("--- " + teamName + " — Lap Simulation (" + circuitType + ") ---");
        cockpit.fitDriver();
        suspension.adjustForCircuit(circuitType);
        engine.start();
        gearbox.shiftUp();  // 1st
        gearbox.shiftUp();  // 2nd
        gearbox.shiftUp();  // 3rd
        ers.deploy();
        gearbox.shiftUp();  // 4th
        gearbox.shiftUp();  // 5th
        ers.harvest();
        gearbox.shiftDown(); // braking zone
        gearbox.shiftDown();
        ers.deploy();
        System.out.println("  [" + teamName + "] Crossing the line in Gear " + gearbox.getCurrentGear() + "!\n");
    }
}
