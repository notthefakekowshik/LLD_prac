package com.lldprep.foundations.creational.abstractfactory.good;

/**
 * Concrete Factory — McLaren chassis components.
 * McLaren uses Mercedes PU but builds their OWN cockpit & suspension.
 * This is why chassis is a separate factory from PU.
 */
public class McLarenChassisFactory implements ChassisComponentFactory {

    @Override
    public Cockpit createCockpit() {
        return new McLarenCockpit();
    }

    @Override
    public Suspension createSuspension() {
        return new McLarenSuspension();
    }
}

class McLarenCockpit implements Cockpit {
    @Override
    public void fitDriver() {
        System.out.println("  [McLaren Cockpit] Carbon monocoque — ultra-tight Lando fit, papaya orange livery");
    }
    @Override
    public String getSpecification() {
        return "McLaren MCL39 monocoque | Lowest drag coefficient on grid | Bespoke driver seat";
    }
}

class McLarenSuspension implements Suspension {
    @Override
    public void adjustForCircuit(String circuitType) {
        System.out.println("  [McLaren Suspension] Pullrod front/pushrod rear — adjusted for " + circuitType);
    }
    @Override
    public String getSpecification() {
        return "McLaren pullrod-front suspension | Innovative 3rd element | Low-ride aero platform";
    }
}
