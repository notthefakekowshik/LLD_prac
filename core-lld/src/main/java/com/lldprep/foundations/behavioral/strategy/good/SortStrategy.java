package com.lldprep.foundations.behavioral.strategy.good;

/**
 * Strategy interface — defines the contract for all sorting algorithms.
 * The Sorter context depends only on this abstraction, never on concrete algorithms.
 */
public interface SortStrategy {
    /** Sorts a copy of the array and returns it. Never mutates the input. */
    int[] sort(int[] data);

    /** Human-readable name for logging/debugging. */
    String name();
}
