package com.lldprep.systems.chess.exception;

public class GameNotFoundException extends ChessException {
    public GameNotFoundException(String gameId) {
        super("Game not found: " + gameId);
    }
}
