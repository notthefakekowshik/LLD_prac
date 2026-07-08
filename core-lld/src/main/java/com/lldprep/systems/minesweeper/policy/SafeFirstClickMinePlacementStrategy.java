package com.lldprep.systems.minesweeper.policy;

import com.lldprep.systems.minesweeper.model.Cell;

import java.util.Random;

/**
 * Places mines at random but guarantees the player's first click — and its 8 neighbours —
 * are mine-free, so the opening move always triggers a flood-fill rather than an instant loss.
 * This is the modern Minesweeper convention.
 *
 * Why it slots in cleanly: the {@code safeRow/safeCol} parameters already exist on the strategy
 * interface, so this is a drop-in swap for {@link RandomMinePlacementStrategy} — zero board changes.
 */
public class SafeFirstClickMinePlacementStrategy implements MinePlacementStrategy {

    private final Random random;

    public SafeFirstClickMinePlacementStrategy() {
        this(new Random());
    }

    public SafeFirstClickMinePlacementStrategy(Random random) {
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
            if (isWithinSafeZone(r, c, safeRow, safeCol)) {
                continue;
            }
            Cell cell = grid[r][c];
            if (!cell.isMine()) {
                cell.placeMine();
                placed++;
            }
        }
    }

    private boolean isWithinSafeZone(int r, int c, int safeRow, int safeCol) {
        return Math.abs(r - safeRow) <= 1 && Math.abs(c - safeCol) <= 1;
    }
}
