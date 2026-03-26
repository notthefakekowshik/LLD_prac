// LSP GOOD: Independent class. Does NOT extend Rectangle.
// Square has its own invariant (all sides equal) expressed through its own setSide method.
// No inherited setter contract is broken because there is none to break.
package com.lldprep.foundations.solid.lsp.good;

public class Square implements Shape {

    private double side;

    public Square(double side) {
        this.side = side;
    }

    public void setSide(double side) {
        this.side = side;
    }

    public double getSide() {
        return side;
    }

    @Override
    public double area() {
        return side * side;
    }

    @Override
    public String name() {
        return "Square(side=" + side + ")";
    }
}
