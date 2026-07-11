package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {

    private static final int[][] KING_MOVES = {
        {-1, -1}, {-1, 0}, {-1, 1},
        {0, -1},           {0, 1},
        {1, -1},  {1, 0},  {1, 1}
    };

    private static final int[][] CASTLING_OFFSETS = {
        {-1, -1, 0, -3},  // queenside: offsets for rookFromCol, rookToCol, required empty cols
        {-1, 1, 0, 1}     // kingside: direction * (king dCol, rook dCol)
    };

    public King(Color color) {
        super(color, PieceType.KING);
    }

    @Override
    public List<Square> getCandidateSquares(Board board, Square current) {
        List<Square> result = new ArrayList<>();
        int row = current.row();
        int col = current.col();

        for (int[] move : KING_MOVES) {
            Square target = new Square(row + move[0], col + move[1]);
            if (target.isValid()) {
                Piece occupant = board.getPiece(target);
                if (occupant == null || isEnemy(occupant)) {
                    result.add(target);
                }
            }
        }

        addCastlingCandidates(board, current, result);

        return result;
    }

    // A king threatens only its 8 neighbours. Excludes castling (a move, not an attack) — this is
    // also what stops isCheck -> castling -> isCheck from recursing forever.
    @Override
    public List<Square> getAttackSquares(Board board, Square current) {
        List<Square> result = new ArrayList<>();
        int row = current.row();
        int col = current.col();
        for (int[] move : KING_MOVES) {
            Square target = new Square(row + move[0], col + move[1]);
            if (target.isValid()) {
                result.add(target);
            }
        }
        return result;
    }

    private void addCastlingCandidates(Board board, Square current, List<Square> result) {
        if (hasMoved()) return;
        // Can't castle out of check.
        if (board.isCheck(getColor())) return;

        int row = current.row();
        Color enemy = getColor().opposite();

        // Kingside: rook at (row, 7); king travels e->f->g, so f (col 5) must be empty AND safe.
        Piece kingsideRook = board.getPiece(new Square(row, 7));
        if (isUnmovedRook(kingsideRook)
                && squaresEmpty(board, row, 5, 6)
                && !board.isSquareAttackedBy(new Square(row, 5), enemy)) {
            result.add(new Square(row, 6));
        }

        // Queenside: rook at (row, 0); king travels e->d->c, so d (col 3) must be empty AND safe.
        // (b-file at col 1 must be empty but may be attacked — the king never steps there.)
        Piece queensideRook = board.getPiece(new Square(row, 0));
        if (isUnmovedRook(queensideRook)
                && squaresEmpty(board, row, 1, 3)
                && !board.isSquareAttackedBy(new Square(row, 3), enemy)) {
            result.add(new Square(row, 2));
        }
    }

    private boolean isUnmovedRook(Piece piece) {
        return piece != null && piece.getType() == PieceType.ROOK && !piece.hasMoved();
    }

    private boolean squaresEmpty(Board board, int row, int colStart, int colEnd) {
        for (int c = colStart; c <= colEnd; c++) {
            if (board.getPiece(new Square(row, c)) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Piece copy() {
        King copy = new King(getColor());
        if (hasMoved()) copy.setHasMoved();
        return copy;
    }
}
