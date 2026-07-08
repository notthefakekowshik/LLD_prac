package com.lldprep.systems.snakeandladder.exception;

/** Thrown when a board or its snakes/ladders violate setup rules (bounds, direction, overlap). */
public class InvalidBoardException extends SnakeAndLadderException {
    public InvalidBoardException(String message) {
        super(message);
    }
}
