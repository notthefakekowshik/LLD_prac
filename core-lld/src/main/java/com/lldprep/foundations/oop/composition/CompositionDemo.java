package com.lldprep.foundations.oop.composition;

import com.lldprep.foundations.oop.composition.bad.FlyingFish;
import com.lldprep.foundations.oop.composition.good.*;

public class CompositionDemo {

    public static void main(String[] args) {
        System.out.println("===== COMPOSITION OVER INHERITANCE =====\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: Swim and fly logic are copy-pasted into every class ---");
        System.out.println("Problem: if swim() changes, every copy (FlyingFish, Tuna, Salmon...) must be updated.");
        FlyingFish badFish = new FlyingFish("BadFlyingFish");
        badFish.swim();
        badFish.fly();
        System.out.println("Also: Tuna would need its own copy of swim() — there's no safe way to share it.");

        System.out.println();

        // --- GOOD VERSION ---
        System.out.println("--- GOOD: Behaviours are composed via injection ---");

        com.lldprep.foundations.oop.composition.good.FlyingFish goodFish =
            new com.lldprep.foundations.oop.composition.good.FlyingFish(
                "GoodFlyingFish", new BasicSwim(), new GlideFly());

        Tuna tuna = new Tuna("Tuna", new BasicSwim());

        goodFish.swim();
        goodFish.fly();

        tuna.swim();
        tuna.fly(); // uses NoFly — no exception, no crash

        System.out.println();

        // --- RUNTIME BEHAVIOUR SWAP ---
        System.out.println("--- RUNTIME SWAP: Swap FlyingFish to NoFly without changing FlyingFish class ---");
        goodFish.setFlyBehavior(new NoFly());
        goodFish.fly(); // now uses NoFly

        System.out.println();
        System.out.println("--- RUNTIME SWAP: Upgrade FlyingFish back to GlideFly ---");
        goodFish.setFlyBehavior(new GlideFly());
        goodFish.fly();

        System.out.println("\nKey insight: BasicSwim exists in ONE place and is shared by both FlyingFish and Tuna.");
        System.out.println("Changing swim algorithm requires editing ONLY BasicSwim.");

        System.out.println();

        // --- TRUE COMPOSITION (Strict UML) ---
        System.out.println("--- TRUE COMPOSITION: Car OWNS Engine internally ---");
        System.out.println("Engine is created INSIDE Car's constructor. It cannot be swapped.");
        System.out.println("When Car is destroyed, Engine is destroyed too.");

        com.lldprep.foundations.oop.composition.car.Car sedan =
            new com.lldprep.foundations.oop.composition.car.Car("Toyota Camry", "V6 Hybrid");
        sedan.start();
        sedan.stop();

        System.out.println();
        System.out.println("Key insight: Car does NOT accept Engine via constructor.");
        System.out.println("Engine is created with 'new Engine()' inside Car's constructor.");
        System.out.println("No setter, no swap — strict ownership. Relationship: Car ◆----> Engine");

        System.out.println("\n===== END COMPOSITION DEMO =====");
    }
}
