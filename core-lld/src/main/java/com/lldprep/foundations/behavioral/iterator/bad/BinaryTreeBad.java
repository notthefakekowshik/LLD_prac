package com.lldprep.foundations.behavioral.iterator.bad;

import java.util.ArrayList;
import java.util.List;

/**
 * BAD: Tree exposes its internal structure and forces callers to perform traversal themselves.
 *
 * Problems:
 * 1. Internal structure (Node, left, right) is exposed via getRoot() — breaks encapsulation.
 * 2. Every caller must implement their own traversal logic — logic is duplicated everywhere.
 * 3. Switching traversal strategy (in-order → pre-order) requires changing all call sites.
 * 4. Cannot use in a for-each loop; callers must know the tree's internal layout.
 */
public class BinaryTreeBad {

    public static class Node {
        public int value;
        public Node left, right;
        public Node(int value) { this.value = value; }
    }

    private Node root;

    public void insert(int value) {
        root = insert(root, value);
    }

    private Node insert(Node node, int value) {
        if (node == null) return new Node(value);
        if (value < node.value) node.left  = insert(node.left,  value);
        else                    node.right = insert(node.right, value);
        return node;
    }

    /** Caller must pull out root and write their own recursion — breaks encapsulation. */
    public Node getRoot() { return root; }

    /** Callers write this traversal themselves — duplicated at every call site. */
    public static List<Integer> inOrderExternal(Node node, List<Integer> result) {
        if (node == null) return result;
        inOrderExternal(node.left, result);
        result.add(node.value);
        inOrderExternal(node.right, result);
        return result;
    }
}
