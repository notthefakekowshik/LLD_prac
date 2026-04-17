package com.lldprep.foundations.structural.flyweight.good;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Flyweight Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * When you have a huge number of similar objects, storing ALL their data per object
 * wastes memory. Many objects share the same "type" data. Flyweight splits state into:
 * <ul>
 *   <li><b>Intrinsic</b> — shared, immutable, stored once in the flyweight (name, color, texture).</li>
 *   <li><b>Extrinsic</b> — unique per instance, passed in at use time, NOT stored (x, y position).</li>
 * </ul>
 *
 * <p><b>How it works:</b><br>
 * - {@code TreeType} — Flyweight; holds intrinsic state. One instance per distinct type.<br>
 * - {@code TreeTypeFactory} — Cache; ensures flyweights are reused, never duplicated.<br>
 * - {@code Tree} — Context; holds extrinsic state (x, y) + a reference to the shared flyweight.<br>
 * - 1,000,000 {@code Tree} objects → only 3 {@code TreeType} objects in memory.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>You have a very large number of objects consuming too much memory.</li>
 *   <li>Most object state can be made extrinsic (passed in, not stored).</li>
 *   <li>Many objects share identical intrinsic state.</li>
 * </ul>
 *
 * <p><b>Real-world examples:</b><br>
 * Java's {@code String} pool, {@code Integer.valueOf()} cache (-128 to 127),
 * character glyphs in a text editor, game particle systems.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Forest with 1,000 trees but only 3 TreeType flyweights</li>
 *   <li>Memory comparison — objects vs flyweights created</li>
 * </ol>
 */
public class FlyweightDemo {

    public static void main(String[] args) {
        demo1_ForestRenderer();
        demo2_MemoryImpact();
    }

    // -------------------------------------------------------------------------

    private static void demo1_ForestRenderer() {
        section("Demo 1: Render 12 trees — only 3 TreeType flyweights created");

        List<Tree> forest = new ArrayList<>();
        Random rnd = new Random(42);

        String[][] types = {
            {"Oak",   "DarkGreen", "rough-bark"},
            {"Pine",  "Green",     "needle-bark"},
            {"Birch", "White",     "smooth-bark"}
        };

        for (int i = 0; i < 12; i++) {
            String[] t = types[i % 3];
            TreeType type = TreeTypeFactory.getTreeType(t[0], t[1], t[2]);
            forest.add(new Tree(rnd.nextInt(100), rnd.nextInt(100), type));
        }

        System.out.printf("%n  Rendering %d trees:%n", forest.size());
        forest.forEach(Tree::render);

        System.out.printf("%n  Trees in forest  : %d%n", forest.size());
        System.out.printf("  TreeType flyweights : %d (shared across all trees)%n",
                TreeTypeFactory.getCachedTypeCount());
    }

    private static void demo2_MemoryImpact() {
        section("Demo 2: Scale to 1,000 trees — flyweight count stays at 3");

        List<Tree> forest = new ArrayList<>();
        Random rnd = new Random(0);
        String[][] types = {
            {"Oak",   "DarkGreen", "rough-bark"},
            {"Pine",  "Green",     "needle-bark"},
            {"Birch", "White",     "smooth-bark"}
        };

        for (int i = 0; i < 1000; i++) {
            String[] t = types[i % 3];
            TreeType type = TreeTypeFactory.getTreeType(t[0], t[1], t[2]);
            forest.add(new Tree(rnd.nextInt(500), rnd.nextInt(500), type));
        }

        System.out.printf("  Trees in forest     : %d%n", forest.size());
        System.out.printf("  TreeType flyweights : %d%n", TreeTypeFactory.getCachedTypeCount());
        System.out.println("  Without flyweight   : 1000 objects each with name+color+texture");
        System.out.println("  With flyweight      : 1000 lightweight Tree + 3 shared TreeType objects");
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
