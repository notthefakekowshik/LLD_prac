package com.lldprep.systems.minesweeper.policy;

import com.lldprep.systems.minesweeper.model.Cell;

import java.util.Random;

/**
 * Places mines uniformly at random. Ignores the safe cell — a first click can hit a mine.
 * Accepts an injectable {@link Random} so tests can pin a seed for deterministic layouts.
 */
public class RandomMinePlacementStrategy implements MinePlacementStrategy {

    private final Random random;

    public RandomMinePlacementStrategy() {
        this(new Random());
    }

    public RandomMinePlacementStrategy(Random random) {
        this.random = random;
    }

    @Override
    public void placeMines(Cell[][] grid, int mineCount, int safeRow, int safeCol) {
        int rows = grid.length;
        int cols = grid[0].length;
        int placed = 0;
        while (placed < mineCount) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            Cell cell = grid[r][c];
            if (!cell.isMine()) {
                cell.placeMine();
                placed++;
            }
        }
    }
}
