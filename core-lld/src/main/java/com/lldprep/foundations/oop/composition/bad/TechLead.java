package com.lldprep.foundations.oop.composition.bad;

/**
 * BAD: Without composition, work and management logic are copy-pasted into every class.
 *
 * <p>Problem: Java does not support multiple inheritance of classes.
 * A TechLead cannot extend both Developer (for code logic) and Manager (for team leadership).
 *
 * <p>Consequence: work() and manage() logic must be COPY-PASTED here.
 * If coding standards change (e.g., add code review step), every copy must be updated.
 */
public class TechLead {

    private final String name;

    public TechLead(String name) {
        this.name = name;
    }

    // DUPLICATED work logic — copy-pasted from what would be a Developer class.
    // If coding workflow changes (add code review, pair programming), every copy must be updated.
    public void work() {
        System.out.println("[BAD] " + name + " is writing and reviewing code.");
    }

    // DUPLICATED management logic — copy-pasted from what would be a Manager class.
    // Same problem: if 1:1 cadence changes or mentorship process updates, every copy must change.
    public void manage() {
        System.out.println("[BAD] " + name + " is leading the engineering team, mentoring juniors.");
    }

    // What about a Developer who doesn't manage? You'd STILL copy work() into Developer
    // and add a stub/no-op for manage(), OR build a messy class hierarchy — neither is clean.
}
