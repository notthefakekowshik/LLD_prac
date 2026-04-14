package com.lldprep.foundations.behavioral.iterator.good;

import java.util.ArrayList;
import java.util.List;

/**
 * Iterator Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * You need to traverse a collection without exposing its internal structure.
 * Without Iterator, callers must know how the collection is built (left/right pointers,
 * array indices, hash buckets) — tight coupling that breaks when the internals change.
 *
 * <p><b>How it works:</b><br>
 * - {@code BinaryTree} keeps its {@code Node} class private — callers never see it.<br>
 * - It exposes factory methods ({@code inOrderIterator()}, etc.) that return {@code TreeIterator}.<br>
 * - Implementing {@code Iterable<Integer>} lets the tree work in for-each loops directly.<br>
 * - Each iterator holds its own traversal state — multiple iterators can run concurrently.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>You're building a custom collection and want to hide its internal structure.</li>
 *   <li>You need multiple traversal strategies for the same collection (BFS, DFS, in-order...).</li>
 *   <li>You want callers to use for-each syntax without knowing the data structure.</li>
 * </ul>
 *
 * <p><b>Java note:</b><br>
 * For built-in collections ({@code ArrayList}, {@code HashMap}), don't reinvent Iterator —
 * they already implement {@code Iterable}. Use this pattern only for <b>custom</b> data structures.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>In-order traversal (sorted output for BST)</li>
 *   <li>Pre-order traversal</li>
 *   <li>Post-order traversal</li>
 *   <li>Native for-each via Iterable</li>
 *   <li>Multiple iterators on the same tree simultaneously</li>
 * </ol>
 */
public class IteratorDemo {

    public static void main(String[] args) {
        BinaryTree tree = buildTree(5, 3, 7, 1, 4, 6, 8);

        demo1_AllTraversals(tree);
        demo2_ForEachLoop(tree);
        demo3_MultipleIteratorsSimultaneously(tree);
    }

    // -------------------------------------------------------------------------

    private static void demo1_AllTraversals(BinaryTree tree) {
        section("Demo 1: Three traversal strategies on the same tree");

        System.out.print("  In-Order   (L→Root→R): ");
        printIterator(tree.inOrderIterator());   // 1 3 4 5 6 7 8 — sorted

        System.out.print("  Pre-Order  (Root→L→R): ");
        printIterator(tree.preOrderIterator());  // 5 3 1 4 7 6 8

        System.out.print("  Post-Order (L→R→Root): ");
        printIterator(tree.postOrderIterator()); // 1 4 3 6 8 7 5
    }

    private static void demo2_ForEachLoop(BinaryTree tree) {
        section("Demo 2: Native for-each loop (uses in-order via Iterable)");

        System.out.print("  for-each: ");
        for (int value : tree) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

    private static void demo3_MultipleIteratorsSimultaneously(BinaryTree tree) {
        section("Demo 3: Two iterators running at the same time — independent state");

        TreeIterator<Integer> it1 = tree.inOrderIterator();
        TreeIterator<Integer> it2 = tree.preOrderIterator();

        List<String> pairs = new ArrayList<>();
        while (it1.hasNext() && it2.hasNext()) {
            pairs.add("(" + it1.next() + "," + it2.next() + ")");
        }
        System.out.println("  inOrder,preOrder pairs: " + pairs);
    }

    // -------------------------------------------------------------------------

    private static BinaryTree buildTree(int... values) {
        BinaryTree tree = new BinaryTree();
        for (int v : values) tree.insert(v);
        return tree;
    }

    private static void printIterator(TreeIterator<Integer> it) {
        while (it.hasNext()) System.out.print(it.next() + " ");
        System.out.println();
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
