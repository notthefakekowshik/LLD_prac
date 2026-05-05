// REALIZATION: Rectangle also realizes Drawable.
// Same contract, completely different implementation — polymorphism in action.
package com.lldprep.foundations.oop.realization;

public class Rectangle implements Drawable {

    private final double width;
    private final double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw() {
        System.out.println("Drawing Rectangle " + width + " x " + height);
    }

    @Override
    public double area() {
        return width * height;
    }
}
