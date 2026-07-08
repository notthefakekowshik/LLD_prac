package com.lldprep.systems.snakeandladder.exception;

/** Base unchecked exception for all Snake & Ladder domain errors. */
public class SnakeAndLadderException extends RuntimeException {
    public SnakeAndLadderException(String message) {
        super(message);
    }
}
