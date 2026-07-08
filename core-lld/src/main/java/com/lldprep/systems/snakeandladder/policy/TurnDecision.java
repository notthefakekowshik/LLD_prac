package com.lldprep.systems.snakeandladder.policy;

/**
 * The outcome a {@link TurnPolicy} hands back for one roll — an explicit value, not a magic number.
 *
 * @param applyMove            whether the player's token moves this turn (false = forfeited turn)
 * @param grantsExtraTurn      whether the same player rolls again instead of passing
 * @param nextConsecutiveBonus the running "bonus roll" streak to carry into the next roll; the game
 *                             stores this verbatim, so it never needs to know what counts as a bonus
 */
public record TurnDecision(boolean applyMove, boolean grantsExtraTurn, int nextConsecutiveBonus) {

    /** Normal turn: move the token, then pass to the next player. Streak resets. */
    public static TurnDecision moveAndPass() {
        return new TurnDecision(true, false, 0);
    }

    /** Bonus roll: move the token and let the same player roll again. Streak carries forward. */
    public static TurnDecision moveAndReplay(int nextConsecutiveBonus) {
        return new TurnDecision(true, true, nextConsecutiveBonus);
    }

    /** Forfeit: no move, pass to the next player. Streak resets. */
    public static TurnDecision forfeit() {
        return new TurnDecision(false, false, 0);
    }
}
