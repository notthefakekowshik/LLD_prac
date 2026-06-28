package com.practice.tictactoe;

public class TicTacToeDemo {
    public static void main(String[] args) throws TicTacToeInvalidMoveException {
        Player kow = new Player(1, "Kowshik", TicTacToePiece.O);
        Player musk = new Player(2, "Musk",  TicTacToePiece.X);
        TicTacToeGame ticTacToeGame = new TicTacToeGame(3, kow, musk);

        ticTacToeGame.makeMove(kow, 0,0);
        ticTacToeGame.makeMove(kow, 0,0);

    }
}
