package com.practice.tictactoe;

public class Player {
    private int id ;
    private String name;
    private TicTacToePiece ticTacToePiece;

    Player(int id, String name,  TicTacToePiece ticTacToePiece) {
        this.id = id;
        this.name = name;
        this.ticTacToePiece = ticTacToePiece;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public TicTacToePiece getTicTacToePiece() {
        return ticTacToePiece;
    }
    public void setTicTacToePiece(TicTacToePiece ticTacToePiece) {
        this.ticTacToePiece = ticTacToePiece;
    }
}
