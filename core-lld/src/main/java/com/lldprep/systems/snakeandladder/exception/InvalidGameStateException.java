package com.lldprep.systems.snakeandladder.exception;

/** Thrown for illegal game operations — too few players, or a turn after the game has finished. */
public class InvalidGameStateException extends SnakeAndLadderException {
    public InvalidGameStateException(String message) {
        super(message);
    }
}
