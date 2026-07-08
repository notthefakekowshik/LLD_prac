package com.lldprep.systems.snakeandladder.policy;

/**
 * Classic rule: you must land EXACTLY on the final cell to win. A roll that would overshoot is
 * rejected — the player stays put and forfeits the turn.
 */
public class ExactLandingMovePolicy implements MovePolicy {

    @Override
    public int computeTarget(int current, int diceRoll, int boardSize) {
        int target = current + diceRoll;
        return target <= boardSize ? target : current; // overshoot → stay
    }
}
