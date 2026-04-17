package com.lldprep.foundations.structural.flyweight.good;

/**
 * Context — stores only the extrinsic (unique-per-instance) state.
 *
 * <p>x, y are unique to each tree. The heavy intrinsic state (name, color, texture)
 * lives in the shared {@link TreeType} flyweight — NOT duplicated here.
 * With 1,000,000 trees, we only ever create as many {@link TreeType} objects as
 * there are distinct tree types (e.g., 3), regardless of tree count.
 */
public class Tree {

    private final int x;
    private final int y;
    private final TreeType type;   // shared flyweight reference — not a copy

    public Tree(int x, int y, TreeType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void render() {
        type.render(x, y);
    }
}
