package com.lldprep.systems.snakeandladder.model;

/**
 * A game token. Starts off-board at position 0; {@link #moveTo(int)} is the only way to change
 * position, so the game orchestrator stays the single authority over where a player sits.
 */
public class Player {

    private final String id;
    private final String name;
    private int position = 0; // 0 = off-board (not yet on cell 1)

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void moveTo(int position) {
        this.position = position;
    }

    public String getId()     { return id; }
    public String getName()   { return name; }
    public int getPosition()  { return position; }

    @Override
    public String toString() {
        return name + "@" + position;
    }
}
