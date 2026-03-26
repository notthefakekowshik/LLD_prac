// COMPOSITION GOOD: Focused interface for swimming behaviour.
// Any class that can swim implements this — no inheritance hierarchy required.
// Behaviour can be swapped at runtime by changing the injected implementation.
package com.lldprep.foundations.oop.composition.good;

public interface SwimBehavior {
    void swim();
}
