package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Factory — Red Bull chassis components (designed by Adrian Newey's legacy team).
 * Red Bull uses Honda/RBPT PU but builds their OWN chassis — famously the best aero on grid.
 */
public class RedBullChassisFactory implements ChassisComponentFactory {

    @Override
    public Cockpit createCockpit() {
        return new RedBullCockpit();
    }

    @Override
    public Suspension createSuspension() {
        return new RedBullSuspension();
    }
}

class RedBullCockpit implements Cockpit {
    @Override
    public void fitDriver() {
        System.out.println("  [Red Bull Cockpit] Carbon monocoque — Max's custom fit, minimal frontal area");
    }
    @Override
    public String getSpecification() {
        return "Red Bull RB21 monocoque | Best-in-class aero integration | Newey-era design DNA";
    }
}

class RedBullSuspension implements Suspension {
    @Override
    public void adjustForCircuit(String circuitType) {
        System.out.println("  [Red Bull Suspension] Pullrod front/pullrod rear — optimized for " + circuitType);
    }
    @Override
    public String getSpecification() {
        return "Red Bull pullrod suspension | Aero-optimized packaging | Superior mechanical grip";
    }
}
