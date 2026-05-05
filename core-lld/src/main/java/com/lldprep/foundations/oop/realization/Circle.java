// REALIZATION: Circle REALIZES the Drawable interface.
// It fulfills the contract — provides concrete implementations of draw() and area().
// The interface defines WHAT, Circle defines HOW.
package com.lldprep.foundations.oop.realization;

public class Circle implements Drawable {

    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public void draw() {
        System.out.println("Drawing Circle with radius " + radius);
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}
