package com.practice.tictactoe;

public class TicTacToeBoard {
    private TicTacToePiece [][] board;

    public TicTacToeBoard(int m, int n) {
        board = new TicTacToePiece[m][n];
    }

    public void setPiece(TicTacToePiece piece, int x, int y) {
        this.board[x][y] = piece;
    }

    public TicTacToePiece getPiece(int x, int y) {
        return this.board[x][y];
    }

    public boolean isCellFilled(int x, int y) {
        return board[x][y] != null;
    }

}
