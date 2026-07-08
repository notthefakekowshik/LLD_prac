package com.lldprep.systems.minesweeper.model;

import com.lldprep.systems.minesweeper.exception.MinesweeperException;
import com.lldprep.systems.minesweeper.exception.OutOfBoundsException;
import com.lldprep.systems.minesweeper.policy.MinePlacementStrategy;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The grid and all board mechanics: lazy mine placement, adjacency counting, and flood-fill reveal.
 *
 * Mine placement is deferred until the first reveal so the {@link MinePlacementStrategy} can see
 * the player's opening click and (optionally) keep it safe. The board tracks how many safe cells
 * have been revealed so win-detection is O(1).
 */
public class Board {

    // 8-directional neighbour offsets.
    private static final int[] DR = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] DC = {-1, 0, 1, -1, 1, -1, 0, 1};

    private final int rows;
    private final int cols;
    private final int mineCount;
    private final Cell[][] grid;
    private final MinePlacementStrategy placementStrategy;

    private boolean minesPlaced = false;
    private int revealedSafeCells = 0;

    public Board(int rows, int cols, int mineCount, MinePlacementStrategy placementStrategy) {
        if (rows <= 0 || cols <= 0) {
            throw new MinesweeperException("Board dimensions must be positive");
        }
        if (mineCount <= 0 || mineCount >= rows * cols) {
            throw new MinesweeperException(
                "Mine count must be in (0, " + (rows * cols) + "); got " + mineCount);
        }
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.placementStrategy = placementStrategy;
        this.grid = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
    }

    /**
     * Reveals a cell, flood-filling outward through zero-adjacency cells.
     *
     * @return true if the revealed cell was a mine (the player loses)
     */
    public boolean reveal(int row, int col) {
        validateBounds(row, col);
        ensureMinesPlaced(row, col);

        Cell start = grid[row][col];
        if (!start.isHidden()) {
            return false; // flagged or already revealed → no-op
        }

        if (start.isMine()) {
            start.reveal();
            return true;
        }

        // BFS flood-fill: reveal the start cell, then cascade through any cell with zero
        // adjacent mines. Flagged cells are skipped (isHidden() excludes them).
        Deque<Cell> queue = new ArrayDeque<>();
        revealCell(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Cell cell = queue.poll();
            if (cell.getAdjacentMines() != 0) {
                continue; // boundary of the empty region
            }
            for (Cell neighbour : neighbours(cell)) {
                if (neighbour.isHidden() && !neighbour.isMine()) {
                    revealCell(neighbour);
                    queue.add(neighbour);
                }
            }
        }
        return false;
    }

    /** Toggles a flag. @return true if the flag state actually changed. */
    public boolean toggleFlag(int row, int col) {
        validateBounds(row, col);
        return grid[row][col].toggleFlag();
    }

    /** Win condition: every non-mine cell has been revealed. */
    public boolean allSafeCellsRevealed() {
        return revealedSafeCells == (rows * cols - mineCount);
    }

    /** Reveals every mine — used to render the final board after a loss. */
    public void revealAllMines() {
        for (Cell[] rowCells : grid) {
            for (Cell cell : rowCells) {
                if (cell.isMine()) cell.reveal();
            }
        }
    }

    public String render() {
        StringBuilder sb = new StringBuilder("    ");
        for (int c = 0; c < cols; c++) sb.append(c % 10).append(' ');
        sb.append('\n');
        for (int r = 0; r < rows; r++) {
            sb.append(String.format("%2d  ", r));
            for (int c = 0; c < cols; c++) {
                sb.append(grid[r][c].toDisplayChar()).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public int getRows()      { return rows; }
    public int getCols()      { return cols; }
    public int getMineCount() {
        return mineCount;
    }

    public Cell getCell(int row, int col) {
        validateBounds(row, col);
        return grid[row][col];
    }

    private void revealCell(Cell cell) {
        if (cell.reveal()) {
            revealedSafeCells++;
        }
    }

    private void ensureMinesPlaced(int safeRow, int safeCol) {
        if (minesPlaced) {
            return;
        }
        placementStrategy.placeMines(grid, mineCount, safeRow, safeCol);
        computeAdjacencies();
        minesPlaced = true;
    }

    private void computeAdjacencies() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isMine()) {
                    continue;
                }
                int count = 0;
                for (Cell n : neighbours(grid[r][c])) {
                    if (n.isMine()) {
                        count++;
                    }
                }
                grid[r][c].setAdjacentMines(count);
            }
        }
    }

    private Iterable<Cell> neighbours(Cell cell) {
        Deque<Cell> result = new ArrayDeque<>(8);
        for (int i = 0; i < DR.length; i++) {
            int nr = cell.getRow() + DR[i];
            int nc = cell.getCol() + DC[i];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                result.add(grid[nr][nc]);
            }
        }
        return result;
    }

    private void validateBounds(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new OutOfBoundsException(row, col, rows, cols);
        }
    }
}
