package com.lldprep.systems.minesweeper.exception;

/** Thrown when a move targets coordinates outside the board — a programming/input error. */
public class OutOfBoundsException extends MinesweeperException {
    public OutOfBoundsException(int row, int col, int rows, int cols) {
        super("Cell (" + row + "," + col + ") is outside the " + rows + "x" + cols + " board");
    }
}
