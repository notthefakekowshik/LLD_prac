package com.lldprep.systems.snakeandladder.factory;

import com.lldprep.systems.snakeandladder.model.Board;
import com.lldprep.systems.snakeandladder.model.Jump;
import com.lldprep.systems.snakeandladder.model.Ladder;
import com.lldprep.systems.snakeandladder.model.Snake;

import java.util.List;

/**
 * Builds boards. Keeps the canonical 100-cell layout in one place so callers never restate the
 * classic snake/ladder coordinates; also exposes a custom builder for arbitrary boards.
 */
public final class BoardFactory {

    private BoardFactory() {}

    /** The classic 10x10 board with the standard 9 ladders and 10 snakes. */
    public static Board standard() {
        List<Jump> jumps = List.of(
            new Ladder(1, 38),  new Ladder(4, 14),  new Ladder(9, 31),
            new Ladder(21, 42), new Ladder(28, 84), new Ladder(51, 67),
            new Ladder(71, 91), new Ladder(80, 100),
            new Snake(17, 7),   new Snake(54, 34),  new Snake(62, 19),
            new Snake(64, 60),  new Snake(87, 24),  new Snake(93, 73),
            new Snake(95, 75),  new Snake(98, 79)
        );
        return new Board(100, jumps);
    }

    public static Board custom(int size, List<Jump> jumps) {
        return new Board(size, jumps);
    }
}
