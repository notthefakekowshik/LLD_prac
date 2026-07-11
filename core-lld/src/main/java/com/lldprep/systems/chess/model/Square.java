package com.lldprep.systems.chess.model;

public record Square(int row, int col) {

    public static final String FILES = "abcdefgh";

    public boolean isValid() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public static Square fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic square: " + algebraic);
        }
        int col = FILES.indexOf(algebraic.charAt(0));
        int row = 8 - Character.getNumericValue(algebraic.charAt(1));
        if (col < 0 || row < 0 || row > 7) {
            throw new IllegalArgumentException("Invalid algebraic square: " + algebraic);
        }
        return new Square(row, col);
    }

    @Override
    public String toString() {
        return "" + FILES.charAt(col) + (8 - row);
    }
}
