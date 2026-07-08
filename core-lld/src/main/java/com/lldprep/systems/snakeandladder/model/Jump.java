package com.lldprep.systems.snakeandladder.model;

import com.lldprep.systems.snakeandladder.exception.InvalidBoardException;

/**
 * A teleport on the board from {@code start} to {@code end}. Snakes and Ladders are the same
 * mechanic — a jump — differing only in direction, so both are modelled as one abstraction.
 *
 * Why (composition over instanceof): the {@code Board} looks up a Jump by its start cell and reads
 * {@code getEnd()} — it never asks "is this a snake or a ladder?". Adding a new jump kind (e.g. a
 * teleporter that wraps around) needs no change to the board or the game loop.
 */
public abstract class Jump {

    private final int start;
    private final int end;

    protected Jump(int start, int end) {
        if (start == end) {
            throw new InvalidBoardException("Jump start and end cannot be the same cell: " + start);
        }
        this.start = start;
        this.end = end;
    }

    public int getStart() { return start; }
    public int getEnd()   { return end; }

    /** Human-readable kind, e.g. "Snake" / "Ladder". */
    public abstract String type();

    @Override
    public String toString() {
        return type() + "[" + start + "->" + end + "]";
    }
}
