package com.lldprep.systems.snakeandladder.model;

import com.lldprep.systems.snakeandladder.exception.InvalidBoardException;

/** A jump that always goes DOWN: the head (start) must sit above the tail (end). */
public class Snake extends Jump {

    public Snake(int head, int tail) {
        super(head, tail);
        // Validated after super() so no overridable method is called from the constructor.
        if (tail >= head) {
            throw new InvalidBoardException(
                "Snake head (" + head + ") must be above its tail (" + tail + ")");
        }
    }

    @Override
    public String type() {
        return "Snake";
    }
}
