package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Square> getCandidateSquares(Board board, Square current) {
        List<Square> result = new ArrayList<>();
        int row = current.row();
        int col = current.col();
        int direction = getColor() == Color.WHITE ? -1 : 1;
        int startRow = getColor() == Color.WHITE ? 6 : 1;

        Square forwardOne = new Square(row + direction, col);
        if (forwardOne.isValid() && board.getPiece(forwardOne) == null) {
            result.add(forwardOne);

            if (row == startRow) {
                Square forwardTwo = new Square(row + 2 * direction, col);
                if (board.getPiece(forwardTwo) == null) {
                    result.add(forwardTwo);
                }
            }
        }

        for (int dCol = -1; dCol <= 1; dCol += 2) {
            Square diag = new Square(row + direction, col + dCol);
            if (diag.isValid()) {
                Piece occupant = board.getPiece(diag);
                if (isEnemy(occupant)) {
                    result.add(diag);
                }
            }
        }

        addEnPassantCandidate(board, current, result);

        return result;
    }

    private void addEnPassantCandidate(Board board, Square current, List<Square> result) {
        int row = current.row();
        int col = current.col();
        int direction = getColor() == Color.WHITE ? -1 : 1;
        int enPassantRank = getColor() == Color.WHITE ? 3 : 4;

        if (row != enPassantRank) return;

        Square target = board.getEnPassantTarget();
        if (target == null) return;

        for (int dCol = -1; dCol <= 1; dCol += 2) {
            Square diag = new Square(row + direction, col + dCol);
            if (diag.equals(target)) {
                result.add(diag);
            }
        }
    }

    @Override
    public Piece copy() {
        Pawn copy = new Pawn(getColor());
        if (hasMoved()) copy.setHasMoved();
        return copy;
    }
}
