package com.lldprep.systems.minesweeper.model;

/** Terminal-aware status of a game. Only IN_PROGRESS accepts moves. */
public enum GameStatus {
    IN_PROGRESS,
    WON,
    LOST;

    public boolean isOver() {
        return this != IN_PROGRESS;
    }
}
