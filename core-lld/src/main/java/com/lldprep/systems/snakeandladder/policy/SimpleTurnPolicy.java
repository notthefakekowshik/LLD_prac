package com.lldprep.systems.snakeandladder.policy;

/** Plain rules: every roll moves the token and passes to the next player. No bonus turns. */
public class SimpleTurnPolicy implements TurnPolicy {

    @Override
    public TurnDecision decide(int roll, int dieMaxValue, int consecutiveBonus) {
        return TurnDecision.moveAndPass();
    }
}
