// ISP: Human worker — implements all methods naturally. No problem here on the human side.
// The fat interface only becomes a problem when non-human implementors are introduced.
package com.lldprep.foundations.solid.isp.bad;

public class HumanWorker implements Worker {

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
