package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Rook extends Piece {

    private static final int[][] DIRECTIONS = {
        {-1, 0}, {0, 1}, {1, 0}, {0, -1}
    };

    public Rook(Color color) {
        super(color, PieceType.ROOK);
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
        Rook copy = new Rook(getColor());
        if (hasMoved()) copy.setHasMoved();
        return copy;
    }
}
