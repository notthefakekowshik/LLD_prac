package com.lldprep.systems.snakeandladder.policy;

/**
 * Strategy for producing a roll.
 *
 * Why (Strategy): the game depends on this abstraction, so a single die, a sum-of-two-dice, or a
 * loaded die for tests all drop in without touching the game loop (OCP + DIP).
 *
 * A {@code Die} is a pure randomizer — it models "give me a face value" and nothing else. Turn
 * rules (extra turns, forfeits) live in {@link TurnPolicy}, not here.
 */
public interface Die {

    /** @return the rolled value, between 1 and {@link #maxValue()} inclusive. */
    int roll();

    /** @return the highest face value this die can produce (lets rules avoid hard-coding "6"). */
    int maxValue();
}
