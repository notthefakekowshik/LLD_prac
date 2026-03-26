// OCP GOOD: Adding Circle requires only creating this new class — no existing code is modified.
package com.lldprep.foundations.solid.ocp.good;

public class Circle implements Shape {

    private final double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public String name() {
        return "Circle(r=" + radius + ")";
    }
}
