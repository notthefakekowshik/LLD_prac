// Without composition, we duplicate swim/fly logic or create impossible inheritance.
// Java does not support multiple inheritance of classes, so a FlyingFish cannot extend
// both Fish and Bird. The only option is to COPY-PASTE the logic directly into this class.
// Consequence: if swim logic changes, every copy must be updated — fragile and error-prone.
package com.lldprep.foundations.oop.composition.bad;

public class FlyingFish {

    private final String name;

    public FlyingFish(String name) {
        this.name = name;
    }

    // DUPLICATED swim logic — copy-pasted from what would be a Fish class.
    // If the swim algorithm changes (e.g., add depth tracking), every copy must be found and updated.
    public void swim() {
        System.out.println("[BAD] " + name + " moves fins and propels forward through water.");
    }

    // DUPLICATED fly logic — copy-pasted from what would be a Bird class.
    // Same problem: any change to fly logic must be applied everywhere it is copied.
    public void fly() {
        System.out.println("[BAD] " + name + " catches air current and glides.");
    }

    // What about a plain Tuna that cannot fly? You'd STILL copy swim() into Tuna
    // and add a stub/comment for fly(), OR build a messy class hierarchy — neither is clean.
}
