package com.lldprep.systems.chess.model;

import com.lldprep.systems.chess.model.enums.Color;
import com.lldprep.systems.chess.model.enums.GameState;
import com.lldprep.systems.chess.model.enums.PieceType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {
    private final String id;
    private final Board board;
    private final Player white;
    private final Player black;
    private GameState state;
    private final List<Move> moves;
    private int fullMoveNumber;

    public Game(String whiteName, String blackName) {
        this.id = "GAME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // Player ids are game-scoped so players from different games never compare equal.
        this.white = new Player(id + "-W", whiteName, Color.WHITE);
        this.black = new Player(id + "-B", blackName, Color.BLACK);
        this.board = new Board();
        this.state = GameState.WHITE_TO_MOVE;
        this.moves = new ArrayList<>();
        this.fullMoveNumber = 1;
        setupBoard();
    }

    public String getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public Player getWhite() {
        return white;
    }

    public Player getBlack() {
        return black;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public List<Move> getMoves() {
        return List.copyOf(moves);
    }

    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    public Color getCurrentTurnColor() {
        return switch (state) {
            case WHITE_TO_MOVE -> Color.WHITE;
            case BLACK_TO_MOVE -> Color.BLACK;
            default -> null;
        };
    }

    public void addMove(Move move) {
        moves.add(move);
    }

    public void advanceTurn() {
        if (state == GameState.WHITE_TO_MOVE) {
            state = GameState.BLACK_TO_MOVE;
        } else if (state == GameState.BLACK_TO_MOVE) {
            state = GameState.WHITE_TO_MOVE;
            fullMoveNumber++;
        }
    }

    private void setupBoard() {
        Board b = board;

        PieceType[] backRankOrder = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
            PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};

        for (int c = 0; c < 8; c++) {
            b.setPiece(new Square(7, c), PieceFactory.create(backRankOrder[c], Color.WHITE));
            b.setPiece(new Square(6, c), PieceFactory.create(PieceType.PAWN, Color.WHITE));
            b.setPiece(new Square(1, c), PieceFactory.create(PieceType.PAWN, Color.BLACK));
            b.setPiece(new Square(0, c), PieceFactory.create(backRankOrder[c], Color.BLACK));
        }
    }

    @Override
    public String toString() {
        return "Game " + id + " | " + white.getName() + " vs " + black.getName() + " | " + state;
    }
}
