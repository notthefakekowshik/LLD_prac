// LSP setup (base class): Rectangle with a key invariant.
// Invariant: area() == width * height — callers rely on this contract.
package com.lldprep.foundations.solid.lsp.bad;

public class Rectangle {

    protected int width;
    protected int height;

    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // Invariant: area() == width * height
    public int area() {
        return width * height;
    }

    @Override
    public String toString() {
        return "Rectangle(width=" + width + ", height=" + height + ", area=" + area() + ")";
    }
}
