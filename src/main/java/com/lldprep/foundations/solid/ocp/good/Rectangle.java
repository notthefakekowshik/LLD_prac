// OCP GOOD: Adding Rectangle requires only creating this new class — no existing code is modified.
package com.lldprep.foundations.solid.ocp.good;

public class Rectangle implements Shape {

    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }

    @Override
    public String name() {
        return "Rectangle(" + width + "x" + height + ")";
    }
}
