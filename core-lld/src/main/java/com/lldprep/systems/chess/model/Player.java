package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;

import java.util.Objects;

public class Player {
    private final String id;
    private final String name;
    private final Color color;

    public Player(String id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Player player)) return false;
        return id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + color + ")";
    }
}
