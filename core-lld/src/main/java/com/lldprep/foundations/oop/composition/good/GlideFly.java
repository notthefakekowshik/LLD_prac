// COMPOSITION GOOD: One concrete fly algorithm for gliding.
// FlyingFish composes with this to gain fly capability without any inheritance.
package com.lldprep.foundations.oop.composition.good;

public class GlideFly implements FlyBehavior {

    @Override
    public void fly() {
        System.out.println("  gliding through air.");
    }
}
