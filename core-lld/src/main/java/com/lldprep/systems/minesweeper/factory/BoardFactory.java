package com.lldprep.systems.minesweeper.factory;

import com.lldprep.systems.minesweeper.model.Board;
import com.lldprep.systems.minesweeper.model.Difficulty;
import com.lldprep.systems.minesweeper.policy.MinePlacementStrategy;

/**
 * Builds boards from a {@link Difficulty} preset or explicit dimensions.
 *
 * Why (Factory): keeps the mapping from preset → (rows, cols, mines) in one place so callers
 * never restate those magic numbers, and lets the placement strategy be injected (DIP).
 */
public final class BoardFactory {

    private BoardFactory() {}

    public static Board fromDifficulty(Difficulty difficulty, MinePlacementStrategy strategy) {
        return new Board(difficulty.getRows(), difficulty.getCols(), difficulty.getMines(), strategy);
    }

    public static Board custom(int rows, int cols, int mines, MinePlacementStrategy strategy) {
        return new Board(rows, cols, mines, strategy);
    }
}
