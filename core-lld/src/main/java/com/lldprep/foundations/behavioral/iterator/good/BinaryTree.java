package com.lldprep.foundations.behavioral.iterator.good;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Binary Search Tree that exposes multiple traversal strategies via Iterator.
 *
 * Internal structure (Node, root) is completely hidden from callers.
 * Callers just pick an iterator type — they never see left/right pointers.
 *
 * Implements Iterable<Integer> so default for-each uses in-order traversal.
 */
public class BinaryTree implements Iterable<Integer> {

    private Node root;

    // -------------------------------------------------------------------------
    // Node — private inner class, never exposed
    // -------------------------------------------------------------------------
    private static class Node {
        int value;
        Node left, right;
        Node(int value) { this.value = value; }
    }

    // -------------------------------------------------------------------------
    // BST operations
    // -------------------------------------------------------------------------
    public void insert(int value) {
        root = insert(root, value);
    }

    private Node insert(Node node, int value) {
        if (node == null) return new Node(value);
        if (value < node.value) node.left  = insert(node.left,  value);
        else                    node.right = insert(node.right, value);
        return node;
    }

    // -------------------------------------------------------------------------
    // Factory methods — callers pick the traversal, never see internals
    // -------------------------------------------------------------------------
    public TreeIterator<Integer> inOrderIterator()   { return new InOrderIterator(root); }
    public TreeIterator<Integer> preOrderIterator()  { return new PreOrderIterator(root); }
    public TreeIterator<Integer> postOrderIterator() { return new PostOrderIterator(root); }

    /** Default for-each = in-order (sorted for BST). */
    @Override
    public Iterator<Integer> iterator() { return inOrderIterator(); }

    // =========================================================================
    // In-Order Iterator  (left → root → right) — produces sorted output in BST
    // =========================================================================
    private static class InOrderIterator implements TreeIterator<Integer> {

        private final Deque<Node> stack = new ArrayDeque<>();

        InOrderIterator(Node root) { pushLeft(root); }

        private void pushLeft(Node node) {
            while (node != null) { stack.push(node); node = node.left; }
        }

        @Override
        public boolean hasNext() { return !stack.isEmpty(); }

        @Override
        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();
            Node node = stack.pop();
            pushLeft(node.right);
            return node.value;
        }
    }

    // =========================================================================
    // Pre-Order Iterator  (root → left → right)
    // =========================================================================
    private static class PreOrderIterator implements TreeIterator<Integer> {

        private final Deque<Node> stack = new ArrayDeque<>();

        PreOrderIterator(Node root) { if (root != null) stack.push(root); }

        @Override
        public boolean hasNext() { return !stack.isEmpty(); }

        @Override
        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();
            Node node = stack.pop();
            if (node.right != null) stack.push(node.right); // right first so left is processed first
            if (node.left  != null) stack.push(node.left);
            return node.value;
        }
    }

    // =========================================================================
    // Post-Order Iterator  (left → right → root) — uses two stacks
    // =========================================================================
    private static class PostOrderIterator implements TreeIterator<Integer> {

        private final List<Integer> order = new ArrayList<>();
        private int index = 0;

        PostOrderIterator(Node root) {
            if (root == null) return;
            Deque<Node> stack = new ArrayDeque<>();
            stack.push(root);
            while (!stack.isEmpty()) {
                Node node = stack.pop();
                order.add(0, node.value); // prepend = reverse
                if (node.left  != null) stack.push(node.left);
                if (node.right != null) stack.push(node.right);
            }
        }

        @Override
        public boolean hasNext() { return index < order.size(); }

        @Override
        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();
            return order.get(index++);
        }
    }
}
