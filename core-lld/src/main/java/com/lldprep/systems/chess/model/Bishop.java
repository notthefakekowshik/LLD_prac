package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Bishop extends Piece {

    private static final int[][] DIRECTIONS = {
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    public Bishop(Color color) {
        super(color, PieceType.BISHOP);
    }

    @Override
    public List<Square> getCandidateSquares(Board board, Square current) {
        List<Square> result = new ArrayList<>();
        for (int[] dir : DIRECTIONS) {
            addSlidingCandidates(board, current, dir[0], dir[1], result);
        }
        return result;
    }

    @Override
    public Piece copy() {
        Bishop copy = new Bishop(getColor());
        if (hasMoved()) copy.setHasMoved();
        return copy;
    }
}
