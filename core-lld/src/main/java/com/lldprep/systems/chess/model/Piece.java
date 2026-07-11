package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;

public abstract class Piece {
    private final Color color;
    private final PieceType type;
    private boolean hasMoved;

    protected Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
        this.hasMoved = false;
    }

    public Color getColor() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved() {
        this.hasMoved = true;
    }

    public abstract List<Square> getCandidateSquares(Board board, Square current);

    // Squares this piece threatens. Defaults to its move candidates; King overrides to exclude
    // castling — castling is a move, not an attack, and including it would recurse through isCheck.
    public List<Square> getAttackSquares(Board board, Square current) {
        return getCandidateSquares(board, current);
    }

    public List<Square> getValidMoves(Board board, Square current) {
        List<Square> candidates = getCandidateSquares(board, current);
        List<Square> legal = new ArrayList<>();
        for (Square target : candidates) {
            Board simulated = board.copy();
            simulated.rawMove(current, target);
            if (!simulated.isCheck(color)) {
                legal.add(target);
            }
        }
        return legal;
    }

    public abstract Piece copy();

    protected boolean isEnemy(Piece other) {
        return other != null && other.getColor() != color;
    }

    protected void addSlidingCandidates(Board board, Square current, int dRow, int dCol, List<Square> result) {
        int r = current.row() + dRow;
        int c = current.col() + dCol;
        while (r >= 0 && r < 8 && c >= 0 && c < 8) {
            Square sq = new Square(r, c);
            Piece occupant = board.getPiece(sq);
            if (occupant == null) {
                result.add(sq);
            } else {
                if (isEnemy(occupant)) {
                    result.add(sq);
                }
                break;
            }
            r += dRow;
            c += dCol;
        }
    }

    @Override
    public String toString() {
        String symbol = switch (type) {
            case KING -> "K";
            case QUEEN -> "Q";
            case ROOK -> "R";
            case BISHOP -> "B";
            case KNIGHT -> "N";
            case PAWN -> "P";
        };
        return color == Color.WHITE ? symbol : symbol.toLowerCase();
    }
}
