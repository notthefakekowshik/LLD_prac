package com.lldprep.systems.snakeandladder.policy;

/**
 * Strategy for how a roll translates into a landing cell — specifically, what happens when the roll
 * would overshoot the final cell. The two well-known variants (exact-landing vs bounce-back) are
 * why this is a Strategy rather than hard-coded arithmetic.
 *
 * Returns the landing cell BEFORE any snake/ladder is applied; the board resolves jumps afterwards.
 */
public interface MovePolicy {
    /**
     * @param current  the player's current cell (0 = off-board)
     * @param diceRoll the rolled value
     * @param boardSize the winning cell
     * @return the cell the player lands on (may equal {@code current} if the move is rejected)
     */
    int computeTarget(int current, int diceRoll, int boardSize);
}
