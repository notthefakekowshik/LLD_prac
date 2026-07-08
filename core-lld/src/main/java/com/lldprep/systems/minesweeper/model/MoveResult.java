package com.lldprep.systems.minesweeper.model;

/** Immutable outcome of a single move — the resulting game status and a human-readable note. */
public record MoveResult(GameStatus status, String message) {
}
