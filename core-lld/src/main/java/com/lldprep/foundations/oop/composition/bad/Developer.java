package com.lldprep.foundations.oop.composition.bad;

/**
 * BAD: Copy-pasted work logic from TechLead.
 *
 * <p>The work() method is identical to TechLead.work() — maintenance nightmare.
 * Any change to the coding workflow requires editing BOTH files (and Manager if it also codes).
 */
public class Developer {

    private final String name;

    public Developer(String name) {
        this.name = name;
    }

    // IDENTICAL to TechLead.work() — copy-pasted
    public void work() {
        System.out.println("[BAD] " + name + " is writing and reviewing code.");
    }

    // Developer doesn't manage, so we leave this out or add a no-op.
    // But what if they get promoted to Tech Lead? The class structure must change completely.
}
