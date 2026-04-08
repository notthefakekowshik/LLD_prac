package com.kowshik.HEAPS;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A Min-Heap implementation from scratch, as would be expected in
 * a technical interview for a Java developer.
 *
 * This implementation uses an ArrayList as the backing store to handle
 * dynamic resizing. It stores integers (`int`) for simplicity.
 */
public class MinHeapFromScratch {

    // The ArrayList will store the elements of the heap in
    // level-order, starting from the root at index 0.
    private final List<Integer> heap;

    /**
     * Constructor to initialize an empty MinHeap.
     */
    public MinHeapFromScratch() {
        this.heap = new ArrayList<>();
    }

    // --- Core Public API ---

    /**
     * Returns the number of elements in the heap.
     * 
     * @return the size of the heap.
     *         Time Complexity: O(1)
     */
    public int size() {
        return heap.size();
    }

    /**
     * Checks if the heap is empty.
     * 
     * @return true if the heap is empty, false otherwise.
     *         Time Complexity: O(1)
     */
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Returns the minimum element (the root) without removing it.
     * 
     * @return the minimum element in the heap.
     * @throws NoSuchElementException if the heap is empty.
     *                                Time Complexity: O(1)
     */
    public int peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Heap is empty. Cannot peek.");
        }
        // The minimum element is always at the root (index 0).
        return heap.getFirst();
    }

    /**
     * Adds a new element to the heap, maintaining the heap property.
     * 
     * @param value the element to add.
     *              Time Complexity: O(log n)
     */
    public void add(int value) {
        // 1. Add the element to the very end of the list.
        // This maintains the "complete binary tree" shape.
        heap.add(value);

        // 2. "Bubble up" (or heapifyUp) the new element to its
        // correct position to restore the heap property.
        heapifyUp();
    }

    /**
     * Removes and returns the minimum element (the root).
     * 
     * @return the minimum element in the heap.
     * @throws NoSuchElementException if the heap is empty.
     *                                Time Complexity: O(log n)
     */
    public int poll() {
        if (isEmpty()) {
            throw new NoSuchElementException("Heap is empty. Cannot poll.");
        }

        // 1. The root (index 0) is the minimum value. Store it to return later.
        int minValue = heap.getFirst();

        // 2. Take the last element in the heap and move it to the root.
        // This maintains the "complete binary tree" shape.
        int lastElement = heap.getLast();
        heap.set(0, lastElement);

        // 3. Remove the last element (which we just moved).
        heap.removeLast();

        // 4. "Bubble down" (or heapifyDown) the new root to its
        // correct position to restore the heap property.
        // Only do this if the heap is not empty after the removal.
        if (!isEmpty()) {
            heapifyDown();
        }

        return minValue;
    }

    // --- Private Helper Methods ---

    /**
     * Restores the heap property by "bubbling up" the last element
     * (the one just added) to its correct position.
     */
    private void heapifyUp() {
        int index = heap.size() - 1; // Start at the last element

        // While the element has a parent and is smaller than its parent
        // (violating the min-heap property)
        while (index > 0 && heap.get(getParentIndex(index)) > heap.get(index)) {
            // Swap the element with its parent
            int parentIndex = getParentIndex(index);
            swap(parentIndex, index);
            // Move up to the parent's index to continue checking
            index = parentIndex;
        }
    }

    /**
     * Restores the heap property by "bubbling down" the root element
     * (or any element) to its correct position.
     */
    private void heapifyDown() {
        int index = 0; // Start at the root
        int size = heap.size();

        // We only need to check for a left child, because if there's no
        // left child, there can be no right child (complete tree property).
        while (getLeftChildIndex(index) < size) {
            int leftChildIndex = getLeftChildIndex(index);
            int rightChildIndex = getRightChildIndex(index);
            int smallerChildIndex = leftChildIndex;

            // Check if a right child exists AND is even smaller than the left child
            if (rightChildIndex < size && heap.get(rightChildIndex) < heap.get(leftChildIndex)) {
                smallerChildIndex = rightChildIndex;
            }

            // If the current node (parent) is smaller than its smallest child,
            // the heap property is satisfied, and we can stop.
            if (heap.get(index) < heap.get(smallerChildIndex)) {
                break;
            } else {
                // Otherwise, swap the parent with its smaller child
                swap(index, smallerChildIndex);
            }
            // Move down to the smaller child's index to continue checking
            index = smallerChildIndex;
        }
    }

    // --- Index & Value Helper Utilities ---

    /**
     * Swaps two elements in the heap.
     */
    private void swap(int index1, int index2) {
        int temp = heap.get(index1);
        heap.set(index1, heap.get(index2));
        heap.set(index2, temp);
    }

    // These formulas are standard for a 0-indexed array representation.
    private int getLeftChildIndex(int parentIndex) {
        return 2 * parentIndex + 1;
    }

    private int getRightChildIndex(int parentIndex) {
        return 2 * parentIndex + 2;
    }

    private int getParentIndex(int childIndex) {
        return (childIndex - 1) / 2;
    }

    /**
     * A simple main method to test the MinHeap implementation.
     */
    public static void main(String[] args) {
        System.out.println("--- MinHeap Test ---");
        MinHeapFromScratch minHeapFromScratch = new MinHeapFromScratch();

        System.out.println("Adding 10...");
        minHeapFromScratch.add(10);
        System.out.println("Adding 4...");
        minHeapFromScratch.add(4);
        System.out.println("Adding 15...");
        minHeapFromScratch.add(15);
        System.out.println("Adding 20...");
        minHeapFromScratch.add(20);
        System.out.println("Adding 2...");
        minHeapFromScratch.add(2);

        System.out.println("\nHeap internal array: " + minHeapFromScratch.heap);
        System.out.println("Current min (peek): " + minHeapFromScratch.peek()); // Should be 2
        System.out.println("Heap size: " + minHeapFromScratch.size()); // Should be 5

        System.out.println("\nPolling elements:");
        System.out.println("Polled: " + minHeapFromScratch.poll()); // Should be 2
        System.out.println("Current min (peek): " + minHeapFromScratch.peek()); // Should be 4
        System.out.println("Heap internal array: " + minHeapFromScratch.heap);

        System.out.println("\nPolled: " + minHeapFromScratch.poll()); // Should be 4
        System.out.println("Current min (peek): " + minHeapFromScratch.peek()); // Should be 10
        System.out.println("Heap internal array: " + minHeapFromScratch.heap);

        System.out.println("\nPolled: " + minHeapFromScratch.poll()); // Should be 10
        System.out.println("Polled: " + minHeapFromScratch.poll()); // Should be 15
        System.out.println("Polled: " + minHeapFromScratch.poll()); // Should be 20

        System.out.println("\nIs heap empty? " + minHeapFromScratch.isEmpty()); // Should be true
        System.out.println("Heap size: " + minHeapFromScratch.size()); // Should be 0
    }
}
