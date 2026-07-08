package com.lldprep.systems.minesweeper.exception;

import com.lldprep.systems.minesweeper.model.GameStatus;

/** Thrown when a move is attempted after the game has already been won or lost. */
public class GameOverException extends MinesweeperException {
    public GameOverException(GameStatus status) {
        super("Game is already over (" + status + ") — no further moves allowed");
    }
}
