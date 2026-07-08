package com.lldprep.systems.minesweeper.model;

/**
 * A single square on the board. Rich model — it owns its own reveal/flag transitions
 * rather than exposing raw setters, so illegal state changes are impossible from outside
 * (e.g. you cannot reveal a flagged cell, or flag a revealed one).
 */
public class Cell {

    private final int row;
    private final int col;
    private boolean mine;
    private int adjacentMines;
    private CellState state = CellState.HIDDEN;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** Reveals a HIDDEN cell. No-op (returns false) if flagged or already revealed. */
    public boolean reveal() {
        if (state != CellState.HIDDEN) return false;
        state = CellState.REVEALED;
        return true;
    }

    /** Toggles a flag on a HIDDEN/FLAGGED cell. No-op (returns false) if already revealed. */
    public boolean toggleFlag() {
        if (state == CellState.REVEALED) return false;
        state = (state == CellState.FLAGGED) ? CellState.HIDDEN : CellState.FLAGGED;
        return true;
    }

    public void placeMine()        { this.mine = true; }
    public void setAdjacentMines(int count) { this.adjacentMines = count; }

    public boolean isMine()        { return mine; }
    public int getAdjacentMines()  { return adjacentMines; }
    public boolean isHidden()      { return state == CellState.HIDDEN; }
    public boolean isRevealed()    { return state == CellState.REVEALED; }
    public boolean isFlagged()     { return state == CellState.FLAGGED; }
    public CellState getState()    { return state; }
    public int getRow()            { return row; }
    public int getCol()            { return col; }

    /** Single-character render used by the board display. */
    public char toDisplayChar() {
        if (state == CellState.FLAGGED) return 'F';
        if (state == CellState.HIDDEN)  return '.';
        if (mine)                       return '*';
        return adjacentMines == 0 ? '_' : (char) ('0' + adjacentMines);
    }
}
