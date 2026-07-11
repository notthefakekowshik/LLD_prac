package com.lldprep.systems.chess.service;

import com.lldprep.systems.chess.exception.GameNotFoundException;
import com.lldprep.systems.chess.exception.GameOverException;
import com.lldprep.systems.chess.exception.InvalidMoveException;
import com.lldprep.systems.chess.model.Board;
import com.lldprep.systems.chess.model.Game;
import com.lldprep.systems.chess.model.Move;
import com.lldprep.systems.chess.model.Piece;
import com.lldprep.systems.chess.model.PieceFactory;
import com.lldprep.systems.chess.model.Square;
import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.GameState;
import com.lldprep.systems.chess.model.enums.PieceType;
import com.lldprep.systems.chess.repository.GameRepository;

import java.util.List;

public class GameService {
    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game createGame(String whiteName, String blackName) {
        Game game = new Game(whiteName, blackName);
        gameRepository.save(game);
        return game;
    }

    public Move makeMove(String gameId, Square from, Square to, PieceType promotionType) {
        Game game = getGameOrThrow(gameId);
        // CRITICAL SECTION — a move (validate + mutate board + state transition) must apply as one
        // atomic unit, so two threads can't interleave on the same game's shared board/turn state.
        // ponytail: per-game lock (the Game monitor) — different games proceed in parallel; a single
        // game is inherently serial. Escalate only if one game ever needs concurrent sub-moves.
        synchronized (game) {
            validateGameActive(game);

            Board board = game.getBoard();
            Color currentColor = game.getCurrentTurnColor();

            Piece piece = board.getPiece(from);
            if (piece == null) {
                throw new InvalidMoveException("No piece at " + from);
            }
            if (piece.getColor() != currentColor) {
                throw new InvalidMoveException("Not " + currentColor + "'s turn");
            }

            List<Square> legalMoves = piece.getValidMoves(board, from);
            if (!legalMoves.contains(to)) {
                throw new InvalidMoveException("Illegal move: " + piece.getType() + " from " + from + " to " + to);
            }

            Piece captured = board.getPiece(to);
            boolean isCastling = piece.getType() == PieceType.KING && Math.abs(to.col() - from.col()) == 2;
            boolean isEnPassant = piece.getType() == PieceType.PAWN
                && to.col() != from.col()
                && captured == null;

            Move move;
            if (isCastling) {
                move = Move.castling(from, to, piece);
            } else if (isEnPassant) {
                move = Move.enPassant(from, to, piece);
            } else if (isPromotion(piece, to)) {
                PieceType promoType = promotionType != null ? promotionType : PieceType.QUEEN;
                move = Move.promotion(from, to, piece, captured, promoType);
            } else {
                move = Move.normal(from, to, piece, captured);
            }

            board.clearEnPassantTarget();

            if (piece.getType() == PieceType.PAWN && Math.abs(to.row() - from.row()) == 2) {
                int enPassantRow = currentColor == Color.WHITE ? 5 : 2;
                board.setEnPassantTarget(new Square(enPassantRow, from.col()));
            }

            board.executeMove(move);
            game.addMove(move);

            // Terminal states end the game; otherwise the turn passes. Plain check isn't tracked as a
            // separate GameState, so a check and a quiet move both just advance the turn.
            Color opponentColor = currentColor.opposite();
            if (board.isCheckmate(opponentColor)) {
                game.setState(currentColor == Color.WHITE ? GameState.WHITE_WINS : GameState.BLACK_WINS);
            } else if (board.isStalemate(opponentColor)) {
                game.setState(GameState.STALEMATE);
            } else {
                game.advanceTurn();
            }

            return move;
        }
    }

    public Move makeMove(String gameId, String fromAlgebraic, String toAlgebraic) {
        return makeMove(gameId, Square.fromAlgebraic(fromAlgebraic), Square.fromAlgebraic(toAlgebraic), null);
    }

    public Game getGame(String gameId) {
        return getGameOrThrow(gameId);
    }

    // Promotion is defined purely by a pawn reaching the far rank — NOT by the caller passing a
    // promotionType (a stray type must never promote a pawn mid-board).
    private boolean isPromotion(Piece piece, Square to) {
        if (piece.getType() != PieceType.PAWN) return false;
        boolean whitePromotion = piece.getColor() == Color.WHITE && to.row() == 0;
        boolean blackPromotion = piece.getColor() == Color.BLACK && to.row() == 7;
        return whitePromotion || blackPromotion;
    }

    private void validateGameActive(Game game) {
        GameState state = game.getState();
        if (state == GameState.WHITE_WINS) {
            throw new GameOverException("White has already won");
        }
        if (state == GameState.BLACK_WINS) {
            throw new GameOverException("Black has already won");
        }
        if (state == GameState.STALEMATE) {
            throw new GameOverException("Game ended in stalemate");
        }
    }

    private Game getGameOrThrow(String gameId) {
        Game game = gameRepository.getById(gameId);
        if (game == null) {
            throw new GameNotFoundException(gameId);
        }
        return game;
    }
}
