package com.lldprep.foundations.structural.flyweight.good;

/**
 * Flyweight — stores only intrinsic (shared, immutable) state.
 *
 * <p>Intrinsic state: name, color, texture — identical for all trees of the same type.
 * This object is created once per type and shared across thousands of tree instances.
 * Extrinsic state (x, y position) is passed in at render time — not stored here.
 */
public class TreeType {

    private final String name;
    private final String color;
    private final String texture;   // In a real renderer this would be a large Texture object

    public TreeType(String name, String color, String texture) {
        this.name = name;
        this.color = color;
        this.texture = texture;
    }

    /** Extrinsic state (x, y) is passed in — NOT stored on the flyweight. */
    public void render(int x, int y) {
        System.out.printf("  [%s|%s|%s] at (%d,%d)%n", name, color, texture, x, y);
    }

    public String getName() {
        return name;
    }
}
