package com.lldprep.systems.snakeandladder.policy;

/**
 * Strategy for turn-taking rules — whether a roll grants an extra turn, forfeits the turn, or just
 * passes play on. This is a game rule, so it lives here rather than inside {@link Die}.
 *
 * Implementations are <b>pure functions</b>: the game owns the streak state and passes it in, so a
 * policy instance holds no per-game state and is safe to reuse and trivial to unit-test.
 */
public interface TurnPolicy {

    /**
     * @param roll              the value just rolled
     * @param dieMaxValue       the die's highest face (so rules key off "max roll", not a literal 6)
     * @param consecutiveBonus  the current player's bonus-roll streak BEFORE this roll
     * @return the decision for this turn
     */
    TurnDecision decide(int roll, int dieMaxValue, int consecutiveBonus);
}
