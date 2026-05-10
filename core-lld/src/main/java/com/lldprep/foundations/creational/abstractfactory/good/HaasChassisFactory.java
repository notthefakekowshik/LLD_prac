package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Factory — Haas chassis components.
 * Haas uses Ferrari PU AND buys some non-listed parts from Ferrari (allowed by FIA).
 * But cockit & suspension design is still their own responsibility.
 */
public class HaasChassisFactory implements ChassisComponentFactory {

    @Override
    public Cockpit createCockpit() {
        return new HaasCockpit();
    }

    @Override
    public Suspension createSuspension() {
        return new HaasSuspension();
    }
}

class HaasCockpit implements Cockpit {
    @Override
    public void fitDriver() {
        System.out.println("  [Haas Cockpit] Carbon monocoque — American-built, compact packaging");
    }
    @Override
    public String getSpecification() {
        return "Haas VF-25 monocoque | Ferrari-inspired aero concepts | Cost-effective build";
    }
}

class HaasSuspension implements Suspension {
    @Override
    public void adjustForCircuit(String circuitType) {
        System.out.println("  [Haas Suspension] Pushrod front/pushrod rear — configured for " + circuitType);
    }
    @Override
    public String getSpecification() {
        return "Haas pushrod suspension | Reliable setup | Optimized for consistency";
    }
}
