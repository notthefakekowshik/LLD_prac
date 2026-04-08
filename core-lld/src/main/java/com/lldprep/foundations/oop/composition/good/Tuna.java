// COMPOSITION GOOD: Tuna HAS-A SwimBehavior. It uses NoFly — not an exception, not a stub.
// Tuna shares the SAME BasicSwim implementation as FlyingFish — no duplication.
package com.lldprep.foundations.oop.composition.good;

public class Tuna {

    private final String name;
    private SwimBehavior swimBehavior;
    private final FlyBehavior flyBehavior = new NoFly(); // Tuna cannot fly — expressed cleanly

    public Tuna(String name, SwimBehavior swimBehavior) {
        this.name = name;
        this.swimBehavior = swimBehavior;
    }

    public void setSwimBehavior(SwimBehavior swimBehavior) {
        this.swimBehavior = swimBehavior;
    }

    public void swim() {
        System.out.print(name + " swims: ");
        swimBehavior.swim();
    }

    public void fly() {
        System.out.print(name + " flies: ");
        flyBehavior.fly();
    }
}
