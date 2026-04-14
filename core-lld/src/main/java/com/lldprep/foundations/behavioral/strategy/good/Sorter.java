package com.lldprep.foundations.behavioral.strategy.good;

/**
 * Context — uses a SortStrategy without knowing or caring which one.
 *
 * Key design choices:
 * 1. Strategy is injected via constructor (DIP).
 * 2. Strategy can be swapped at runtime via setStrategy().
 * 3. Context has zero knowledge of any concrete algorithm.
 */
public class Sorter {

    private SortStrategy strategy; // Why: depends on abstraction, not concretion

    public Sorter(SortStrategy strategy) {
        this.strategy = strategy;
    }

    /** Swap the algorithm at runtime — no need to create a new Sorter. */
    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }

    public int[] sort(int[] data) {
        System.out.println("Using strategy: " + strategy.name());
        return strategy.sort(data);
    }
}
