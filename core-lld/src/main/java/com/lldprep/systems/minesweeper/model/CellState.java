package com.lldprep.systems.minesweeper.model;

/** Lifecycle of a single cell. A cell is exactly one of these at any time. */
public enum CellState {
    HIDDEN,    // covered, not yet revealed
    REVEALED,  // uncovered — shows a mine or an adjacent-mine count
    FLAGGED    // marked by the player as a suspected mine; cannot be revealed
}
