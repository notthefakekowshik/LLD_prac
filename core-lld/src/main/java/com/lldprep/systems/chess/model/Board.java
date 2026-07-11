package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final Piece[][] grid;
    private Square enPassantTarget;

    public Board() {
        this.grid = new Piece[8][8];
    }

    public Board(Piece[][] grid, Square enPassantTarget) {
        this.grid = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                this.grid[r][c] = grid[r][c] != null ? grid[r][c].copy() : null;
            }
        }
        this.enPassantTarget = enPassantTarget;
    }

    public Piece getPiece(Square square) {
        return grid[square.row()][square.col()];
    }

    public void setPiece(Square square, Piece piece) {
        grid[square.row()][square.col()] = piece;
    }

    public Square getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(Square target) {
        this.enPassantTarget = target;
    }

    public void clearEnPassantTarget() {
        this.enPassantTarget = null;
    }

    public void rawMove(Square from, Square to) {
        Piece moving = getPiece(from);
        setPiece(from, null);
        setPiece(to, moving);
    }

    // Why: copy() creates a fresh board for move simulation — the core of self-check prevention.
    public Board copy() {
        return new Board(grid, enPassantTarget);
    }

    public boolean isCheck(Color defendingColor) {
        Square kingSquare = findKing(defendingColor);
        if (kingSquare == null) return false;
        return isSquareAttackedBy(kingSquare, defendingColor.opposite());
    }

    // Why: castling legality needs "is this square attacked?" for squares the king does NOT stand on
    // (the transit square), so isCheck's find-the-king form can't answer it — this generalises it.
    public boolean isSquareAttackedBy(Square target, Color attackerColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = grid[r][c];
                if (piece != null && piece.getColor() == attackerColor) {
                    List<Square> attacks = piece.getAttackSquares(this, new Square(r, c));
                    if (attacks.contains(target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isCheckmate(Color defendingColor) {
        return isCheck(defendingColor) && hasNoLegalMoves(defendingColor);
    }

    public boolean isStalemate(Color defendingColor) {
        return !isCheck(defendingColor) && hasNoLegalMoves(defendingColor);
    }

    public boolean hasNoLegalMoves(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = grid[r][c];
                if (piece != null && piece.getColor() == color) {
                    Square current = new Square(r, c);
                    if (!piece.getValidMoves(this, current).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Square findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = grid[r][c];
                if (piece != null && piece.getType() == PieceType.KING && piece.getColor() == color) {
                    return new Square(r, c);
                }
            }
        }
        return null;
    }

    public void executeMove(Move move) {
        Square from = move.from();
        Square to = move.to();

        if (move.isCastling()) {
            executeCastling(move);
            return;
        }

        if (move.isEnPassant()) {
            executeEnPassant(move);
            return;
        }

        setPiece(from, null);
        Piece moving = move.piece();
        if (move.promotion() != null) {
            moving = PieceFactory.create(move.promotion(), moving.getColor());
        }
        moving.setHasMoved();
        setPiece(to, moving);
    }

    private void executeCastling(Move move) {
        Square from = move.from();
        Square to = move.to();
        int row = from.row();
        boolean kingside = to.col() == 6;

        Piece king = getPiece(from);
        setPiece(from, null);
        king.setHasMoved();
        setPiece(to, king);

        if (kingside) {
            Square rookFrom = new Square(row, 7);
            Square rookTo = new Square(row, 5);
            Piece rook = getPiece(rookFrom);
            setPiece(rookFrom, null);
            rook.setHasMoved();
            setPiece(rookTo, rook);
        } else {
            Square rookFrom = new Square(row, 0);
            Square rookTo = new Square(row, 3);
            Piece rook = getPiece(rookFrom);
            setPiece(rookFrom, null);
            rook.setHasMoved();
            setPiece(rookTo, rook);
        }
    }

    private void executeEnPassant(Move move) {
        Square from = move.from();
        Square to = move.to();

        Piece pawn = getPiece(from);
        setPiece(from, null);
        pawn.setHasMoved();

        int capturedRow = from.row();
        int capturedCol = to.col();
        setPiece(new Square(capturedRow, capturedCol), null);

        setPiece(to, pawn);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int r = 0; r < 8; r++) {
            sb.append(8 - r).append(" ");
            for (int c = 0; c < 8; c++) {
                Piece piece = grid[r][c];
                sb.append(piece == null ? "." : piece.toString()).append(" ");
            }
            sb.append(8 - r).append("\n");
        }
        sb.append("  a b c d e f g h");
        return sb.toString();
    }
}
