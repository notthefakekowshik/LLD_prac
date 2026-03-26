// COMPOSITION GOOD: FlyingFish HAS-A SwimBehavior and HAS-A FlyBehavior.
// No copy-pasted code. No inheritance of unrelated classes.
// Behaviours can be swapped at runtime — e.g., upgrade from GlideFly to PoweredFly
// without touching this class at all.
package com.lldprep.foundations.oop.composition.good;

public class FlyingFish {

    private final String name;
    private SwimBehavior swimBehavior;
    private FlyBehavior flyBehavior;

    public FlyingFish(String name, SwimBehavior swimBehavior, FlyBehavior flyBehavior) {
        this.name = name;
        this.swimBehavior = swimBehavior;
        this.flyBehavior = flyBehavior;
    }

    // Allow runtime behaviour swap (Strategy pattern)
    public void setSwimBehavior(SwimBehavior swimBehavior) {
        this.swimBehavior = swimBehavior;
    }

    public void setFlyBehavior(FlyBehavior flyBehavior) {
        this.flyBehavior = flyBehavior;
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
