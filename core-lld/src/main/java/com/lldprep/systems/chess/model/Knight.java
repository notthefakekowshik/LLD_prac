package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    private static final int[][] KNIGHT_MOVES = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
        {1, -2},  {1, 2},  {2, -1},  {2, 1}
    };

    public Knight(Color color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public List<Square> getCandidateSquares(Board board, Square current) {
        List<Square> result = new ArrayList<>();
        int row = current.row();
        int col = current.col();

        for (int[] move : KNIGHT_MOVES) {
            Square target = new Square(row + move[0], col + move[1]);
            if (target.isValid()) {
                Piece occupant = board.getPiece(target);
                if (occupant == null || isEnemy(occupant)) {
                    result.add(target);
                }
            }
        }
        return result;
    }

    @Override
    public Piece copy() {
        Knight copy = new Knight(getColor());
        if (hasMoved()) copy.setHasMoved();
        return copy;
    }
}
