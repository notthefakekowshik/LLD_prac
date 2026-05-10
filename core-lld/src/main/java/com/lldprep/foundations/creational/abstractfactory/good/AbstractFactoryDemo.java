package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * ABSTRACT FACTORY PATTERN — F1 Car Assembly (Multiple Independent Factories)
 * =============================================================================
 *
 * WHY IT EXISTS:
 * Creates families of related objects without specifying concrete classes.
 * Ensures that products from the same family are used together.
 *
 * F1 ANALOGY — TWO INDEPENDENT FAMILIES:
 *
 * FAMILY 1 — Power Unit (supplied by manufacturer):
 *   PowerUnitFactory → Engine (ICE) + ERS + Gearbox
 *   Mercedes supplies PU to: McLaren, Williams, Aston Martin
 *   Ferrari supplies PU to:  Haas, Sauber
 *   Honda/RBPT supplies to:  Red Bull, RB
 *   → Components MUST be from same manufacturer (can't mix Mercedes engine + Ferrari gearbox)
 *
 * FAMILY 2 — Chassis (built in-house by each team):
 *   ChassisComponentFactory → Cockpit + Suspension
 *   McLaren designs their OWN cockpit & suspension (even though they use Mercedes PU)
 *   → Completely INDEPENDENT of PU supplier
 *
 * WHY TWO SEPARATE FACTORIES?
 *   If you shoved Cockpit into PowerUnitFactory, you'd be saying "Mercedes designs McLaren's cockpit"
 *   — that's WRONG. The factories are decoupled because the families are independent.
 *   Real systems often compose MULTIPLE abstract factories, not one monolithic factory.
 *
 * COMPARISON WITH FACTORY:
 * - Factory: Creates ONE type of object (e.g., just engines)
 * - Abstract Factory: Creates FAMILIES of related objects (engine + ERS + gearbox)
 *
 * @see <a href="https://en.wikipedia.org/wiki/Abstract_factory_pattern">Abstract Factory Pattern</a>
 */
public class AbstractFactoryDemo {

    public static void main(String[] args) {
        System.out.println("=== ABSTRACT FACTORY PATTERN — F1 Car Assembly ===\n");

        // 1. McLaren: Mercedes PU + McLaren's own chassis
        System.out.println("1. McLAREN — Mercedes PU + in-house chassis:");
        PowerUnitFactory mercedesPU = PowerUnitProvider.getFactory(PowerUnitProvider.Manufacturer.MERCEDES);
        ChassisComponentFactory mclarenChassis = new McLarenChassisFactory();
        F1Car mclaren = new F1Car("McLaren MCL39", mercedesPU, mclarenChassis);
        mclaren.showSpecifications();
        mclaren.simulateLap("high-speed (Monza)");

        // 2. Haas: Ferrari PU + Haas's own chassis
        System.out.println("2. HAAS — Ferrari PU + in-house chassis:");
        PowerUnitFactory ferrariPU = PowerUnitProvider.getFactory(PowerUnitProvider.Manufacturer.FERRARI);
        ChassisComponentFactory haasChassis = new HaasChassisFactory();
        F1Car haas = new F1Car("Haas VF-25", ferrariPU, haasChassis);
        haas.showSpecifications();
        haas.simulateLap("street circuit (Monaco)");

        // 3. Red Bull: Honda/RBPT PU + Red Bull's legendary chassis
        System.out.println("3. RED BULL — Honda/RBPT PU + in-house chassis:");
        PowerUnitFactory hondaPU = PowerUnitProvider.getFactory(PowerUnitProvider.Manufacturer.HONDA_RBPT);
        ChassisComponentFactory redBullChassis = new RedBullChassisFactory();
        F1Car redBull = new F1Car("Red Bull RB21", hondaPU, redBullChassis);
        redBull.showSpecifications();
        redBull.simulateLap("mixed (Silverstone)");

        // Key insight: the two factories are PLUGGABLE and INDEPENDENT
        System.out.println("=== KEY BENEFITS ===");
        System.out.println("1. TWO independent families — PU (supplier) and Chassis (in-house) are decoupled");
        System.out.println("2. GUARANTEED consistency WITHIN each family — no mixed PU components");
        System.out.println("3. PLUGGABLE across families — McLaren can switch from Mercedes PU to Audi PU");
        System.out.println("   without touching their chassis code, and vice versa");
        System.out.println("4. Easy to add new manufacturers (AudiFactory) OR new teams (new ChassisFactory)");
        System.out.println("5. F1Car (client) depends only on abstractions — never on concrete factories");
    }
}
