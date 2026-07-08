package com.lldprep.systems.snakeandladder.model;

/** Lifecycle of a game. Only IN_PROGRESS accepts turns. */
public enum GameStatus {
    IN_PROGRESS,
    FINISHED;

    public boolean isOver() {
        return this == FINISHED;
    }
}
