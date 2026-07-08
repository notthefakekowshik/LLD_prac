package com.lldprep.systems.snakeandladder.model;

import com.lldprep.systems.snakeandladder.exception.InvalidBoardException;

/** A jump that always goes UP: the bottom (start) must sit below the top (end). */
public class Ladder extends Jump {

    public Ladder(int bottom, int top) {
        super(bottom, top);
        if (top <= bottom) {
            throw new InvalidBoardException(
                "Ladder top (" + top + ") must be above its bottom (" + bottom + ")");
        }
    }

    @Override
    public String type() {
        return "Ladder";
    }
}
