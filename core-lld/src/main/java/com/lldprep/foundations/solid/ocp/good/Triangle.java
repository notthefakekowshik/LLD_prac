// OCP GOOD: Adding Triangle requires only creating this new class — no existing code is modified.
package com.lldprep.foundations.solid.ocp.good;

public class Triangle implements Shape {

    private final double base;
    private final double height;

    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }

    @Override
    public double area() {
        return 0.5 * base * height;
    }

    @Override
    public String name() {
        return "Triangle(b=" + base + ", h=" + height + ")";
    }
}
