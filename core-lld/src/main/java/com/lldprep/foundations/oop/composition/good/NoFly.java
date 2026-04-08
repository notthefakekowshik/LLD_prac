// COMPOSITION GOOD: Null Object pattern for flying.
// Classes that cannot fly (like Tuna) use this instead of throwing exceptions or leaving stubs.
// The behaviour exists, but it does nothing harmful — and it is self-documenting.
package com.lldprep.foundations.oop.composition.good;

public class NoFly implements FlyBehavior {

    @Override
    public void fly() {
        System.out.println("  cannot fly.");
    }
}
