package com.lldprep.systems.snakeandladder.model;

/**
 * Immutable record of one turn: who played, what they rolled, where they moved from/to, whether a
 * snake/ladder was taken, and the resulting game status.
 */
public record TurnResult(
        String playerName,
        int diceRoll,
        int fromPosition,
        int toPosition,
        boolean tookJump,
        GameStatus status) {

    /** True if the roll was rejected (e.g. exact-landing overshoot left the player in place). */
    public boolean stayedPut() {
        return fromPosition == toPosition;
    }
}
