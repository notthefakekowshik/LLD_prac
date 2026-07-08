package com.lldprep.systems.minesweeper.service;

import com.lldprep.systems.minesweeper.exception.GameOverException;
import com.lldprep.systems.minesweeper.model.Board;
import com.lldprep.systems.minesweeper.model.GameStatus;
import com.lldprep.systems.minesweeper.model.MoveResult;

/**
 * The game's single entry point — a Facade over the board: the player interacts only through
 * reveal/flag and reads status. Win/loss transitions and move validation live here.
 *
 * No interface: there is exactly one game engine and nothing swaps it, so a concrete class IS the
 * facade (Facade is a role, not a Java interface). The real extension seam is the injected
 * {@link Board} (and, transitively, its mine-placement strategy) via the constructor (DIP).
 */
public class MinesweeperGame {

    private final Board board;
    private GameStatus status = GameStatus.IN_PROGRESS;

    public MinesweeperGame(Board board) {
        this.board = board;
    }

    // CRITICAL SECTION — a game is one interactive session; moves must be serialized so a
    // reveal and its win/loss check apply atomically.
    // ponytail: single lock on the whole game — fine for one player; split per-region only if
    // a future mode ever needs concurrent moves on one board.
    /** Reveals a cell; may trigger a flood-fill, a loss (mine), or a win (last safe cell). */
    public synchronized MoveResult reveal(int row, int col) {
        requireInProgress();
        boolean hitMine = board.reveal(row, col);
        if (hitMine) {
            board.revealAllMines();
            status = GameStatus.LOST;
            return new MoveResult(status, "Boom! Hit a mine at (" + row + "," + col + ").");
        }
        if (board.allSafeCellsRevealed()) {
            status = GameStatus.WON;
            return new MoveResult(status, "All safe cells revealed — you win!");
        }
        return new MoveResult(status, "Revealed (" + row + "," + col + ").");
    }

    /** Flags or unflags a suspected mine. Cannot flag an already-revealed cell. */
    public synchronized MoveResult toggleFlag(int row, int col) {
        requireInProgress();
        boolean changed = board.toggleFlag(row, col);
        String note = changed
            ? "Toggled flag at (" + row + "," + col + ")."
            : "Cannot flag a revealed cell at (" + row + "," + col + ").";
        return new MoveResult(status, note);
    }

    public synchronized GameStatus getStatus() {
        return status;
    }

    /** Read-only access to the board for rendering. */
    public Board getBoard() {
        return board;
    }

    private void requireInProgress() {
        if (status.isOver()) {
            throw new GameOverException(status);
        }
    }
}
