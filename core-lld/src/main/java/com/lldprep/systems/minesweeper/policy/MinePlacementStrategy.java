package com.lldprep.systems.minesweeper.policy;

import com.lldprep.systems.minesweeper.model.Cell;

/**
 * Strategy for scattering mines across the grid.
 *
 * Why (Strategy pattern): placement policy varies independently of the board — plain random,
 * first-click-safe, or a fixed seeded layout for tests — so the board depends on this abstraction
 * and new policies are added without touching Board (OCP + DIP).
 *
 * Placement is invoked lazily on the FIRST reveal, so implementations receive the safe cell the
 * player just clicked and may choose to keep it (and optionally its neighbours) mine-free.
 */
public interface MinePlacementStrategy {

    /**
     * Marks exactly {@code mineCount} cells in {@code grid} as mines.
     *
     * @param grid      the full board grid, all cells initially mine-free
     * @param mineCount number of mines to place
     * @param safeRow   row of the player's first click
     * @param safeCol   column of the player's first click
     */
    void placeMines(Cell[][] grid, int mineCount, int safeRow, int safeCol);
}
