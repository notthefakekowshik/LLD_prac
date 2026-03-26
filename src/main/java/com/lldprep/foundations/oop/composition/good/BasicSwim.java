// COMPOSITION GOOD: One concrete swim algorithm — lives in ONE place.
// Change it here and every composed class (FlyingFish, Tuna, etc.) benefits automatically.
package com.lldprep.foundations.oop.composition.good;

public class BasicSwim implements SwimBehavior {

    @Override
    public void swim() {
        System.out.println("  moves fins and propels forward through water.");
    }
}
