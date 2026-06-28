package com.practice.tictactoe;

public class TicTacToeGame {
    private TicTacToeBoard ticTacToeBoard;
    private int m;
    private boolean isGameCompleted;
    private TicTacToePiece currentMovePiece;
    private Player currentPlayer, nextPlayer, gameWinner;

    public TicTacToeGame(int m, Player currentPlayer, Player nextPlayer) {
        this.m = m;
        this.ticTacToeBoard = new TicTacToeBoard(m,m);
        this.currentPlayer = currentPlayer;
        this.nextPlayer = nextPlayer;
    }

    /*
        I got stuck here whether to use throws at method level or use try catch
        wouldn't throws at method level populate all the upstream methods? all of them should ACK this exception.
        What if we've 10 different exceptions? would the main class ACK all of them?
     */
    public void makeMove(Player playerToMakeMove, int x, int y) throws TicTacToeInvalidMoveException {
        if (isValidMove(playerToMakeMove, x, y)) {
            throw new TicTacToeInvalidMoveException("Invalid move!");
        }
        ticTacToeBoard.setPiece(playerToMakeMove.getTicTacToePiece(), x, y);
        if (checkIfGameCompleted(x, y)) {
            System.out.println("Game completed, hurray!");
            gameWinner = playerToMakeMove;
            isGameCompleted = true;
            return;
        }
        currentPlayer = nextPlayer;
        nextPlayer = playerToMakeMove;
    }

    private boolean isValidMove(Player playerToMakeMove, int x, int y) {
        if (isGameCompleted &&
                playerToMakeMove.getId() != currentPlayer.getId() ||
                ticTacToeBoard.isCellFilled(x,y)) {
            return false;
        }
        return true;
    }

    // TODO; Logic to traverse horizontal, vertical, diagonal if any of them is completed.
    private boolean checkIfGameCompleted(int x, int y) {
        boolean isRowFilled = true;
        for(int i = 0; i < m; i++) {
            if(ticTacToeBoard.getPiece(i, y) != currentPlayer.getTicTacToePiece()) {
                isRowFilled = false;
                break;
            }
        }

        boolean isColumnFilled = true;
        for(int i = 0; i < m; i++) {
            if(ticTacToeBoard.getPiece(x, i) != currentPlayer.getTicTacToePiece()) {
                isColumnFilled = false;
                break;
            }
        }
    }
}
