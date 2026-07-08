package com.lldprep.systems.minesweeper.exception;

/** Base unchecked exception for all Minesweeper domain errors. */
public class MinesweeperException extends RuntimeException {
    public MinesweeperException(String message) {
        super(message);
    }
}
