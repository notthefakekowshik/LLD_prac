// LSP VIOLATION: Square breaks Rectangle's contract.
// A Square IS-A Rectangle mathematically, but NOT substitutable for it in code.
// Rectangle's contract guarantees setWidth and setHeight are independent.
// Square's override silently couples them, so code written for Rectangle
// produces WRONG results when given a Square — a classic LSP violation.
package com.lldprep.foundations.solid.lsp.bad;

public class Square extends Rectangle {

    public Square(int side) {
        super(side, side);
    }

    // LSP VIOLATION: setWidth changes BOTH dimensions — breaks Rectangle's independent-setter contract
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width; // forces height == width
    }

    // LSP VIOLATION: setHeight changes BOTH dimensions — breaks Rectangle's independent-setter contract
    @Override
    public void setHeight(int height) {
        this.height = height;
        this.width = height; // forces width == height
    }

    @Override
    public String toString() {
        return "Square(side=" + width + ", area=" + area() + ")";
    }
}
