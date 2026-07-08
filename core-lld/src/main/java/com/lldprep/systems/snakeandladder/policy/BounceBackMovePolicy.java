package com.lldprep.systems.snakeandladder.policy;

/**
 * Alternative rule: a roll that overshoots the final cell "bounces back" by the excess.
 * e.g. on a size-100 board, at 98 a roll of 5 → 100 then bounces to 97.
 */
public class BounceBackMovePolicy implements MovePolicy {

    @Override
    public int computeTarget(int current, int diceRoll, int boardSize) {
        int target = current + diceRoll;
        if (target <= boardSize) {
            return target;
        }
        return boardSize - (target - boardSize); // reflect the overshoot

    }
}
