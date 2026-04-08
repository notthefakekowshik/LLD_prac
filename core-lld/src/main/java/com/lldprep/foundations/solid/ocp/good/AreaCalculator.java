// OCP GOOD: This class is CLOSED for modification. It never needs to change when new shapes are added.
// It is agnostic to concrete shape types — it only depends on the Shape abstraction.
package com.lldprep.foundations.solid.ocp.good;

import java.util.List;

public class AreaCalculator {

    public double totalArea(List<Shape> shapes) {
        double total = 0;
        for (Shape shape : shapes) {
            total += shape.area();
        }
        return total;
    }
}
