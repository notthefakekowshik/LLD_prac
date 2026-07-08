package com.lldprep.systems.snakeandladder.policy;

/**
 * Classic six-rule: rolling the die's max face grants an extra turn; three such rolls in a row
 * forfeit the turn (no move) and pass play on. Keyed off {@code dieMaxValue}, so it works for any
 * die size without hard-coding "6".
 */
public class ExtraTurnOnMaxRollPolicy implements TurnPolicy {

    private static final int MAX_CONSECUTIVE_BONUS = 3;

    @Override
    public TurnDecision decide(int roll, int dieMaxValue, int consecutiveBonus) {
        if (roll != dieMaxValue) {
            return TurnDecision.moveAndPass();
        }
        int streak = consecutiveBonus + 1;
        if (streak >= MAX_CONSECUTIVE_BONUS) {
            return TurnDecision.forfeit();
        }
        return TurnDecision.moveAndReplay(streak);
    }
}
