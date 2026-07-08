package com.lldprep.systems.snakeandladder.model;

import com.lldprep.systems.snakeandladder.exception.InvalidBoardException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The board: {@code size} cells (win cell = size) and a set of jumps keyed by their start cell for
 * O(1) destination lookup. All setup rules are enforced once, at construction, so the game loop can
 * trust the board is well-formed.
 */
public class Board {

    private final int size;
    private final Map<Integer, Jump> jumpsByStart = new HashMap<>();

    public Board(int size, List<Jump> jumps) {
        if (size < 2) {
            throw new InvalidBoardException("Board size must be at least 2; got " + size);
        }
        this.size = size;
        indexAndValidate(jumps);
    }

    /** Resolves the final cell after applying any jump that starts at {@code position}. */
    public int getDestination(int position) {
        Jump jump = jumpsByStart.get(position);
        return jump != null ? jump.getEnd() : position;
    }

    public boolean hasJumpAt(int position) {
        return jumpsByStart.containsKey(position);
    }

    public int getSize() {
        return size;
    }

    public Collection<Jump> getJumps() {
        return jumpsByStart.values();
    }

    private void indexAndValidate(List<Jump> jumps) {
        for (Jump jump : jumps) {
            requireInBounds(jump.getStart(), "start", jump);
            requireInBounds(jump.getEnd(), "end", jump);
            if (jump.getStart() == size) {
                throw new InvalidBoardException("Jump cannot start on the winning cell: " + jump);
            }
            if (jumpsByStart.containsKey(jump.getStart())) {
                throw new InvalidBoardException("Two jumps share start cell " + jump.getStart());
            }
            jumpsByStart.put(jump.getStart(), jump);
        }
        // No chaining: a jump must not end where another jump begins (avoids compound teleports).
        for (Jump jump : jumpsByStart.values()) {
            if (jumpsByStart.containsKey(jump.getEnd())) {
                throw new InvalidBoardException(
                    "Jump " + jump + " ends on the start of another jump — chaining not allowed");
            }
        }
    }

    private void requireInBounds(int cell, String which, Jump jump) {
        if (cell < 1 || cell > size) {
            throw new InvalidBoardException(
                jump + " has out-of-bounds " + which + " cell " + cell + " (board 1.." + size + ")");
        }
    }
}
