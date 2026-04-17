package com.lldprep.foundations.structural.flyweight.bad;

import java.util.ArrayList;
import java.util.List;

/**
 * BAD: Every tree object stores ALL its data — including heavy shared state.
 *
 * With 1,000,000 trees, each storing name+color+texture (intrinsic) AND x+y (extrinsic),
 * memory scales linearly. The intrinsic fields (name, color, texture) are identical across
 * all trees of the same type — they are repeated in memory for every instance.
 *
 * Problem: name="Oak" + color="Green" + texture=(large byte[]) × 1,000,000 trees = OOM.
 */
public class FlyweightBad {

    public static void main(String[] args) {
        List<TreeBad> forest = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // Every object has its own copy of name/color/texture — duplicated 10 (or 1M) times
            forest.add(new TreeBad("Oak", "Green", "rough-bark-texture", i * 10, i * 5));
        }
        System.out.println("Forest size: " + forest.size() + " trees (each duplicates intrinsic state)");
    }
}

class TreeBad {
    private final String name;     // intrinsic — same for all Oaks, duplicated!
    private final String color;    // intrinsic — same for all Oaks, duplicated!
    private final String texture;  // intrinsic — same for all Oaks, duplicated!
    private final int x;           // extrinsic — unique per tree
    private final int y;           // extrinsic — unique per tree

    TreeBad(String name, String color, String texture, int x, int y) {
        this.name = name;
        this.color = color;
        this.texture = texture;
        this.x = x;
        this.y = y;
    }

    public void render() {
        System.out.printf("  Tree[%s/%s] at (%d,%d)%n", name, color, x, y);
    }
}
