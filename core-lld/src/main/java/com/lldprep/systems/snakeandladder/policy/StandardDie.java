package com.lldprep.systems.snakeandladder.policy;

import java.util.Random;

/**
 * A fair n-faced die (default 6). Pure randomizer — no game state. Accepts an injectable
 * {@link Random} so tests and demos can pin a seed for reproducible games.
 */
public class StandardDie implements Die {

    private final int faces;
    private final Random random;

    public StandardDie() {
        this(6, new Random());
    }

    public StandardDie(Random random) {
        this(6, random);
    }

    public StandardDie(int faces, Random random) {
        if (faces < 1) {
            throw new IllegalArgumentException("A die needs at least 1 face; got " + faces);
        }
        this.faces = faces;
        this.random = random;
    }

    @Override
    public int roll() {
        return random.nextInt(faces) + 1;
    }

    @Override
    public int maxValue() {
        return faces;
    }
}
