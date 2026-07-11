package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.PieceType;

public record Move(
    Square from,
    Square to,
    Piece piece,
    Piece captured,
    PieceType promotion,
    boolean isCastling,
    boolean isEnPassant
) {

    public static Move normal(Square from, Square to, Piece piece, Piece captured) {
        return new Move(from, to, piece, captured, null, false, false);
    }

    public static Move promotion(Square from, Square to, Piece piece, Piece captured, PieceType promotion) {
        return new Move(from, to, piece, captured, promotion, false, false);
    }

    public static Move castling(Square from, Square to, Piece piece) {
        return new Move(from, to, piece, null, null, true, false);
    }

    public static Move enPassant(Square from, Square to, Piece piece) {
        Piece captured = new Pawn(piece.getColor().opposite());
        return new Move(from, to, piece, captured, null, false, true);
    }

    public String notation() {
        StringBuilder sb = new StringBuilder();
        if (isCastling) {
            sb.append(to().col() == 6 ? "O-O" : "O-O-O");
        } else {
            if (piece.getType() != PieceType.PAWN) {
                sb.append(piece.toString().toUpperCase());
            }
            sb.append(from);
            if (captured != null) {
                sb.append("x");
            }
            sb.append(to);
            if (promotion != null) {
                sb.append("=").append(promotion.name().charAt(0));
            }
        }
        return sb.toString();
    }
}
