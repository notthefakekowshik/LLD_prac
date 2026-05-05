// AGGREGATION: Player is an independent object.
// Players exist before and after any Team is formed.
// A Player can move between teams — no lifecycle dependency on Team.
package com.lldprep.foundations.oop.aggregation;

public class Player {

    private final String name;

    public Player(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void play() {
        System.out.println(name + " is playing.");
    }
}
