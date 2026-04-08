// ISP GOOD: Human implements all three focused interfaces because humans genuinely support
// working, eating, and resting. Each interface is small and purposeful.
package com.lldprep.foundations.solid.isp.good;

public class HumanWorker implements Workable, Eatable, Restable {

    private final String name;

    public HumanWorker(String name) {
        this.name = name;
    }

    @Override
    public void work() {
        System.out.println(name + " is working.");
    }

    @Override
    public void eat(String food) {
        System.out.println(name + " is eating " + food + ".");
    }

    @Override
    public void sleep(int hours) {
        System.out.println(name + " is sleeping for " + hours + " hours.");
    }
}
